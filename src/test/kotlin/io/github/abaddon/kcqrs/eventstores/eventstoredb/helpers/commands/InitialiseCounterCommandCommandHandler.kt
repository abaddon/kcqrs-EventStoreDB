//package io.github.abaddon.kcqrs.eventstores.eventstoredb.sample.counter.commands
//
//import io.github.abaddon.kcqrs.core.domain.AggregateHandler
//import io.github.abaddon.kcqrs.core.persistence.IRepository
//import io.github.abaddon.kcqrs.eventstores.eventstoredb.sample.counter.entities.CounterAggregateRoot
//import org.slf4j.LoggerFactory
//import java.util.*
//
//class InitialiseCounterCommandCommandHandler(repository: IRepository<CounterAggregateRoot>) : AggregateHandler<InitialiseCounterCommand, CounterAggregateRoot>(
//    repository,
//    LoggerFactory.getLogger(InitialiseCounterCommandCommandHandler::class.java.simpleName)
//) {
//    override suspend fun handle(command: InitialiseCounterCommand) {
//        val aggregate = CounterAggregateRoot.initialiseCounter(command.aggregateID,command.value)
//        repository.save(aggregate, UUID.randomUUID(), mapOf())
//    }
//
//}
//
///*
//{
//      try
//      {
//          var entity = DailyProgramming.CreateDailyProgramming((DailyProgrammingId)command.AggregateId, command.MovieId, command.ScreenId, command.Date, command.Seats, command.MovieTitle, command.ScreenName);
//          await Repository.Save(entity, Guid.NewGuid(), headers => { });
//      }
//      catch (Exception e)
//      {
//        Logger.LogError($"CreateDailyProgrammingCommand: Error processing the command: {e.Message} - StackTrace: {e.StackTrace}");
//        throw;
//      }
//    }
// */