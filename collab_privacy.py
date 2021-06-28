#
# Author: Eoin Lyness
# Train collaborative filtering model (privacy-preserving)
#

import pandas as pd
import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as f
import torch.optim as optim
import syft as sy
import time
from torch.utils.data import Dataset
from syft.frameworks.torch.fl import utils
import random
from copy import deepcopy
from torch.utils.data import DataLoader, Dataset

random.seed(4006)
hook = sy.TorchHook(torch)
class Arguments():
    def __init__(self):
        self.batch_size = 64
        self.test_batch_size = 64
        self.seed = 4006
 
class UserItemRatingDataset(Dataset):
    """Wrapper, convert <user, item, rating> Tensor into Pytorch Dataset"""
    def __init__(self, user_tensor, item_tensor, target_tensor):
        """
        args:
            target_tensor: torch.Tensor, the corresponding rating for <user, item> pair
        """
        self.user_tensor = user_tensor
        self.item_tensor = item_tensor
        self.target_tensor = target_tensor
 
    def __getitem__(self, index):
        data_tensor=torch.stack([self.user_tensor[index], self.item_tensor[index]],dim=0)
        return data_tensor, self.target_tensor[index]
 
    def __len__(self):
        return len(self.user_tensor)


#Load dataset
fname = 'output/cluster_out.csv'
data = pd.read_csv(fname, index_col=0)

#Split data into 80:20 training:validation
train_data = data.sample(frac=0.8)
train_data = train_data.reset_index(drop=True)

test_data = data.sample(frac=0.2)
test_data = test_data.reset_index(drop=True)

#Process dataset attributes to prepare for use with model:
#Map each user to a unique ID
#Cast cluster and mood to type float 
user_ids = train_data["user"].unique().tolist()
cluster_ids = train_data["cluster"].unique().tolist()
num_users = len(user_ids)
num_clusters = len(cluster_ids)

user_map = {x: i for i, x in enumerate(user_ids)}
train_data["user"] = train_data["user"].map(user_map)
train_data["cluster"] = train_data["cluster"].values.astype(np.float32)
train_data["mood"] = train_data["mood"].values.astype(np.float32)

test_user_ids = test_data["user"].unique().tolist()
test_cluster_ids = test_data["cluster"].unique().tolist()
test_num_users = len(test_user_ids)
test_num_clusters = len(test_cluster_ids)

test_user_map = {x: i for i, x in enumerate(test_user_ids)}
test_data["user"] = test_data["user"].map(test_user_map)
test_data["cluster"] = test_data["cluster"].values.astype(np.float32)
test_data["mood"] = test_data["mood"].values.astype(np.float32)


#Create virtual workers
workers = []

for user_id in user_ids:
    worker = sy.VirtualWorker(hook, id="user_"+str(user_id))
    workers.append(worker)

#Create federated data loader for training data
federated_train_loader = sy.FederatedDataLoader(
UserItemRatingDataset(torch.LongTensor(train_data["user"]),torch.LongTensor(train_data["cluster"]),torch.FloatTensor(train_data["mood"])).federate(tuple(workers)),batch_size=64,shuffle=True,iter_per_worker=True)

kwargs = {}

#Create data loaders to validate the training and validation data
train_test_loader = torch.utils.data.DataLoader(
UserItemRatingDataset(torch.LongTensor(train_data["user"]),torch.LongTensor(train_data["cluster"]),torch.FloatTensor(train_data["mood"])),batch_size=64,shuffle=True, **kwargs)

valid_test_loader = torch.utils.data.DataLoader(
UserItemRatingDataset(torch.LongTensor(test_data["user"]),torch.LongTensor(test_data["cluster"]),torch.FloatTensor(test_data["mood"])),batch_size=64,shuffle=True, **kwargs)

#Define the model
class Model(nn.Module):
    def __init__(self):
        super(Model, self).__init__()
        self.num_users = num_users
        self.num_movies = num_clusters
        self.embedding_size = embedding_size
        
        self.user_embedding = nn.Embedding(num_users, embedding_size)
        self.movie_embedding = nn.Embedding(num_clusters, embedding_size)
        
        self.fc_layers = nn.ModuleList()
        self.fc_layers.append(nn.Linear(50, 50))
        self.output_layer = nn.Linear(embedding_size, 1)

    def forward(self, train_data):
        users = torch.as_tensor(train_data[:,0])
        movies = torch.as_tensor(train_data[:,1])
        user_embedding_x = self.user_embedding(users)
        movie_embedding_y = self.movie_embedding(movies)
        prod=torch.mul(user_embedding_x,movie_embedding_y)
        
        logit = self.output_layer(prod)
        return logit

    def predict(self, train_data):
        return self.forward(train_data)


