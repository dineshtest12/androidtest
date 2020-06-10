package com.isuncloud.ott.rn

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.Gson
import com.isuncloud.ott.repository.model.rn.*
import com.isuncloud.ott.utils.RandomIDGenerator
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import timber.log.Timber
import javax.inject.Inject

class WizardModule(private val reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val INIT_INFINITE_CHAIN = "initInfinitechain"
        private const val MAKE_LIGHT_TX = "makeLightTx"
    }

    @Inject lateinit var gson: Gson

    private var eventMap: HashMap<String, Pair<String, SingleEmitter<String>>> = hashMapOf()

    override fun getName(): String {
        return "WizardModule"
    }

    fun initInfiniteChain(envRequest: EnvRequest): Single<InitResult> {
        return sendEvent(INIT_INFINITE_CHAIN, envRequest)
                .map { gson.fromJson(it, InitResult::class.java) }
    }

    fun makeLightTx(lightTx: MakeLightTx): Flowable<String> {
        return sendEvent(MAKE_LIGHT_TX, lightTx).toFlowable()
    }

    @ReactMethod
    fun receiveEvent(receiveData: String) {
        eventResultHandler(receiveData)
    }

    @ReactMethod
    fun receiveAsyncEvent(receiveData: String) {
//        eventResultHandler(receiveData)
    }

    private fun sendEvent(eventName: String, model: BaseModel): Single<String> {
        val eventID = RandomIDGenerator.generated()
        val eventRequest = EventRequest()
        eventRequest.eventID = eventID
        eventRequest.request = model

        val sendData = objectToString(eventRequest)
        Timber.d("send data: $sendData")

        return Single.create<String> {
            eventMap[eventID] = Pair(eventName, it)
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(eventName, sendData)
        }
    }

    private fun eventResultHandler(receiveData: String) {
        Timber.d("receive data: $receiveData")
        val eventResult = gson.fromJson(receiveData, EventResult::class.java)
        val eventID = eventResult.eventID
        if (eventResult.success) {
            eventMap[eventID]!!.second.onSuccess(objectToString(eventResult.result))
        }
    }

    private fun objectToString(rawData: Any): String {
        return gson.toJson(rawData).toString()
    }

}