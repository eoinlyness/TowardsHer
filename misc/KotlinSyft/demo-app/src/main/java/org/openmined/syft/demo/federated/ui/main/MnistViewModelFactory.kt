package org.openmined.syft.demo.federated.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.openmined.syft.demo.federated.service.WorkerRepository
import org.openmined.syft.demo.federated.ui.work.WorkInfoViewModel

@Suppress("UNCHECKED_CAST")
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class MnistViewModelFactory(
    private val baseURL: String,
    private val authToken: String,
    private val workerRepository: WorkerRepository,
    private val modelName: String,
    private val modelVersion: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MnistActivityViewModel::class.java))
            return MnistActivityViewModel(
                baseURL,
                authToken,
                workerRepository,
                modelName,
                modelVersion
            ) as T
        throw IllegalArgumentException("unknown view model class")
    }
}