def train_on_batches(worker, batches, model_in, device, lr):
    """Train the model on the worker on the provided batches
    Args:
        worker(syft.workers.BaseWorker): worker on which the
        training will be executed
        batches: batches of data of this worker
        model_in: machine learning model, training will be done on a copy
        device (torch.device): where to run the training
        lr: learning rate of the training steps
    Returns:
        model, loss: obtained model and loss after training
    """
    model = model_in.copy()
    optimizer = optim.SGD(model.parameters(), lr=lr)

    model.train()
    model.send(worker)
    loss_local = False
    LOG_INTERVAL=len(batches)

    for batch_idx, (data, target) in enumerate(batches):
        loss_local = False
        data, target = data.to(device), target.to(device)
        optimizer.zero_grad()
        output = model(data)
        loss = f.mse_loss(output.view(-1), target)
        loss.backward()
        optimizer.step()
        if batch_idx % LOG_INTERVAL == 0:
            loss = loss.get()
            loss_local = True

    if not loss_local:
      loss = loss.get()
    model.get()
    return model, loss


def get_next_batches(fdataloader: sy.FederatedDataLoader, nr_batches: int):
    """retrieve next nr_batches of the federated data loader and group
    the batches by worker
    Args:
        fdataloader (sy.FederatedDataLoader): federated data loader
        over which the function will iterate
        nr_batches (int): number of batches (per worker) to retrieve
    Returns:
        Dict[syft.workers.BaseWorker, List[batches]]
    """
    batches = {}
    for worker_id in fdataloader.workers:
        worker = fdataloader.federated_dataset.datasets[worker_id].location
        batches[worker] = []
    try:
        for i in range(nr_batches):
            next_batches = next(fdataloader)
            for worker in next_batches:
                batches[worker].append(next_batches[worker])
    except StopIteration:
        pass
    return batches


def train(
    model, device, federated_train_loader, lr, federate_after_n_batches, abort_after_one=False
):
    model.train()

    nr_batches = federate_after_n_batches

    models = {}
    loss_values = {}

    iter(federated_train_loader)  # initialize iterators
    batches = get_next_batches(federated_train_loader, nr_batches)
    counter = 0

    while True:
        data_for_all_workers = True
        for worker in batches:
            curr_batches = batches[worker]
            if curr_batches:
                models[worker], loss_values[worker] = train_on_batches(
                    worker, curr_batches, model, device, lr
                )
            else:
                data_for_all_workers = False
        counter += nr_batches
        if not data_for_all_workers:
            break
        model = utils.federated_avg(models)
        batches = get_next_batches(federated_train_loader, nr_batches)
        if abort_after_one:
            break

    return model


def test(model, device, test_loader):
    model.eval()
    test_loss = 0
    with torch.no_grad():
        for data, target in test_loader:
            data, target = data.to(device), target.to(device)
            output = model(data)
            test_loss += f.mse_loss(output.view(-1), target, reduction='sum').item()

    test_loss /= len(test_loader.dataset)
    return test_loss


fname_out = 'output/collab_privacy_out.csv'
result = pd.DataFrame(columns=['epoch', 'train_loss', 'valid_loss', 'time'])

#Define training parameters 
embedding_size=50
model = Model()
lr = 0.1
batch_size = 64
federate_after_n_batches=round(len(train_data) / batch_size / len(workers))
optimizer = optim.SGD(model.parameters(), lr=lr)

device=torch.device("cpu")
#Run training over 50 epochs
for epoch in range(1, 51):
        print("Starting epoch {}/{}".format(epoch,50))
        #Train the model and record training time
        start = time.time()
        model = train(model, device, federated_train_loader, lr, federate_after_n_batches)
        stop = time.time()
        t = stop - start
        
        #Calculate training and validation loss after training
        train_loss = test(model,device, train_test_loader)
        valid_loss = test(model, device, valid_test_loader)
        
        print('Training Time (s): {:.2f}, Train loss: {:.4f}, Valid loss: {:.4f}\n'.format(t, train_loss, valid_loss))

        result.loc[epoch-1] = [epoch, train_loss, valid_loss, t]

#Output results to file
result.to_csv(fname_out)

