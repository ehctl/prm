package com.linhnvt.project_prm.utils

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.linhnvt.project_prm.R

class MyApp : Application() {
    companion object {
        private var instance: MyApp? = null
        private var PROJECT_TOPIC = "projectPrm"
        fun context(): Context = instance!!.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(Constant.COMMON_TAG, getString(R.string.fetching_fcm_failed), task.exception)
                return@OnCompleteListener
            }

            Log.i(Constant.COMMON_TAG,"${task.result}")

        })

        FirebaseMessaging.getInstance().subscribeToTopic(PROJECT_TOPIC)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d(Constant.COMMON_TAG, getString(R.string.failed_to_subscribe))
                }
                Log.d(Constant.COMMON_TAG, getString(R.string.subscribe_success))
            }
    }
}