package org.jboss.resteasy.plugins.providers.sse.client;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.plugins.providers.sse.SseConstants;
import org.jboss.resteasy.plugins.providers.sse.SseEventInputImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;

public class SseEventSourceImpl implements SseEventSource
{
   public static final long RECONNECT_DEFAULT = 500;

   private final WebTarget target;

   private final long reconnectDelay;

   private final SseEventSourceScheduler sseEventSourceScheduler;

   private enum State {
      PENDING, OPEN, CLOSED
   }

   private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

   private final AtomicBoolean completionListenersInvoked = new AtomicBoolean(false); // Liberty change

   private final List<Consumer<InboundSseEvent>> onEventConsumers = new CopyOnWriteArrayList<>();

   private final List<Consumer<Throwable>> onErrorConsumers = new CopyOnWriteArrayList<>();

   private final List<Runnable> onCompleteConsumers = new CopyOnWriteArrayList<>();

   private final boolean alwaysReconnect;

   private volatile ClientResponse response;

   public static class SourceBuilder extends Builder
   {
      private WebTarget target = null;

      private long reconnect = RECONNECT_DEFAULT;

      private String name = null;

      private ScheduledExecutorService executor;

      //tck requires this default behavior
      private boolean alwaysReconnect = true;

      public SourceBuilder()
      {
         //NOOP
      }

      public Builder named(String name)
      {
         this.name = name;
         return this;
      }

      public SseEventSource build()
      {
         return new SseEventSourceImpl(target, name, reconnect, false, executor, alwaysReconnect);
      }

      @Override
      public Builder target(WebTarget target)
      {
         if (target == null)
         {
            throw new NullPointerException();
         }
         this.target = target;
         return this;
      }

      @Override
      public Builder reconnectingEvery(long delay, TimeUnit unit)
      {
         reconnect = unit.toMillis(delay);
         return this;
      }

      public Builder executor(ScheduledExecutorService executor)
      {
         this.executor = executor;
         return this;
      }

      public Builder alwaysReconnect(boolean alwaysReconnect) {
         this.alwaysReconnect = alwaysReconnect;
         return this;
      }
   }

   public SseEventSourceImpl(final WebTarget target)
   {
      this(target, true);
   }

   public SseEventSourceImpl(final WebTarget target, final boolean open)
   {
      this(target, null, RECONNECT_DEFAULT, open, null, true);
   }

   private SseEventSourceImpl(final WebTarget target, final String name, final long reconnectDelay, final boolean open, final ScheduledExecutorService executor, final boolean alwaysReconnect)
   {
      if (target == null)
      {
         throw new IllegalArgumentException(Messages.MESSAGES.webTargetIsNotSetForEventSource());
      }
      this.target = target;
      this.reconnectDelay = reconnectDelay;
      this.alwaysReconnect = alwaysReconnect;

      if (executor == null)
      {
         ScheduledExecutorService scheduledExecutor = null;
         if (target instanceof ResteasyWebTarget)
         {
            scheduledExecutor = ((ResteasyWebTarget) target).getResteasyClient().getScheduledExecutor();
         }
         if (name != null) {
            this.sseEventSourceScheduler = new SseEventSourceScheduler(scheduledExecutor, name);
         } else {
            this.sseEventSourceScheduler = new SseEventSourceScheduler(scheduledExecutor, String.format("sse-event-source(%s)", target.getUri()));
         }
      }
      else
      {
         if (name != null) {
            this.sseEventSourceScheduler = new SseEventSourceScheduler(executor, name);
         } else {
            this.sseEventSourceScheduler = new SseEventSourceScheduler(executor, String.format("sse-event-source(%s)", target.getUri()));
         }
      }

      if (open)
      {
         open();
      }
   }

   @Override
   public void open()
   {
      open(null);
   }

   public void open(String lastEventId)
   {
      open(lastEventId, "GET", null, MediaType.SERVER_SENT_EVENTS_TYPE);
   }

