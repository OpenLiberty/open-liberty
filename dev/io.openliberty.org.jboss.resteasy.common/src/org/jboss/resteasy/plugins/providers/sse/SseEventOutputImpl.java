package org.jboss.resteasy.plugins.providers.sse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.SseElementType;
import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ServerResponseWriter;
import org.jboss.resteasy.plugins.server.Cleanable;
import org.jboss.resteasy.plugins.server.Cleanables;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
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
   private final MessageBodyWriter<OutboundSseEvent> writer;

   private final ResteasyAsynchronousContext asyncContext;

   private final HttpResponse response;

   private final HttpRequest request;

   private volatile boolean closed;

   private final Map<Class<?>, Object> contextDataMap;

   private volatile boolean responseFlushed = false;

   private final Object lock = new Object();

   private final ResteasyProviderFactory providerFactory;

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
      request = ResteasyContext.getContextData(org.jboss.resteasy.spi.HttpRequest.class);
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

      response = ResteasyContext.getContextData(HttpResponse.class);
   }

   @Override
   public void close()
   {
      close(true);
   }

   protected void close(boolean flushBeforeClose)
   {
      // avoid even attempting to get a lock if someone else has closed it or is closing it
      if(closed)
         return;
      synchronized (lock)
      {
         closed = true;
         if(flushBeforeClose && responseFlushed) {
            try(CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap)){
               // make sure we flush to await for any queued data being sent
               AsyncOutputStream aos = response.getAsyncOutputStream();
               aos.asyncFlush().toCompletableFuture().get();
            }catch(IOException | InterruptedException | ExecutionException x) {
               // ignore it and let's just close
            }
         }
         if (asyncContext.isSuspended())
         {
            ResteasyAsynchronousResponse asyncResponse = asyncContext.getAsyncResponse();
            if (asyncResponse != null)
            {
               try {
                  asyncResponse.complete();
               } catch(RuntimeException x) {
                  Throwable cause = x;
                  while(cause.getCause() != null && cause.getCause() != cause)
                     cause = cause.getCause();
                  if(cause instanceof IOException) {
                     // ignore it, we're closed now
                  }else {
                     LOG.debug(cause.getMessage());
                     return;
                  }
               }
            }
         }
         clearContextData();
      }
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
      // avoid even attempting to get a lock if someone else has flushed the response
      if(responseFlushed)
         return CompletableFuture.completedFuture(null);
      synchronized (lock)
      {
         if (!responseFlushed)
         {
            BuiltResponse jaxrsResponse = null;
            if (this.closed)
            {
               //jaxrsResponse = (BuiltResponse) Response.noContent().build();
               jaxrsResponse = (BuiltResponse) Response.ok().build(); //Liberty change - use 200 instead of 204
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
                     Map<String, String> parameterMap = new HashMap<String, String>();
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
                     Map<String, String> parameterMap = new HashMap<String, String>();
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
                           close(false);
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
                              close(false);
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
               close(false);
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
      return closed;
   }

   @Override
   public CompletionStage<?> send(OutboundSseEvent event)
   {
      synchronized (lock)
      {
         if (closed)
         {
            // FIXME: should be this
//            CompletableFuture<?> ret = new CompletableFuture<>();
//            ret.completeExceptionally(new IllegalStateException(Messages.MESSAGES.sseEventSinkIsClosed()));
//            return ret;
            // But the TCK expects a real exception
            throw new IllegalStateException(Messages.MESSAGES.sseEventSinkIsClosed());
         }
         // eager composition to guarantee ordering
         return internalFlushResponseToClient(true)
                 .thenCompose(v ->  writeEvent(event));
      }
   }

   protected CompletionStage<Void> writeEvent(OutboundSseEvent event)
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
               boolean mediaTypeSet = event instanceof OutboundSseEventImpl ? ((OutboundSseEventImpl) event).isMediaTypeSet() : true;
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
                           close(false);
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
            close(false);
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
}
