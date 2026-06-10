package com.example.moneymate.di



import com.example.moneymate.data.local.PreferencesLocalDataSource
import com.example.moneymate.data.remote.PreferencesRemoteDataSource
import com.example.moneymate.data.repository.PreferencesRepositoryImpl
import com.example.moneymate.domain.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {


    @Provides
    @Singleton
    fun providePreferencesRepository(
        localDataSource: PreferencesLocalDataSource,
        remoteDataSource: PreferencesRemoteDataSource,
        ioDispatcher: CoroutineDispatcher
    ): PreferencesRepository = PreferencesRepositoryImpl(localDataSource, remoteDataSource, ioDispatcher)
}