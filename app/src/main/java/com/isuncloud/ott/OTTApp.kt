package com.isuncloud.ott

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context
import androidx.multidex.MultiDex
import com.facebook.stetho.Stetho
import com.isuncloud.ott.di.AppComponent
import com.isuncloud.ott.di.DaggerAppComponent
import com.isuncloud.ott.utils.ReleaseTree
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import timber.log.Timber
import timber.log.Timber.DebugTree
import javax.inject.Inject

class OTTApp: Application(), HasActivityInjector, LifecycleObserver {

    companion object {
        lateinit var ottApp: OTTApp
        fun getAppComponent(): AppComponent {
            return ottApp.appComponent
        }
    }

    @Inject
    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildConfig.DEBUG || BuildConfig.FLAVOR == "staging") {
            Stetho.initializeWithDefaults(this)
        }

        // Timber for log
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // JSR-310
        AndroidThreeTen.init(this)

        // Dagger
        appComponent = DaggerAppComponent.builder()
                .application(this)
                .build()

        ottApp = this

        appComponent.inject(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Timber.d("App in background")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Timber.d("App in foreground")
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun activityInjector(): AndroidInjector<Activity>
            = activityDispatchingAndroidInjector

}