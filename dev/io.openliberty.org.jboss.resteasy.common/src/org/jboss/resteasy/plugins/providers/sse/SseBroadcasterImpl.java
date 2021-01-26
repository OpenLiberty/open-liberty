package org.jboss.resteasy.plugins.providers.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;

public class SseBroadcasterImpl implements SseBroadcaster
{
   private ConcurrentLinkedQueue<SseEventSink> outputQueue = new ConcurrentLinkedQueue<>();

   private final List<BiConsumer<SseEventSink, Throwable>> onErrorConsumers = new CopyOnWriteArrayList<>();

   private final List<Consumer<SseEventSink>> closeConsumers = new CopyOnWriteArrayList<>();

   private final AtomicBoolean closed = new AtomicBoolean();

   // Used to perform a mutual exclusion between register and close operations
   // since every registered SseEventSink needs to be closed when
   // SseBroadcaster.close() is invoked to prevent leaks due to SseEventSink
   // never closed.
   // Actually most of the time when a SseEventSink is registered to a
   // SseBroadcaster, user is expected its termination to be handled by the
   // SseBroadcaster itself. So user will never call SseEventSink.close() on
   // each SseEventSink he registers but instead he will just call
   // SseBroadcaster.close().
   private final Lock readLock;
   private final Lock writeLock;

   public SseBroadcasterImpl()
   {
      ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
      this.readLock = readWriteLock.readLock();
      this.writeLock = readWriteLock.writeLock();
   }

   @Override
   public void close()
   {
      if (!closed.compareAndSet(false, true))
      {
         return;
      }
      writeLock.lock();
      try
      {
         //Javadoc says close the broadcaster and all subscribed {@link SseEventSink} instances.
         //is it necessay to close the subsribed SseEventSink ?
         outputQueue.forEach(eventSink -> {
            eventSink.close();
            try {
               eventSink.close();
            } catch (RuntimeException e) {
               LogMessages.LOGGER.debug(e.getLocalizedMessage());
            } finally {
               notifyOnCloseListeners(eventSink);
            }
         });
      }
      finally
      {
         writeLock.unlock();
      }
   }

   private void checkClosed()
   {
      if (closed.get())
      {
          throw new IllegalStateException(Messages.MESSAGES.sseBroadcasterIsClosed());
      }
   }

   private void notifyOnCloseListeners(SseEventSink eventSink)
   {
      // First remove the eventSink from the outputQueue to ensure that
      // concurrent calls to this method will notify listeners only once for a
      // given eventSink instance.
      if (outputQueue.remove(eventSink))
      {
         closeConsumers.forEach(consumer -> {
            consumer.accept(eventSink);
         });
      }
   }

   private void notifyOnErrorListeners(SseEventSink eventSink, Throwable throwable)
   {
      // We have to notify close listeners if the SSE event output has been
      // closed (either by client closing the connection (IOException) or by
      // calling SseEventSink.close() (IllegalStateException) on the server
      // side).
      if (throwable instanceof IOException || throwable instanceof IllegalStateException)
      {
         notifyOnCloseListeners(eventSink);
      }
      onErrorConsumers.forEach(consumer -> {
         consumer.accept(eventSink, throwable);
      });
   }

   @Override
   public void onError(BiConsumer<SseEventSink, Throwable> onError)
   {
      checkClosed();
      onErrorConsumers.add(onError);
   }

   @Override
   public void onClose(Consumer<SseEventSink> onClose)
   {
      checkClosed();
      closeConsumers.add(onClose);
   }

   @Override
   public void register(SseEventSink sseEventSink)
   {
      checkClosed();
      readLock.lock();
      try
      {
         checkClosed();
         outputQueue.add(sseEventSink);
      }
      finally
      {
         readLock.unlock();
      }
   }

   @Override
   public CompletionStage<?> broadcast(OutboundSseEvent event)
   {
      checkClosed();
      CompletionStage<?> ret = CompletableFuture.completedFuture(null);
      for (SseEventSink eventSink : outputQueue)
      {
         ret = ret.thenCompose(v -> {
            try {
               return eventSink.send(event)
                     .exceptionally(err -> {
                        // do not propagate the exception to the returned CF
                        // apparently, the goal is to close this sink and not report the error
                        // of the broadcast operation
                        notifyOnErrorListeners(eventSink, err);
                        return null;
                     });
            }
            catch (Exception e)
            {
               // do not propagate the exception to the returned CF
               // apparently, the goal is to close this sink and not report the error
               // of the broadcast operation
               notifyOnErrorListeners(eventSink, e);
               return CompletableFuture.completedFuture(null);
            }
         });
      }
      return ret;
   }

}
