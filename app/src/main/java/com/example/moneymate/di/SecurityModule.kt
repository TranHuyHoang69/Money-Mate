package com.example.moneymate.di

import android.content.Context
import com.example.moneymate.data.local.SecurityLocalDataSource
import com.example.moneymate.data.remote.SecurityRemoteDataSource
import com.example.moneymate.data.repository.SecurityRepositoryImpl
import com.example.moneymate.domain.repository.SecurityRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideSecurityLocalDataSource(@ApplicationContext context: Context) = SecurityLocalDataSource(context)

    @Provides
    @Singleton
    fun provideSecurityRemoteDataSource(firestore: FirebaseFirestore, auth: FirebaseAuth) =
        SecurityRemoteDataSource(firestore, auth)

    @Provides
    @Singleton
    fun provideSecurityRepository(
        local: SecurityLocalDataSource,
        remote: SecurityRemoteDataSource,
        dispatcher: CoroutineDispatcher
    ): SecurityRepository = SecurityRepositoryImpl(local, remote, dispatcher)
}