   public void open(String lastEventId, String verb, Entity<?> entity, MediaType... mediaTypes)
   {
      if (!state.compareAndSet(State.PENDING, State.OPEN))
      {
         throw new IllegalStateException(Messages.MESSAGES.eventSourceIsNotReadyForOpen());
      }
      EventHandler handler = new EventHandler(reconnectDelay, lastEventId, verb, entity, mediaTypes);
      sseEventSourceScheduler.schedule(handler, 0, TimeUnit.SECONDS);
      handler.awaitConnected();
   }

   @Override
   public boolean isOpen()
   {
      return state.get() == State.OPEN;
   }

   @Override
   public void register(Consumer<InboundSseEvent> onEvent)
   {
      if (onEvent == null)
      {
         throw new IllegalArgumentException();
      }
      onEventConsumers.add(onEvent);
   }

   @Override
   public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError)
   {
      if (onEvent == null)
      {
         throw new IllegalArgumentException();
      }
      if (onError == null)
      {
         throw new IllegalArgumentException();
      }
      onEventConsumers.add(onEvent);
      onErrorConsumers.add(onError);
   }

   @Override
   public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError, Runnable onComplete)
   {
      if (onEvent == null)
      {
         throw new IllegalArgumentException();
      }
      if (onError == null)
      {
         throw new IllegalArgumentException();
      }
      if (onComplete == null)
      {
         throw new IllegalArgumentException();
      }
      onEventConsumers.add(onEvent);
      onErrorConsumers.add(onError);
      onCompleteConsumers.add(onComplete);
   }

   @Override
   public boolean close(final long timeout, final TimeUnit unit)
   {
      internalClose();
      try
      {
         return sseEventSourceScheduler.awaitTermination(timeout, unit);
      }
      catch (InterruptedException e)
      {
         onErrorConsumers.forEach(consumer -> {
            consumer.accept(e);
         });
         Thread.currentThread().interrupt();
         return false;
      }
   }

   // Liberty change start
   private void runCompletionListeners()
   {
       if (!completionListenersInvoked.getAndSet(true)) {
           onCompleteConsumers.forEach(Runnable::run);
       }
   }
   // Liberty change end

   private void internalClose()
   {
      if (state.getAndSet(State.CLOSED) == State.CLOSED)
      {
         return;
      }
      final ClientResponse clientResponse = response;
      if (clientResponse != null)
      {
         try
         {
            clientResponse.releaseConnection(false);
         }
         catch (IOException e)
         {
            onErrorConsumers.forEach(consumer -> {
               consumer.accept(e);
            });
         }
      }
      sseEventSourceScheduler.shutdownNow();
      runCompletionListeners(); // Liberty change
   }

   private class EventHandler implements Runnable
   {

      private final CountDownLatch connectedLatch;

      private String lastEventId;

      private long reconnectDelay;

      private String verb;
      private Entity<?> entity;
      private MediaType[] mediaTypes;

      EventHandler(final long reconnectDelay, final String lastEventId, final String verb, final Entity<?> entity, final MediaType... mediaTypes)
      {
         this.connectedLatch = new CountDownLatch(1);
         this.reconnectDelay = reconnectDelay;
         this.lastEventId = lastEventId;
         this.verb = verb;
         this.entity = entity;
         this.mediaTypes = mediaTypes;
      }

      private EventHandler(final EventHandler anotherHandler)
      {
         this.connectedLatch = anotherHandler.connectedLatch;
         this.reconnectDelay = anotherHandler.reconnectDelay;
         this.lastEventId = anotherHandler.lastEventId;
         this.verb = anotherHandler.verb;
         this.entity = anotherHandler.entity;
         this.mediaTypes = anotherHandler.mediaTypes;
      }

      @Override
      public void run()
      {
         if (state.get() != State.OPEN)
         {
            return;
         }

         SseEventInputImpl eventInput = null;
         InboundSseEvent event = null; // Liberty change - declaring earlier for invocation in two try blocks
         final Providers providers = (ClientConfiguration) target.getConfiguration(); //Liberty change
         try
         {
            final Invocation.Builder requestBuilder = buildRequest(mediaTypes);
            Invocation request = null;
            if (entity == null)
            {
               request = requestBuilder.build(verb);
            }
            else
            {
               request = requestBuilder.build(verb, entity);
            }
            final ClientResponse clientResponse = (ClientResponse) request.invoke();
            response = clientResponse;
            if (Family.SUCCESSFUL.equals(clientResponse.getStatusInfo().getFamily()))
            {
               onConnection();
               // liberty change start
               if (clientResponse.getStatus() == 204) {
                   // per spec, only completion listeners should be invoked on a 204 response - no reconnect
                   runCompletionListeners();
                   return;
               }
               // liberty change end
               eventInput = clientResponse.readEntity(SseEventInputImpl.class);
               //if 200<= response code <300 and response contentType is null, fail the connection.
               if (eventInput == null)
               {
                  if (!alwaysReconnect) {
                     runCompletionListeners(); // Liberty change - just run completion listeners instead of internalClose()
                  } else {
                     reconnect(this.reconnectDelay);
                  }
                  return;
               }
               event = eventInput.read(providers); // Liberty change
            }
            else
            {
               //Let's buffer the entity in case the response contains an entity the user would like to retrieve from the exception.
               //This will also ensure that the connection is correctly closed.
               clientResponse.bufferEntity();
               //Throw an instance of WebApplicationException depending on the response.
               ClientInvocation.handleErrorStatus(clientResponse);
            }
         }
         catch (ServiceUnavailableException ex)
         {
            if (ex.hasRetryAfter())
            {
               try {   // https://issues.redhat.com/browse/RESTEASY-2952
                   onConnection();
                   Date requestTime = new Date();
                   long localReconnectDelay = ex.getRetryTime(requestTime).getTime() - requestTime.getTime();
                   reconnect(localReconnectDelay);
               } catch (Throwable t) {
                    onUnrecoverableError(t);  // https://issues.redhat.com/browse/RESTEASY-2952
               }
            }
            else
            {
               onUnrecoverableError(ex);
            }
            return;
         }
         catch (Throwable e)
         {
            onUnrecoverableError(e);
            return;
         }

         while (!Thread.currentThread().isInterrupted() && state.get() == State.OPEN)
         {
            // Liberty start
            if (event == null && eventInput.isClosed()) {
                reconnect(reconnectDelay);
                break;
            }
            // Liberty end
            if (eventInput != null && eventInput.isClosed()) {
               break;
            }
            try
            {
               if (event != null)
               {
                  onEvent(event);
               }
               //event sink closed
               else if (!alwaysReconnect || eventInput == null || eventInput.isClosed()) // liberty change - check for eventInput == null
               {
                  runCompletionListeners(); // Liberty change - just run completion listeners instead of internalClose()
                  break;
               }
               // Liberty change start - effectively making this a do/while instead of while loop
               event = eventInput.read(providers);
               if (event == null) {
                  runCompletionListeners();
                  break;
               }
               // Liberty change end
            }
            catch (IOException e)
            {
               reconnect(reconnectDelay);
               break;
            }
         }
      }

      public void awaitConnected()
      {
         try
         {
            connectedLatch.await();
         }
         catch (InterruptedException ex)
         {
            Thread.currentThread().interrupt();
         }

      }

      private void onConnection()
      {
         connectedLatch.countDown();
      }

      private void onUnrecoverableError(Throwable throwable)
      {
         connectedLatch.countDown();
         onErrorConsumers.forEach(consumer -> {
            consumer.accept(throwable);
         });
         internalClose();
      }

      private void onEvent(final InboundSseEvent event)
      {
         if (event.getId() != null)
         {
            lastEventId = event.getId();
         }
         if (event.isReconnectDelaySet())
         {
            reconnectDelay = event.getReconnectDelay();
         }
         onEventConsumers.forEach(consumer -> {
            consumer.accept(event);
         });
      }

      private Invocation.Builder buildRequest(MediaType... mediaTypes)
      {
         final Invocation.Builder request = (mediaTypes != null && mediaTypes.length > 0) ? target.request(mediaTypes) : target.request();
         if (lastEventId != null && !lastEventId.isEmpty())
         {
            request.header(SseConstants.LAST_EVENT_ID_HEADER, lastEventId);
         }
         return request;
      }

      private void reconnect(final long delay)
      {
         if (state.get() != State.OPEN)
         {
            return;
         }

         EventHandler processor = new EventHandler(this);
         sseEventSourceScheduler.schedule(processor, delay, TimeUnit.MILLISECONDS);
      }
   }
}
