package com.example.arthguard.core.di

import android.content.Context
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.features.dashboard.data.local.BudgetDao
import com.example.arthguard.features.dashboard.data.local.ExpenseDao
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.repository.ExpenseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    @Singleton
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    @Singleton
    fun provideExpenseRepository(dao: ExpenseDao): ExpenseRepository {
        return ExpenseRepositoryImpl(dao)
    }
}
