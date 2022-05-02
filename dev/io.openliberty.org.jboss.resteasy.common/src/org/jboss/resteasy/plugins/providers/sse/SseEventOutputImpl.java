package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
import org.jboss.resteasy.core.ServerResponseWriter;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.Cleanable;
import org.jboss.resteasy.plugins.server.Cleanables;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.util.FindAnnotation;

public class SseEventOutputImpl extends GenericType<OutboundSseEvent> implements SseEventSink
{
   private static final Logger LOG = Logger.getLogger(SseEventOutputImpl.class);

   // States
   private static final int READY = 0;
   private static final int PROCESSING = 1;
   private static final int PASSTHROUGH = 2;
   private static final int CLOSED = 3;

   private final MessageBodyWriter<OutboundSseEvent> writer;

   private final ResteasyAsynchronousContext asyncContext;

   private final HttpResponse response;

   private final HttpRequest request;

   private final Map<Class<?>, Object> contextDataMap;

   private volatile boolean responseFlushed = false;

   private final Object lock;

   private final ResteasyProviderFactory providerFactory;
   private final AtomicInteger state;
   private final Deque<FutureEvent> events;

   @Deprecated
   public SseEventOutputImpl(final MessageBodyWriter<OutboundSseEvent> writer)
   {
      this(writer, ResteasyProviderFactory.getInstance());
   }

