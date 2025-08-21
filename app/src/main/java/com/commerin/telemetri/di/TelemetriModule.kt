package com.commerin.telemetri.di

import android.content.Context
import com.commerin.telemetri.core.TelemetriManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelemetriModule {

    @Provides
    @Singleton
    fun provideTelemetriManager(@ApplicationContext context: Context): TelemetriManager {
        return TelemetriManager.getInstance(context)
    }
}
