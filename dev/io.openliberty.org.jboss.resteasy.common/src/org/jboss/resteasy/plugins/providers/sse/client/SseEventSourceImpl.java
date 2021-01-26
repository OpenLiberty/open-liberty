package org.jboss.resteasy.plugins.providers.sse.client;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

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

   //Liberty... removed final to allow change.
   private long reconnectDelay;

   private final SseEventSourceScheduler sseEventSourceScheduler;

   private enum State {
      PENDING, OPEN, CLOSED
   }

   private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

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
      //Liberty... setting this to false for now.  Had been true.
      private boolean alwaysReconnect = false;

      public SourceBuilder()
      {
         //NOOP
      }

      public Builder named(String name)
      {
         System.out.println("SourceBuilder.named: " + name);
         this.name = name;
         return this;
      }

      public SseEventSource build()
      {
          System.out.println("SourceBuilder.build");
         return new SseEventSourceImpl(target, name, reconnect, false, executor, alwaysReconnect);
      }

      @Override
      public Builder target(WebTarget target)
      {
          System.out.println("SourceBuilder.target: " + target.toString());

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
          System.out.println("SourceBuilder.reconnectingEvery");
         reconnect = unit.toMillis(delay);
         return this;
      }

      public Builder executor(ScheduledExecutorService executor)
      {
          System.out.println("SourceBuilder.executor");

         this.executor = executor;
         return this;
      }

      public Builder alwaysReconnect(boolean alwaysReconnect) {
          System.out.println("SourceBuilder.alwaysReconnect: " + alwaysReconnect);
         this.alwaysReconnect = alwaysReconnect;
         return this;
      }
   }

   public SseEventSourceImpl(final WebTarget target)
   {
      this(target, true);
      System.out.println("SseEventSourceImpl.init: " + target.toString());
   }

   public SseEventSourceImpl(final WebTarget target, final boolean open)
   {
      this(target, null, RECONNECT_DEFAULT, open, null, true);
      System.out.println("SseEventSourceImpl.init2 ");
   }

   private SseEventSourceImpl(final WebTarget target, final String name, final long reconnectDelay, final boolean open, final ScheduledExecutorService executor, final boolean alwaysReconnect)
   {
       System.out.println("SseEventSourceImpl.init3 ");
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
       System.out.println("SseEventSourceImpl.open() ");
      open(null);
   }

   public void open(String lastEventId)
   {
       System.out.println("SseEventSourceImpl.open()2 " + lastEventId);
      open(lastEventId, "GET", null, MediaType.SERVER_SENT_EVENTS_TYPE);
   }

   public void open(String lastEventId, String verb, Entity<?> entity, MediaType... mediaTypes)
   {
       System.out.println("SseEventSourceImpl.open()3 " + lastEventId);

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
       boolean retVal = state.get() == State.OPEN;
       System.out.println("SseEventSourceImpl.open() "  + retVal);
      return state.get() == State.OPEN;
   }

   @Override
   public void register(Consumer<InboundSseEvent> onEvent)
   {
      System.out.println("SseEventSourceImpl.register()");
      if (onEvent == null)
      {
         throw new IllegalArgumentException();
      }
      onEventConsumers.add(onEvent);
   }

   @Override
   public void register(Consumer<InboundSseEvent> onEvent, Consumer<Throwable> onError)
   {
       System.out.println("SseEventSourceImpl.register()2");
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
       System.out.println("SseEventSourceImpl.register()3");
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
       System.out.println("SseEventSourceImpl.close()");
      internalClose();
      
      //Liberty moved from above internalClose
      //Reset reconnectDelay 
      if (reconnectDelay != 0) {
          reconnectDelay = 0;
      }
      state.set(State.CLOSED);
      sseEventSourceScheduler.shutdownNow();          
      //Liberty end
      
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

   private void internalClose()
   {
       System.out.println("SseEventSourceImpl.internalClose()");

      //Liberty start 
//      if (state.getAndSet(State.CLOSED) == State.CLOSED)
      if (state.get() == State.CLOSED) //Liberty end
      {
         return;
      }
      if (response != null)
      {
         try
         {
            response.releaseConnection(false);
         }
         catch (IOException e)
         {
            onErrorConsumers.forEach(consumer -> {
               consumer.accept(e);
            });
         }
      }
      //Liberty Jim test start
      onCompleteConsumers.forEach(consumer -> {
          consumer.run();
      });
      
      return;
/*      } else {
          //moved from above onCompleteConsumers
          state.set(State.CLOSED);
          sseEventSourceScheduler.shutdownNow();          
      }
*/
      //Liberty end
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
          System.out.println("EventHandler.init");
         this.connectedLatch = new CountDownLatch(1);
         this.reconnectDelay = reconnectDelay;
         this.lastEventId = lastEventId;
         this.verb = verb;
         this.entity = entity;
         this.mediaTypes = mediaTypes;
      }

      private EventHandler(final EventHandler anotherHandler)
      {
          System.out.println("EventHandler.init2");
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
          System.out.println("EventHandler.run()");

         if (state.get() != State.OPEN)
         {
            return;
         }

         SseEventInputImpl eventInput = null;
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
            response = (ClientResponse) request.invoke();
            if (Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily()))
            {
               onConnection();
               eventInput = response.readEntity(SseEventInputImpl.class);
               //if 200<= response code <300 and response contentType is null, fail the connection.
               if (eventInput == null && !alwaysReconnect)
               {
                  internalClose();
                  return;
               }
            }
            else
            {
               //Let's buffer the entity in case the response contains an entity the user would like to retrieve from the exception.
               //This will also ensure that the connection is correctly closed.
               response.bufferEntity();
               //Throw an instance of WebApplicationException depending on the response.
               ClientInvocation.handleErrorStatus(response);
            }
         }
         catch (ServiceUnavailableException ex)
         {
            if (ex.hasRetryAfter())
            {
            // Liberty start
            onConnection();
            Date requestTime = new Date();
            long localReconnectDelay = ex.getRetryTime(requestTime).getTime() - requestTime.getTime();
            // Liberty,  remove this since a ServiceUnavailableException with hasRetryAfter should not
            // invoke the SSE onError methods.
            //         onErrorConsumers.forEach(consumer -> {
            //         consumer.accept(ex);
            //       }); 
			//Liberty end                  
            reconnect(localReconnectDelay);
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

         final Providers providers = (ClientConfiguration) target.getConfiguration();
         while (!Thread.currentThread().isInterrupted() && state.get() == State.OPEN)
         {
            if (eventInput == null || eventInput.isClosed())
            {
               reconnect(reconnectDelay);
               break;
            }
            try
            {
               InboundSseEvent event = eventInput.read(providers);
               if (event != null)
               {
                  onEvent(event);
               }
               //event sink closed
               else if (!alwaysReconnect)               
               {
                  internalClose();
				  
				  //Liberty must reconnect if delay is set
                  if (reconnectDelay != 0) {
                      reconnect(reconnectDelay);
                  }
                  break;
               }
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
          System.out.println("EventHandler.awaitConnected()");
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
          System.out.println("EventHandler.onConnection()");

         connectedLatch.countDown();
      }

      private void onUnrecoverableError(Throwable throwable)
      {
          System.out.println("EventHandler.onUnrecoverableError()");
         connectedLatch.countDown();
         onErrorConsumers.forEach(consumer -> {
            consumer.accept(throwable);
         });
         internalClose();
      }

      private void onEvent(final InboundSseEvent event)
      {
          System.out.println("EventHandler.onEvent()");
         if (event.getId() != null)
         {
            lastEventId = event.getId();
         }
         if (event.isReconnectDelaySet())
         {
            reconnectDelay = event.getReconnectDelay();
			
            //Liberty set reconnectDelay on SseEventSource object
            setReconnectDelay(reconnectDelay);
         }
         onEventConsumers.forEach(consumer -> {
            consumer.accept(event);
         });
      }

      private Invocation.Builder buildRequest(MediaType... mediaTypes)
      {
          System.out.println("EventHandler.buildRequest()");
         final Invocation.Builder request = (mediaTypes != null && mediaTypes.length > 0) ? target.request(mediaTypes) : target.request();
         if (lastEventId != null && !lastEventId.isEmpty())
         {
            request.header(SseConstants.LAST_EVENT_ID_HEADER, lastEventId);
         }
         return request;
      }

      private void reconnect(final long delay)
      {
          System.out.println("EventHandler.reconnect()");
         if (state.get() != State.OPEN)
         {
            return;
         }

         EventHandler processor = new EventHandler(this);
         sseEventSourceScheduler.schedule(processor, delay, TimeUnit.MILLISECONDS);

      }
   }
   // Liberty Start
   public void setReconnectDelay(long l) {
       System.out.println("SseEventSourceImpl.setReconnectDelay(): " + l);
       reconnectDelay = l;
   }
   
   @Override
   public String toString() {
       return (this.getClass().getName() +
               "|target=" + target +
               "|reconnectDelay=" + reconnectDelay +
               "|state=" + state);

   }
// Liberty end


}