   public SseEventOutputImpl(final MessageBodyWriter<OutboundSseEvent> writer, final ResteasyProviderFactory providerFactory)
   {
      this.writer = writer;
      contextDataMap = ResteasyContext.getContextDataMap();
      this.providerFactory = providerFactory;
      request = ResteasyContext.getRequiredContextData(org.jboss.resteasy.spi.HttpRequest.class);
      asyncContext = request.getAsyncContext();

      if (!asyncContext.isSuspended())
      {
         try
         {
            asyncContext.suspend();
         }
         catch (IllegalStateException ex)
         {
            LogMessages.LOGGER.failedToSetRequestAsync();
         }
      }

      response = ResteasyContext.getRequiredContextData(HttpResponse.class);
      try {
         // This is an odd use-case for a lock. However, the AsyncOutputStream locks on itself which could lead to
         // deadlocks if we use our own lock. Using the output stream as the lock avoids issues like this.
         lock = response.getAsyncOutputStream();
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
      state = new AtomicInteger(READY);
      events = new ConcurrentLinkedDeque<>();
   }

   @Override
   public void close()
   {
      close(true, null);
   }

   @Deprecated
   protected void close(boolean flushBeforeClose)
   {
      close(flushBeforeClose, null);
   }

   public void clearContextData()
   {
      Map<Class<?>, Object> map = ResteasyContext.getContextDataMap(false);
      Cleanables cleanables = map != null ? (Cleanables) map.get(Cleanables.class) : null;
      if (cleanables != null)
      {
         for (Iterator<Cleanable> it = cleanables.getCleanables().iterator(); it.hasNext(); )
         {
            try
            {
               it.next().clean();
            }
            catch(Exception e)
            {
               // Empty
            }
         }
         ResteasyContext.clearContextData();
      }
   }

   protected void flushResponseToClient()
   {
      internalFlushResponseToClient(false);
   }

   private CompletionStage<Void> internalFlushResponseToClient(boolean throwIOException)
   {
      synchronized (lock)
      {
         if (!responseFlushed)
         {
            final BuiltResponse jaxrsResponse = createResponse();
            try
            {
               CompletableFuture<Void> ret = new CompletableFuture<>();
               ServerResponseWriter.writeNomapResponse(jaxrsResponse, request, response,
                     providerFactory, t -> {
                        AsyncOutputStream aos;
                        try
                        {
                           aos = response.getAsyncOutputStream();
                        } catch (IOException x)
                        {
                           close(false, x);
                           ret.completeExceptionally(x);
                           return;
                        }
                        // eager composition to guarantee ordering
                          CompletionStage<Void> a = aos.asyncWrite(SseConstants.DOUBLE_EOL)
                                  .thenCompose(v ->  aos.asyncFlush());
                        // we've queued a response flush, so avoid a second one being queued
                        responseFlushed = true;

                        a.thenAccept(v -> {
                           ret.complete(null);
                        }).exceptionally(e -> {
                           if(e instanceof CompletionException)
                              e = e.getCause();
                           if(e instanceof IOException)
                              close(false, e);
                           if(throwIOException)
                              ret.completeExceptionally(e);
                           else
                              ret.completeExceptionally(new ProcessingException(Messages.MESSAGES.failedToCreateSseEventOutput(), e));
                           return null;
                        });
                  }, true);
               return ret;
            }
            catch (IOException e)
            {
               close(false, e);
               CompletableFuture<Void> ret = new CompletableFuture<>();
               if (throwIOException)
               {
                  ret.completeExceptionally(e);
               } else {
                  ret.completeExceptionally(new ProcessingException(Messages.MESSAGES.failedToCreateSseEventOutput(), e));
               }
               return ret;
            }
         }
      }
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public boolean isClosed()
   {
      return state.get() == CLOSED;
   }

   @Override
   public CompletionStage<?> send(OutboundSseEvent event)
   {
      final int state = this.state.get();
      if (state == CLOSED) {
         // FIXME: should be this
         // CompletableFuture<?> ret = new CompletableFuture<>();
         // ret.completeExceptionally(new IllegalStateException(Messages.MESSAGES.sseEventSinkIsClosed()));
         // return ret;
         // But the TCK expects a real exception
         throw new IllegalStateException(Messages.MESSAGES.sseEventSinkIsClosed());
      }
      if (state == PASSTHROUGH) {
         synchronized (lock) {
            return internalWriteEvent(event);
         }
      } else if (state == PROCESSING) {
         final FutureEvent futureEvent = new FutureEvent(event);
         events.addLast(futureEvent);
         return futureEvent.future
                 .thenRun(this::drainQueue);
      }
      final FutureEvent futureEvent = new FutureEvent(event);
      events.addLast(futureEvent);
      return internalFlushResponseToClient(true)
              .thenRun(this::drainQueue)
              .exceptionally((e) -> {
                 if (e instanceof CompletionException)
                    e = e.getCause();
                 if (e instanceof IOException)
                    close(false, e);
                 LogMessages.LOGGER.failedToWriteSseEvent(event.toString(), e);
                 SynchronousDispatcher.rethrow(e);
                 // never reached
                 return null;
              });
   }

   @Deprecated
   protected CompletionStage<Void> writeEvent(OutboundSseEvent event)
   {
      synchronized (lock)
      {
         return internalWriteEvent(event);
      }
   }



   private CompletionStage<Void> internalWriteEvent(final OutboundSseEvent event)
   {
      synchronized (lock)
      {
         try(CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap))
         {
            if (event != null)
            {
               //// Check media type?
               ByteArrayOutputStream bout = new ByteArrayOutputStream();
               MediaType mediaType = event.getMediaType();
               boolean mediaTypeSet = !(event instanceof OutboundSseEventImpl) || ((OutboundSseEventImpl) event).isMediaTypeSet();
               if (mediaType == null || !mediaTypeSet)
               {
                  Object o = response.getOutputHeaders().getFirst("Content-Type");
                  if (o != null)
                  {
                     if (o instanceof MediaType)
                     {
                        MediaType mt = (MediaType) o;
                        String s = mt.getParameters().get(SseConstants.SSE_ELEMENT_MEDIA_TYPE);
                        if (s != null)
                        {
                           mediaType = MediaType.valueOf(s);
                        }
                     }
                     else if (o instanceof String)
                     {
                        MediaType mt = MediaType.valueOf((String) o);
                        String s = mt.getParameters().get(SseConstants.SSE_ELEMENT_MEDIA_TYPE);
                        if (s != null)
                        {
                           mediaType = MediaType.valueOf(s);
                        }
                     }
                     else
                     {
                        throw new RuntimeException(Messages.MESSAGES.expectedStringOrMediaType(o));
                     }
                  }
               }
               if (mediaType == null)
               {
                  mediaType = MediaType.TEXT_PLAIN_TYPE;
               }
               if (event instanceof OutboundSseEventImpl)
               {
                  ((OutboundSseEventImpl) event).setMediaType(mediaType);
               }
               writer.writeTo(event, event.getClass(), null, new Annotation[]{}, mediaType, null, bout);
               AsyncOutputStream aos = response.getAsyncOutputStream();
               // eager composition to guarantee ordering
               return aos.asyncWrite(bout.toByteArray())
                       .thenCompose(v ->  aos.asyncFlush())
                       .exceptionally(e -> {
                          if(e instanceof CompletionException)
                             e = e.getCause();
                          if(e instanceof IOException)
                             close(false, e);
                          LogMessages.LOGGER.failedToWriteSseEvent(event.toString(), e);
                          SynchronousDispatcher.rethrow(e);
                          // never reached
                          return null;
                       });
            }
         }
         catch (IOException e)
         {
            //The connection could be broken or closed. whenever IO error happens, mark closed to true to
            //stop event writing
            close(false, e);
            LogMessages.LOGGER.failedToWriteSseEvent(event.toString(), e);
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(e);
            return ret;
         }
         catch (Exception e)
         {
            LogMessages.LOGGER.failedToWriteSseEvent(event.toString(), e);
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(new ProcessingException(e));
            return ret;
         }
      }
      return CompletableFuture.completedFuture(null);
   }

   private BuiltResponse createResponse() {
      BuiltResponse jaxrsResponse;
      if (state.get() == CLOSED)
      {
         //jaxrsResponse = (BuiltResponse) Response.noContent().build();
         jaxrsResponse = (BuiltResponse) Response.ok(null, MediaType.SERVER_SENT_EVENTS_TYPE).build(); //Liberty change - use 200 instead of 204
      }
      else //set back to client 200 OK to implies the SseEventOutput is ready
      {
         ResourceMethodInvoker method =(ResourceMethodInvoker) request.getAttribute(ResourceMethodInvoker.class.getName());
         MediaType[] mediaTypes = method.getProduces();
         if (mediaTypes != null &&  getSseEventType(mediaTypes) != null)
         {
            // @Produces("text/event-stream")
            SseElementType sseElementType = FindAnnotation.findAnnotation(method.getMethodAnnotations(),SseElementType.class);
            if (sseElementType != null)
            {
               // Get element media type from @SseElementType.
               Map<String, String> parameterMap = new HashMap<>();
               parameterMap.put(SseConstants.SSE_ELEMENT_MEDIA_TYPE, sseElementType.value());
               MediaType mediaType = new MediaType(MediaType.SERVER_SENT_EVENTS_TYPE.getType(), MediaType.SERVER_SENT_EVENTS_TYPE.getSubtype(), parameterMap);
               jaxrsResponse = (BuiltResponse) Response.ok().type(mediaType).build();
            }
            else
            {
               // No element media type declared.
               jaxrsResponse = (BuiltResponse) Response.ok().type(getSseEventType(mediaTypes)).build();
//                   // use "element-type=text/plain"?
            }
         }
         else
         {
            Stream stream = FindAnnotation.findAnnotation(method.getMethodAnnotations(),Stream.class);
            if (stream != null)
            {
               // Get element media type from @Produces.
               jaxrsResponse = (BuiltResponse) Response.ok("").build();
               MediaType elementType = ServerResponseWriter.getResponseMediaType(jaxrsResponse, request, response, providerFactory, method);
               Map<String, String> parameterMap = new HashMap<>();
               parameterMap.put(SseConstants.SSE_ELEMENT_MEDIA_TYPE, elementType.toString());
               String[] streamType = getStreamType(method);
               MediaType mediaType = new MediaType(streamType[0], streamType[1], parameterMap);
               jaxrsResponse = (BuiltResponse) Response.ok().type(mediaType).build();
            }
            else
            {
               throw new RuntimeException(Messages.MESSAGES.expectedStreamOrSseMediaType());
            }
         }
      }
      return jaxrsResponse;
   }

   private String[] getStreamType(ResourceMethodInvoker method)
   {
      Stream stream = FindAnnotation.findAnnotation(method.getMethodAnnotations(),Stream.class);
      Stream.MODE mode = stream != null ? stream.value() : null;
      if (mode == null)
      {
         return new String[]{"text", "event-stream"};
      }
      else if (Stream.MODE.GENERAL.equals(mode))
      {
         return new String[] {"application", "x-stream-general"};
      }
      else if (Stream.MODE.RAW.equals(mode))
      {
         return new String[] {"application", "x-stream-raw"};
      }
      throw new RuntimeException(Messages.MESSAGES.expectedStreamModeGeneralOrRaw(mode));
   }

   @Override
   public boolean equals(Object o) {
      return this == o;
   }

   @Override
   public int hashCode()
   {
      // required by checkcode
      return super.hashCode();
   }
   private MediaType getSseEventType(MediaType[] mediaTypes) {
      for (MediaType type : mediaTypes) {
        if (type.getType().equalsIgnoreCase(MediaType.SERVER_SENT_EVENTS_TYPE.getType())
              && type.getSubtype().equalsIgnoreCase(MediaType.SERVER_SENT_EVENTS_TYPE.getSubtype()))
        {
           return type;
        }
      }
      return null;
  }

   private void close(final boolean flushBeforeClose, final Throwable error) {
      // avoid even attempting to get a lock if someone else has closed it or is closing it
      if (state.getAndSet(CLOSED) != CLOSED) {
         if (error != null) {
            drainQueue(error);
         } else {
            synchronized (lock) {
               events.clear();
            }
         }
         if (flushBeforeClose && responseFlushed) {
            try (CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap)) {
               // make sure we flush to await for any queued data being sent
               AsyncOutputStream aos = response.getAsyncOutputStream();
               aos.asyncFlush().toCompletableFuture().get();
            } catch (IOException | InterruptedException | ExecutionException x) {
               // ignore it and let's just close
            }
         }
         if (asyncContext.isSuspended()) {
            ResteasyAsynchronousResponse asyncResponse = asyncContext.getAsyncResponse();
            if (asyncResponse != null) {
               try {
                  asyncResponse.complete();
               } catch (RuntimeException x) {
                  Throwable cause = x;
                  while (cause.getCause() != null && cause.getCause() != cause)
                     cause = cause.getCause();
                  if (cause instanceof IOException) {
                     // ignore it, we're closed now
                  } else {
                     LOG.debug(cause.getMessage());
                     return;
                  }
               }
            }
         }
         clearContextData();
      }
   }

