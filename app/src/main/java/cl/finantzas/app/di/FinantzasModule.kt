package cl.finantzas.app.di

import cl.aiep.finantzas.domain.rules.AntExpenseRuleEngine
import cl.finantzas.app.domain.repository.InMemoryExpenseRepository
import cl.finantzas.app.domain.repository.ExpenseReaderRepository
import cl.finantzas.app.domain.repository.ExpenseWriterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt con las dependencias de alcance global (vida de la app).
 */
@Module
@InstallIn(SingletonComponent::class)
object FinantzasModule {

    @Provides
    @Singleton
    fun provideAntExpenseRuleEngine(): AntExpenseRuleEngine = AntExpenseRuleEngine()

    @Provides
    fun provideExpenseReaderRepository(impl: InMemoryExpenseRepository): ExpenseReaderRepository = impl

    @Provides
    fun provideExpenseWriterRepository(impl: InMemoryExpenseRepository): ExpenseWriterRepository = impl
}