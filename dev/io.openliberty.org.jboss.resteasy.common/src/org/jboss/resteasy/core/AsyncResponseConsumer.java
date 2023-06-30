package org.jboss.resteasy.core;

import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
import org.jboss.resteasy.plugins.providers.sse.OutboundSseEventImpl;
import org.jboss.resteasy.plugins.providers.sse.SseConstants;
import org.jboss.resteasy.plugins.providers.sse.SseImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.BuiltResponseEntityNotBacked;
import org.jboss.resteasy.specimpl.MultivaluedTreeMap;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 * @version $Revision: 1 $
 *
 * The basic idea implemented by AsyncResponseConsumer is that a resource method returns a CompletionStage,
 * an Observable, etc., and some version of AsyncResponseConsumer subscribes to it. Each subclass of
 * AsyncResponseConsumer knows how to handle new data items as they are provided.
 */
public abstract class AsyncResponseConsumer
{
   protected Map<Class<?>, Object> contextDataMap;
   protected ResourceMethodInvoker method;
   protected SynchronousDispatcher dispatcher;
   protected ResteasyAsynchronousResponse asyncResponse;
   protected boolean isComplete;

   public AsyncResponseConsumer(final ResourceMethodInvoker method)
   {
      this.method = method;
      contextDataMap = ResteasyContext.getContextDataMap();
      dispatcher = (SynchronousDispatcher) contextDataMap.get(Dispatcher.class);
      HttpRequest httpRequest = (HttpRequest) contextDataMap.get(HttpRequest.class);
      if(httpRequest.getAsyncContext().isSuspended())
         asyncResponse = httpRequest.getAsyncContext().getAsyncResponse();
      else
         asyncResponse = httpRequest.getAsyncContext().suspend();
   }

   public static AsyncResponseConsumer makeAsyncResponseConsumer(ResourceMethodInvoker method, AsyncResponseProvider<?> asyncResponseProvider) {
      return new CompletionStageResponseConsumer(method, asyncResponseProvider);
   }

   public static AsyncResponseConsumer makeAsyncResponseConsumer(ResourceMethodInvoker method, AsyncStreamProvider<?> asyncStreamProvider) {
      if(method.isSse())
      {
         return new AsyncGeneralStreamingSseResponseConsumer(method, asyncStreamProvider);
      }
      Stream stream = method.getMethod().getAnnotation(Stream.class);
      if (stream != null)
      {
         if (Stream.MODE.RAW.equals(stream.value()))
         {
            return new AsyncRawStreamingResponseConsumer(method, asyncStreamProvider);
         }
         else
         {
            return new AsyncGeneralStreamingSseResponseConsumer(method, asyncStreamProvider);
         }
      }
      return new AsyncStreamCollectorResponseConsumer(method, asyncStreamProvider);
   }

   protected void doComplete() {
      asyncResponse.complete();
   }

   public final synchronized void complete(Throwable t)
   {
      if (!isComplete)
      {
         isComplete = true;
         doComplete();
         asyncResponse.completionCallbacks(t);
         ResteasyContext.removeContextDataLevel();
      }
   }