   private void drainQueue() {
      state.compareAndSet(READY, PROCESSING);
      synchronized (lock) {
         drainQueue(null);
      }
      // We block here to ensure that events don't pass through until we drain the queue one additional time.
      synchronized (lock) {
         // If we're not in a closed state, drain the queue one more time to ensure nothing was added during the
         // previous lock.
         if (state.compareAndSet(PROCESSING, PASSTHROUGH)) {
            drainQueue(null);
         }
      }
   }

   private void drainQueue(final Throwable throwable) {
      FutureEvent event;
      final AtomicReference<Throwable> thrown = new AtomicReference<>(null);
      while ((event = events.pollFirst()) != null) {
         final Throwable t = throwable == null ? thrown.get() : throwable;
         if (t != null) {
            event.future.completeExceptionally(t);
         } else {
            final OutboundSseEvent e = event.event;
            final CompletableFuture<Void> future = event.future;
            internalWriteEvent(e)
                    .thenRun(() -> future.complete(null))
                    .exceptionally((error) -> {
                       LOG.debugf("Failed to process event %s - %s", future, e);
                       thrown.set(error);
                       future.completeExceptionally(error);
                       SynchronousDispatcher.rethrow(error);
                       return null;
                    });
         }
      }
   }

   private static class FutureEvent {
      final CompletableFuture<Void> future;
      final OutboundSseEvent event;

      private FutureEvent(final OutboundSseEvent event) {
         this.event = event;
         future = new CompletableFuture<>();
      }
   }
}
