#
# Author: Eoin Lyness
# Cluster the discretised log data
#

from __future__ import division
import random
import numpy as np
from scipy.spatial.distance import cdist
from scipy.sparse import issparse
import random
import sys
from time import time
import pandas as pd
import json

########################
# FUNCTION DEFINITIONS #
########################

def kmeans( X, centres, delta=.001, maxiter=10, p=2, verbose=1 ):
    """ centres, Xtocentre, distances = kmeans( X, initial centres ... )
    in:
        X N x dim  may be sparse
        centres k x dim: initial centres, e.g. random.sample( X, k )
        delta: relative error, iterate until the average distance to centres
            is within delta of the previous average distance
        maxiter
        p: for minkowski metric -- local mod cdist for 0 < p < 1 too
        verbose: 0 silent, 2 prints running distances
    out:
        centres, k x dim
        Xtocentre: each X -> its nearest centre, ints N -> k
        distances, N
    see also: kmeanssample below, class Kmeans below.
    """
    if not issparse(X):
        X = np.asanyarray(X)
    centres = centres.todense() if issparse(centres) \
        else centres.copy()
    N = len(X)
    dim = 1
    k = len(centres)
    cdim = 1
    if dim != cdim:
        raise ValueError( "kmeans: X %s and centres %s must have the same number of columns" % (
            X.shape, centres.shape ))
    if verbose:
        print("kmeans: X %s  centres %s  delta=%.2g  maxiter=%d" % (
            X.shape, centres.shape, delta, maxiter))
    allx = np.arange(N)
    prevdist = 0
    for jiter in range( 1, maxiter+1 ):
        D = cdist_sparse( X, centres)  # |X| x |centres|
        xtoc = D.argmin(axis=1)  # X -> nearest centre
        distances = D[allx,xtoc]
        avdist = distances.mean()  # median ?
        if verbose >= 2:
            print("kmeans: av |X - nearest centre| = %.4g" % avdist)
        if (1 - delta) * prevdist <= avdist <= prevdist \
        or jiter == maxiter:
            break
        prevdist = avdist
        for jc in range(k):  # (1 pass in C)
            c = np.where( xtoc == jc )[0]
            if len(c) > 0:
                centres[jc] = X[int(c.mean())]
    if verbose:
        print("kmeans: %d iterations  cluster sizes:" % jiter, np.bincount(xtoc))
    if verbose >= 2:
        r50 = np.zeros(k)
        r90 = np.zeros(k)
        for j in range(k):
            dist = distances[ xtoc == j ]
            if len(dist) > 0:
                r50[j], r90[j] = np.percentile( dist, (50, 90) )
        print("kmeans: cluster 50 % radius", r50.astype(int))
        print("kmeans: cluster 90 % radius", r90.astype(int))
            # scale L1 / dim, L2 / sqrt(dim) ?
    return centres, xtoc, distances

def kmeanssample( X, k, nsample=0, **kwargs ):
    """ 2-pass kmeans, fast for large N:
        1) kmeans a random sample of nsample ~ sqrt(N) from X
        2) full kmeans, starting from those centres
    """
        # merge w kmeans ? mttiw
        # v large N: sample N^1/2, N^1/2 of that
        # seed like sklearn ?
    N = len(X)
    dim = 1
    if nsample == 0:
        nsample = max( 2*np.sqrt(N), 10*k )
    Xsample = randomsample( X, int(nsample) )
    pass1centres = randomsample( X, int(k) )
    samplecentres = kmeans( Xsample, pass1centres, **kwargs )[0]
    return kmeans( X, samplecentres, **kwargs )

def cdist_sparse(X, Y):
    d = np.empty( (X.shape[0], Y.shape[0]), np.float64 )
    for j, x in enumerate(X):
        x = eval(json.loads(json.dumps(x)))
        for k, y in enumerate(Y):
            y = eval(json.loads(json.dumps(y)))
            d[j,k] = distance( x, y)
    return d

def randomsample( X, n ):
    """ random.sample of the rows of X
        X may be sparse -- best csr
    """
    sampleix = random.sample( range( X.shape[0] ), int(n) )
    return X[sampleix]

def nearestcentres( X, centres, p=2 ):
    """ each X -> nearest centre
    """
    D = cdist( X, centres, p=p )  # |X| x |centres|
    return D.argmin(axis=1)

def Lqmetric( x, y=None, q=.5 ):
    return (np.abs(x - y) ** q) .mean() if y is not None \
        else (np.abs(x) ** q) .mean()


#Calculate distance between hour chunks
def distance(x, y=None):
    keys = ['appInfo', 'callList', 'path', 'places', 'smsList', 'smsOutList', 'urls']

    score = 0
    max_score = 1
    prev_activity_x = ""
    prev_activity_y = ""

    if y is None:
        return 1

    #Increase score if both hours have same label
    if x["activity"] == y["activity"]:
        score = score + 1
    
    #Iterate over each activity
    for key in keys:
        j = 0
        
        for i in range(len(x[key])):
            #Stop if end of y is reached            
            if j >= len(y[key]):
                break

            identifier = "activity"
            if key == "callList" or key == "smsList" or key == "smsOutList":
                identifier = "id"

            #If current activity in y is same as previous then skip it
            if y[key][j][identifier] == prev_activity_y:
                if j < len(y[key])-1:
                    j = j + 1
            else:
                prev_activity_y = y[key][j][identifier] 

            #If current activity in x is same as previous then skip it
            if x[key][i][identifier] == prev_activity_x:
                continue

            prev_activity_x = x[key][i][identifier]
            max_score = max_score + 1
   
            #Increase score if same activity in both hours
            if x[key][i][identifier] == y[key][j][identifier]:
                score = score + 1
            j = j + 1

    #Percentage of similar activities out of total
    res = (score / max_score)
    #Convert similarity to distance between 0 and 1
    return 1.0 - res


class Kmeans:
    """ km = Kmeans( X, k= or centres=, ... )
        in: either initial centres= for kmeans
            or k= [nsample=] for kmeanssample
        out: km.centres, km.Xtocentre, km.distances
        iterator:
            for jcentre, J in km:
                clustercentre = centres[jcentre]
                J indexes e.g. X[J], classes[J]
    """
    def __init__( self, X, k=0, centres=None, nsample=0, **kwargs ):
        self.X = X
        if centres is None:
            self.centres, self.Xtocentre, self.distances = kmeanssample(
                X, k=k, nsample=nsample, **kwargs )
        else:
            self.centres, self.Xtocentre, self.distances = kmeans(
                X, centres, **kwargs )

    def __iter__(self):
        for jc in range(len(self.centres)):
            yield jc, (self.Xtocentre == jc)

#################################################################################

########################
#     MAIN PROGRAM     #
########################

fname = 'output/combined_data.csv'
data = pd.read_csv(fname, index_col=0)

#Parameters
X = np.asarray(data['data'])
N = len(X)
dim = 10
ncluster = 10
kmdelta = .001
kmiter = 2
kmsample = 0 # 0: random centres, > 0: kmeanssample
seed = 4006

np.set_printoptions( 1, threshold=200, edgeitems=5, suppress=True )
np.random.seed(seed)
random.seed(seed)

print("N %d  dim %d  ncluster %d  kmsample %d" % (N, dim, ncluster, kmsample))
t0 = time()
centres, xtoc, dist = kmeanssample( X, ncluster, nsample=kmsample, delta=kmdelta, maxiter=kmiter, verbose=1 )

#Add cluster values as additional column to dataset
data['cluster'] = xtoc

#Output the new dataset
outputFile = "output/cluster_out.csv"
data.to_csv(outputFile)
    
