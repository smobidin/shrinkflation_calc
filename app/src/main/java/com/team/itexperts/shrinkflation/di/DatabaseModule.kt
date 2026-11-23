package com.team.itexperts.shrinkflation.di

import android.content.Context
import androidx.room.Room
import com.team.itexperts.shrinkflation.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "shr_calc.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideProductDao(db: AppDatabase) = db.productDao()

    @Provides
    fun provideProductVariantDao(db: AppDatabase) = db.productVariantDao()

    @Provides
    fun providePriceEntryDao(db: AppDatabase) = db.priceEntryDao()
}