   protected void internalResume(Object entity, Consumer<Throwable> onComplete)
   {
      try(CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap)){
         HttpRequest httpRequest = (HttpRequest) contextDataMap.get(HttpRequest.class);
         HttpResponse httpResponse = (HttpResponse) contextDataMap.get(HttpResponse.class);

         BuiltResponse builtResponse = createResponse(entity, httpRequest);
         try
         {
            sendBuiltResponse(builtResponse, httpRequest, httpResponse, e -> {
               if(e != null)
               {
                  exceptionWhileResuming(e);
               }
               onComplete.accept(e);
            });
         }
         catch (Throwable e)
         {
            exceptionWhileResuming(e);
            onComplete.accept(e);
         }
      }
   }

   private void exceptionWhileResuming(Throwable e)
   {
      try
      {
         // OK, not funny: if this is not a handled exception, it will just be logged and rethrown, so ignore it and move on
         internalResume(e, t -> {});
      }
      catch(Throwable t2)
      {
      }
      // be done with this stream
      complete(e);
   }

   protected void sendBuiltResponse(BuiltResponse builtResponse, HttpRequest httpRequest, HttpResponse httpResponse, Consumer<Throwable> onComplete) throws IOException
   {
      // send headers only if we're not streaming, or if we're sending the first stream element
      boolean sendHeaders = sendHeaders();
      ServerResponseWriter.writeNomapResponse(builtResponse, httpRequest, httpResponse, dispatcher.getProviderFactory(), onComplete, sendHeaders);
   }

   protected abstract boolean sendHeaders();

   protected void internalResume(Throwable t, Consumer<Throwable> onComplete)
   {
      try(CloseableContext c = ResteasyContext.addCloseableContextDataLevel(contextDataMap)){
         HttpRequest httpRequest = (HttpRequest) contextDataMap.get(HttpRequest.class);
         HttpResponse httpResponse = (HttpResponse) contextDataMap.get(HttpResponse.class);
         try {
            dispatcher.writeException(httpRequest, httpResponse, t, onComplete);
         }catch(Throwable t2) {
            // ignore t2 and report the original exception without going through filters
            dispatcher.unhandledAsynchronousException(httpResponse, t);
            onComplete.accept(t);
            
            throw t2;  //Liberty change
         }
      }
   }

   protected BuiltResponse createResponse(Object entity, HttpRequest httpRequest)
   {
      BuiltResponse builtResponse = null;
      if (entity == null)
      {
         builtResponse = (BuiltResponse) Response.noContent().build();
      }
      else if (entity instanceof BuiltResponse)
      {
         builtResponse = (BuiltResponse) entity;
      }
      else if (entity instanceof Response)
      {
         Response r = (Response) entity;
         Headers<Object> metadata = new Headers<Object>();
         metadata.putAll(r.getMetadata());
         builtResponse = new BuiltResponseEntityNotBacked(r.getStatus(), r.getStatusInfo().getReasonPhrase(),
                 metadata, r.getEntity(),  method.getMethodAnnotations());
      }
      else
      {
         if (method == null)
         {
            throw new IllegalStateException(Messages.MESSAGES.unknownMediaTypeResponseEntity());
         }
         BuiltResponse jaxrsResponse = (BuiltResponse) Response.ok(entity).build();
         // it has to be a Publisher<X>, so extract the X and wrap it around a List<X>
         // FIXME: actually the provider should extract that, because it could come from another type param
         // before conversion to Publisher
         Type unwrappedType = ((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0];
         Type newType = adaptGenericType(unwrappedType);

         jaxrsResponse.setGenericType(newType);
         jaxrsResponse.addMethodAnnotations(method.getMethodAnnotations());
         builtResponse = jaxrsResponse;
      }

      return builtResponse;
   }

   protected Type adaptGenericType(Type unwrappedType)
   {
      return unwrappedType;
   }

   /*
    * As the name indicates, CompletionStageResponseConsumer subscribes to a CompletionStage supplied by
    * a resource method.
    */
   private static class CompletionStageResponseConsumer extends AsyncResponseConsumer implements BiConsumer<Object, Throwable>
   {
      private AsyncResponseProvider<?> asyncResponseProvider;

      CompletionStageResponseConsumer(final ResourceMethodInvoker method, final AsyncResponseProvider<?> asyncResponseProvider)
      {
         super(method);
         this.asyncResponseProvider = asyncResponseProvider;
      }

      @Override
      protected boolean sendHeaders()
      {
         return true;
      }

      @Override
      public void accept(Object t, Throwable u)
      {
         if (t != null || u == null)
         {
            internalResume(t, x -> complete(null));
         }
         else
         {
            // since this is called by the CompletionStage API, we want to unwrap its exceptions in order for the
            // exception mappers to function
            if(u instanceof CompletionException) {
                u = u.getCause();
            }
            Throwable throwable = u;
            internalResume(throwable, x -> complete(throwable));
         }
      }

      @Override
      public void subscribe(Object rtn)
      {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         CompletionStage<?> stage = ((AsyncResponseProvider)asyncResponseProvider).toCompletionStage(rtn);
         stage.whenComplete(this);
      }
   }

   private abstract static class AsyncStreamResponseConsumer extends AsyncResponseConsumer implements Subscriber<Object>
   {
      protected Subscription subscription;
      private AsyncStreamProvider<?> asyncStreamProvider;

      AsyncStreamResponseConsumer(final ResourceMethodInvoker method, final AsyncStreamProvider<?> asyncStreamProvider)
      {
         super(method);
         this.asyncStreamProvider = asyncStreamProvider;
      }

      @Override
      protected void doComplete()
      {
         // we can be done by exception before we've even subscribed
         if(subscription != null)
            subscription.cancel();
         super.doComplete();
      }

      @Override
      public void onComplete()
      {
         complete(null);
      }

      @Override
      public void onError(Throwable t)
      {
         internalResume(t, x -> complete(t));
      }

      /**
       * Subclass to collect the next element and inform if you want more.
       * @param element the next element to collect
       * @return true if you want more elements, false if not
       */
      protected void addNextElement(Object element)
      {
         internalResume(element, t -> {
            if(t != null)
               complete(t);
         });
      }

      @Override
      public void onNext(Object v)
      {
         addNextElement(v);
      }

      @Override
      public void onSubscribe(Subscription subscription)
      {
         this.subscription = subscription;
         subscription.request(1);
      }

      @Override
      public void subscribe(Object rtn)
      {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         Publisher<?> publisher = ((AsyncStreamProvider)asyncStreamProvider).toAsyncStream(rtn);
         publisher.subscribe(this);
      }
   }

   /*
    * AsyncRawStreamingResponseConsumer supports raw streaming, which is invoked when a resource method
    * is annotated with @Stream(Stream.MODE.RAW). In raw streaming, an undelimited sequence of data elements
    * such as bytes or chars is written. The client application is responsible for parsing it.
    */
   private static class AsyncRawStreamingResponseConsumer extends AsyncStreamResponseConsumer
   {
      private boolean sentEntity;
      private boolean onCompleteReceived;
      private volatile boolean sendingEvent;

      AsyncRawStreamingResponseConsumer(final ResourceMethodInvoker method, final AsyncStreamProvider<?> asyncStreamProvider)
      {
         super(method, asyncStreamProvider);
      }

      @Override
      protected void sendBuiltResponse(BuiltResponse builtResponse, HttpRequest httpRequest, HttpResponse httpResponse, Consumer<Throwable> onComplete) throws IOException
      {
         ServerResponseWriter.setResponseMediaType(builtResponse, httpRequest, httpResponse, dispatcher.getProviderFactory(), method);
         boolean resetMediaType = false;
         String mediaTypeString = builtResponse.getHeaderString("Content-Type");
         if (mediaTypeString == null)
         {
            mediaTypeString = MediaType.APPLICATION_OCTET_STREAM;
            resetMediaType = true;
         }
         MediaType mediaType = MediaType.valueOf(mediaTypeString);
         Stream[] streams = method.getMethod().getAnnotationsByType(Stream.class);
         if (streams.length > 0)
         {
            Stream stream = streams[0];
            if (stream.includeStreaming())
            {
               Map<String, String> map = new HashMap<String, String>(mediaType.getParameters());
               map.put(Stream.INCLUDE_STREAMING_PARAMETER, "true");
               mediaType = new MediaType(mediaType.getType(), mediaType.getSubtype(), map);
               resetMediaType = true;
            }
         }
         if (resetMediaType)
         {
            MultivaluedMap<String, Object> headerMap = new MultivaluedTreeMap<String, Object>();
            headerMap.putAll(builtResponse.getHeaders());
            headerMap.remove("Content-Type");
            headerMap.add("Content-Type", mediaType);
            builtResponse.setMetadata(headerMap);
         }
         super.sendBuiltResponse(builtResponse, httpRequest, httpResponse, onComplete);
         sentEntity = true;
      }

      protected void addNextElement(Object element)
      {
         sendingEvent = true;
         internalResume(element, t -> {
            synchronized(this) {
               sendingEvent = false;
               if(onCompleteReceived) {
                  super.onComplete();
               }
               else if(t != null)
               {
                  complete(t);
               }
               else
               {
                  subscription.request(1);
               }
            }
         });
      }

      @Override
      public synchronized void onComplete()
      {
         onCompleteReceived = true;
         if(sendingEvent == false)
            super.onComplete();
      }

      @Override
      protected boolean sendHeaders()
      {
         return !sentEntity;
      }
   }

   /*
    * Rather than writing a stream of data items, AsyncStreamCollectorResponseConsumer collects a sequence
    * of data items into a list and writes the entire list when all data items have been collected.
    */
   private static class AsyncStreamCollectorResponseConsumer extends AsyncStreamResponseConsumer
   {
      private List<Object> collector = new ArrayList<Object>();

      AsyncStreamCollectorResponseConsumer(final ResourceMethodInvoker method, final AsyncStreamProvider<?> asyncStreamProvider)
      {
         super(method, asyncStreamProvider);
      }

      @Override
      protected boolean sendHeaders()
      {
         return true;
      }

      @Override
      protected void addNextElement(Object element)
      {
         collector.add(element);
         subscription.request(1);
      }

      @Override
      public void onComplete()
      {
         internalResume(collector, t -> complete(t));
      }

      @Override
      protected Type adaptGenericType(Type unwrappedType)
      {
         // we want a List<returnType>
         return new ParameterizedType()
         {

            @Override
            public Type[] getActualTypeArguments() {
               return new Type[]{unwrappedType};
            }

            @Override
            public Type getOwnerType() {
               return null;
            }

            @Override
            public Type getRawType() {
               return List.class;
            }
            // FIXME: equals/hashCode/toString?
         };
      }
   }

   /**
    * AsyncGeneralStreamingSseResponseConsumer handles two cases:
    *
    * 1. SSE streaming, and
    * 2. General streaming, which is requested when a resource method is annotated @Stream or @Stream(Stream.MODE.GENERAL).
    *
    * General streaming is an extension of streaming as defined for SSE. The extension include
    * support for encoding non-text data.
    */
   private static class AsyncGeneralStreamingSseResponseConsumer extends AsyncStreamResponseConsumer
   {
      private SseImpl sse;
      private SseEventSink sseEventSink;
      private boolean onCompleteReceived;
      private volatile boolean sendingEvent;

      private AsyncGeneralStreamingSseResponseConsumer(final ResourceMethodInvoker method, final AsyncStreamProvider<?> asyncStreamProvider)
      {
         super(method, asyncStreamProvider);
         sse = new SseImpl();
         sseEventSink = ResteasyContext.getContextData(SseEventSink.class);
      }

      @Override
      protected void doComplete()
      {
         // don't call super.doComplete which completes the asyncContext because Sse does that
         // we can be done by exception before we've even subscribed
         if(subscription != null)
            subscription.cancel();
         sseEventSink.close();
      }

      @Override
      protected void addNextElement(Object element)
      {
         super.addNextElement(element);
      }

      @Override
      public synchronized void onComplete()
      {
         onCompleteReceived = true;
         if(sendingEvent == false)
            super.onComplete();
      }

      @Override
      protected void sendBuiltResponse(BuiltResponse builtResponse, HttpRequest httpRequest, HttpResponse httpResponse, Consumer<Throwable> onComplete)
      {
         ServerResponseWriter.setResponseMediaType(builtResponse, httpRequest, httpResponse, dispatcher.getProviderFactory(), method);
         MediaType elementType = null;
         if (builtResponse.getEntity() instanceof OutboundSseEvent)
         {
            OutboundSseEvent entity = (OutboundSseEvent)builtResponse.getEntity();
            elementType = entity.getMediaType();
         }
         MediaType contentType = null;
         Object o = httpResponse.getOutputHeaders().getFirst("Content-Type");
         if (o != null)
         {
            if (o instanceof String)
            {
               contentType = MediaType.valueOf((String) o);
            }
            else if (o instanceof MediaType)
            {
               contentType = (MediaType) o;
            }
            else
            {
               throw new RuntimeException(Messages.MESSAGES.expectedStringOrMediaType(o));
            }
            if (elementType == null)
            {
               String et = contentType.getParameters().get(SseConstants.SSE_ELEMENT_MEDIA_TYPE);
               elementType = et != null ? MediaType.valueOf(et) : MediaType.TEXT_PLAIN_TYPE;
            }
         }
         else
         {
            throw new RuntimeException(Messages.MESSAGES.expectedStringOrMediaType(o));
         }
         OutboundSseEvent event = sse.newEventBuilder()
            .mediaType(elementType)
            .data(builtResponse.getEntityClass(), builtResponse.getEntity())
            .build();

         if ("application".equals(contentType.getType())
               && "x-stream-general".equals(contentType.getSubtype())
               && event instanceof OutboundSseEventImpl)
         {
            ((OutboundSseEventImpl) event).setEscape(true);
         }
         sendingEvent = true;
         // we can only get onComplete after we return from this method
         try {
            sseEventSink.send(event).whenComplete((val, ex) -> {
               synchronized(this) {
                  sendingEvent = false;
                  if(onCompleteReceived)
                     super.onComplete();
                  else if(ex != null)
                  {
                     // cancel the subscription
                     complete(ex);
                     onComplete.accept(ex);
                  }
                  else
                  {
                     // we're good, ask for the next one
                     subscription.request(1);
                     onComplete.accept(ex);
                  }
               }
            });
         }catch(Exception x) {
            // most likely connection closed
            complete(x);
            onComplete.accept(x);
         }
      }

      @Override
      protected boolean sendHeaders()
      {
         // never actually called since we override sendBuiltResponse
         return false;
      }
   }

   public abstract void subscribe(Object rtn);
}
