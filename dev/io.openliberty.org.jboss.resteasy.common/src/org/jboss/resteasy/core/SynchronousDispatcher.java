package org.jboss.resteasy.core;

import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.plugins.server.Cleanable;
import org.jboss.resteasy.plugins.server.Cleanables;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.RequestImpl;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpRequestPreprocessor;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InternalServerErrorException;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnhandledException;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SynchronousDispatcher implements Dispatcher
{
   protected ResteasyProviderFactory providerFactory;
   protected Registry registry;
   protected List<HttpRequestPreprocessor> requestPreprocessors = new ArrayList<HttpRequestPreprocessor>();
   @SuppressWarnings("rawtypes")
   protected Map<Class, Object> defaultContextObjects = new HashMap<Class, Object>();
   protected Set<String> unwrappedExceptions = new HashSet<String>();
   protected boolean bufferExceptionEntityRead = false;
   protected boolean bufferExceptionEntity = true;

   {
      // This is to make sure LogMessages are preloaded as profiler shows a runtime hit
      // This will also insure that this initialization is done at static init time when loaded with Graal
      // Not a big deal if you remove this.
      @SuppressWarnings("unused")
      LogMessages preload = LogMessages.LOGGER;
   }

   public SynchronousDispatcher(final ResteasyProviderFactory providerFactory)
   {
      this.providerFactory = providerFactory;
      this.registry = new ResourceMethodRegistry(providerFactory);
      defaultContextObjects.put(Providers.class, providerFactory);
      defaultContextObjects.put(Registry.class, registry);
      defaultContextObjects.put(Dispatcher.class, this);
      defaultContextObjects.put(InternalDispatcher.class, InternalDispatcher.getInstance());
   }

   public SynchronousDispatcher(final ResteasyProviderFactory providerFactory, final ResourceMethodRegistry registry)
   {
      this(providerFactory);
      this.registry = registry;
      defaultContextObjects.put(Registry.class, registry);
   }

   public ResteasyProviderFactory getProviderFactory()
   {
      return providerFactory;
   }

   public Registry getRegistry()
   {
      return registry;
   }

   public Map<Class, Object> getDefaultContextObjects()
   {
      return defaultContextObjects;
   }

   public Set<String> getUnwrappedExceptions()
   {
      return unwrappedExceptions;
   }

   /*
    * TODO: refactor this method
    * This only used by org.jboss.restesy.springmvc.ResteasyHandlerMapping
    * And most of the code is same with the other preprocess method.
    * We should consider to refactor this method to reuse part of the code with
    * another one.
    */
   public Response preprocess(HttpRequest request) {
      RESTEasyTracingLogger.initTracingSupport(providerFactory, request);
      Response aborted = null;

      RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);

      try {
         final long totalTimestamp = tracingLogger.timestamp("PRE_MATCH_SUMMARY");
         for (HttpRequestPreprocessor preprocessor : this.requestPreprocessors) {
            final long timestamp = tracingLogger.timestamp("PRE_MATCH");
            preprocessor.preProcess(request);
            tracingLogger.logDuration("PRE_MATCH", timestamp, preprocessor.getClass().toString());
         }
         tracingLogger.logDuration("PRE_MATCH_SUMMARY", totalTimestamp, this.requestPreprocessors.size());
         ContainerRequestFilter[] requestFilters = providerFactory.getContainerRequestFilterRegistry().preMatch();
         // FIXME: support async
         PreMatchContainerRequestContext requestContext = new PreMatchContainerRequestContext(request, requestFilters, null);
         aborted = requestContext.filter();
      } catch (Exception e) {
         //logger.error("Failed in preprocess, mapping exception", e);
         aborted = new ExceptionHandler(providerFactory, unwrappedExceptions).handleException(request, e);
      }


      return aborted;
   }

   /**
    * Call pre-process ContainerRequestFilters.
    *
    * @param request http request
    * @param response http response
    * @param continuation runnable
    */
   protected void preprocess(HttpRequest request, HttpResponse response, Runnable continuation) {
      Response aborted = null;
      PreMatchContainerRequestContext requestContext = null;

      RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);

      try {
         final long totalTimestamp = tracingLogger.timestamp("PRE_MATCH_SUMMARY");
         for (HttpRequestPreprocessor preprocessor : this.requestPreprocessors) {
            final long timestamp = tracingLogger.timestamp("PRE_MATCH");
            preprocessor.preProcess(request);
            tracingLogger.logDuration("PRE_MATCH", timestamp, preprocessor.getClass().toString());
         }
         tracingLogger.logDuration("PRE_MATCH_SUMMARY", totalTimestamp, this.requestPreprocessors.size());
         ContainerRequestFilter[] requestFilters = providerFactory.getContainerRequestFilterRegistry().preMatch();
         requestContext = new PreMatchContainerRequestContext(request, requestFilters,
            () -> {
               continuation.run();
               return null;
            });
         aborted = requestContext.filter();
      } catch (Exception e) {
         //logger.error("Failed in preprocess, mapping exception", e);
         // we only want to catch exceptions happening in the filters, not in the continuation
         if (requestContext == null || !requestContext.startedContinuation()) {
            writeException(request, response, e, t -> {
            });

            rethrow(e);  //Liberty change
    //        return;    //Liberty change
          } else {
            rethrow(e);
         }
      }
      if (aborted != null) {
         tracingLogger.log("FINISHED", response.getStatus());
         tracingLogger.flush(response.getOutputHeaders());
         writeResponse(request, response, aborted);
         return;
      }
   }

   @SuppressWarnings("unchecked")
   public static <T extends Throwable> void rethrow(Throwable t) throws T
   {
      throw (T)t;
   }

   @Deprecated
   public void writeException(HttpRequest request, HttpResponse response, Throwable e)
   {
      writeException(request, response, e, t -> {});
   }

   public void writeException(HttpRequest request, HttpResponse response, Throwable e, Consumer<Throwable> onComplete)
   {
       if (!bufferExceptionEntityRead)
       {
           bufferExceptionEntityRead = true;
           ResteasyConfiguration context = ResteasyContext.getContextData(ResteasyConfiguration.class);
           if (context != null)
           {
               String s = context.getParameter("resteasy.buffer.exception.entity");
               if (s != null)
               {
                   bufferExceptionEntity = Boolean.parseBoolean(s);
               }
           }
       }
       if (response.isCommitted() && response.suppressExceptionDuringChunkedTransfer())
       {
           LogMessages.LOGGER.debug(Messages.MESSAGES.responseIsCommitted());
           onComplete.accept(null);
           //Liberty change start
           while (e.getCause() != null) {
               e = e.getCause();
           }

           rethrow(e);
           //        return;
           //Liberty change stop
       }

       Response handledResponse = new ExceptionHandler(providerFactory, unwrappedExceptions).handleException(request, e);
      if (handledResponse == null) throw new UnhandledException(e);
       if (!bufferExceptionEntity)
       {
           response.getOutputHeaders().add("resteasy.buffer.exception.entity", "false");
       }
       try
       {
           ServerResponseWriter.writeNomapResponse(((BuiltResponse) handledResponse), request, response, providerFactory, onComplete);
       }
       catch (Exception e1)
       {
           throw new UnhandledException(e1);
       } finally {
           RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
           tracingLogger.log("FINISHED", response.getStatus());
           tracingLogger.flush(response.getOutputHeaders());
       }
   }


   public void invoke(HttpRequest request, HttpResponse response)
   {
      RESTEasyTracingLogger.initTracingSupport(providerFactory, request);
      RESTEasyTracingLogger.logStart(request);

      try
      {
         pushContextObjects(request, response);
         preprocess(request, response, () -> {
            ResourceInvoker invoker = null;
            try
            {
               try
               {
                  invoker = getInvoker(request);
               }
               catch (Exception exception)
               {
                  //logger.error("getInvoker() failed mapping exception", exception);
                  writeException(request, response, exception, t -> {});
                  return;
               }
               invoke(request, response, invoker);
            }
            finally
            {
               // we're probably clearing it twice but still required
               clearContextData();
            }
         });
      }
      finally
      {
         clearContextData();
      }
   }

   /**
    * Propagate NotFoundException.  This is used for Filters.
    *
    * @param request http request
    * @param response http response
    */
   public void invokePropagateNotFound(HttpRequest request, HttpResponse response) throws NotFoundException
   {
      try
      {
         pushContextObjects(request, response);
         preprocess(request, response, () -> {
            ResourceInvoker invoker = null;
            try
            {
               try
               {
                  invoker = getInvoker(request);
               }
               catch (Exception failure)
               {
                  if (failure instanceof NotFoundException)
                  {
                     throw ((NotFoundException) failure);
                  }
                  else
                  {
                     //logger.error("getInvoker() failed mapping exception", failure);
                     writeException(request, response, failure, t->{});
                     return;
                  }
               }
               invoke(request, response, invoker);
            }
            finally
            {
               // we're probably clearing it twice but still required
               clearContextData();
            }
         });
      }
      finally
      {
         clearContextData();
      }

   }

   public ResourceInvoker getInvoker(HttpRequest request)
         throws Failure
   {
      LogMessages.LOGGER.pathInfo(request.getUri().getPath());
      if (!request.isInitial())
      {
         throw new InternalServerErrorException(Messages.MESSAGES.isNotInitialRequest(request.getUri().getPath()));
      }
      ResourceInvoker invoker = registry.getResourceInvoker(request);
      if (invoker == null)
      {
         throw new NotFoundException(Messages.MESSAGES.unableToFindJaxRsResource(request.getUri().getPath()));
      }
      RESTEasyTracingLogger logger = RESTEasyTracingLogger.getInstance(request);
      logger.log("MATCH_RESOURCE", invoker);
      logger.log("MATCH_RESOURCE_METHOD", invoker.getMethod());
      return invoker;
   }

   @SuppressWarnings("unchecked")
   public void pushContextObjects(final HttpRequest request, final HttpResponse response)
   {
      @SuppressWarnings("rawtypes")
      Map contextDataMap = ResteasyContext.getContextDataMap();
      contextDataMap.put(HttpRequest.class, request);
      contextDataMap.put(HttpResponse.class, response);
      contextDataMap.put(HttpHeaders.class, request.getHttpHeaders());
      contextDataMap.put(UriInfo.class, request.getUri());
      contextDataMap.put(Request.class, new RequestImpl(request, response));
      contextDataMap.put(ResteasyAsynchronousContext.class, request.getAsyncContext());
      ResourceContext resourceContext = new ResourceContext()
      {
         @Override
         public <T> T getResource(Class<T> resourceClass)
         {
            return providerFactory.injectedInstance(resourceClass, request, response);
         }

         @Override
         public <T> T initResource(T resource)
         {
            providerFactory.injectProperties(resource, request, response);
            return resource;
         }
      };
      contextDataMap.put(ResourceContext.class, resourceContext);

      contextDataMap.putAll(defaultContextObjects);
      contextDataMap.put(Cleanables.class, new Cleanables());
      contextDataMap.put(PostResourceMethodInvokers.class, new PostResourceMethodInvokers());
   }

   public Response internalInvocation(HttpRequest request, HttpResponse response, Object entity)
   {
      // be extra careful in the clean up process. Only pop if there was an
      // equivalent push.
      ResteasyContext.addContextDataLevel();
      boolean pushedBody = false;
      try
      {
         MessageBodyParameterInjector.pushBody(entity);
         pushedBody = true;
         ResourceInvoker invoker = getInvoker(request);
         if (invoker != null)
         {
            pushContextObjects(request, response);
            return execute(request, response, invoker);
         }

         // this should never happen, since getInvoker should throw an exception
         // if invoker is null
         return null;
      }
      finally
      {
         ResteasyContext.removeContextDataLevel();
         if (pushedBody)
         {
            MessageBodyParameterInjector.popBody();
         }
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
      // just in case there were internalDispatches that need to be cleaned up
      MessageBodyParameterInjector.clearBodies();
   }

   /**
    * Return a response wither from an invoke or exception handling.
    *
    * @param request http request
    * @param response http response
    * @param invoker resource invoker
    * @return response
    */
   public Response execute(HttpRequest request, HttpResponse response, ResourceInvoker invoker)
   {
      Response jaxrsResponse = null;
      try
      {
         RESTEasyTracingLogger logger = RESTEasyTracingLogger.getInstance(request);
         logger.log("DISPATCH_RESPONSE", jaxrsResponse);

         request.getAsyncContext().initialRequestStarted();
         jaxrsResponse = invoker.invoke(request, response);
         request.getAsyncContext().initialRequestEnded();

         if (request.getAsyncContext().isSuspended())
         {
            /**
             * Callback by the initial calling thread.  This callback will probably do nothing in an asynchronous environment
             * but will be used to simulate AsynchronousResponse in vanilla Servlet containers that do not support
             * asychronous HTTP.
             *
             */
            request.getAsyncContext().getAsyncResponse().initialRequestThreadFinished();
            jaxrsResponse = null; // we're handing response asynchronously
         }
      }
      catch (CompletionException e)
      {
         //logger.error("invoke() failed mapping exception", e);
         jaxrsResponse = new ExceptionHandler(providerFactory, unwrappedExceptions).handleException(request, e.getCause());
         if (jaxrsResponse == null) throw new UnhandledException(e.getCause());
      }
      catch (Exception e)
      {
         //logger.error("invoke() failed mapping exception", e);
         jaxrsResponse = new ExceptionHandler(providerFactory, unwrappedExceptions).handleException(request, e);
         if (jaxrsResponse == null) throw new UnhandledException(e);
      }
      return jaxrsResponse;
   }

   /**
    * Invoke and write response.
    *
    * @param request http request
    * @param response http response
    * @param invoker resource invoker
    */
   public void invoke(HttpRequest request, HttpResponse response, ResourceInvoker invoker)
   {

      RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
      Response jaxrsResponse = null;
      try
      {
         request.getAsyncContext().initialRequestStarted();
         jaxrsResponse = invoker.invoke(request, response);
         request.getAsyncContext().initialRequestEnded();

         tracingLogger.log("DISPATCH_RESPONSE", jaxrsResponse);

         if (request.getAsyncContext().isSuspended())
         {
            /**
             * Callback by the initial calling thread.  This callback will probably do nothing in an asynchronous environment
             * but will be used to simulate AsynchronousResponse in vanilla Servlet containers that do not support
             * asychronous HTTP.
             *
             */
            request.getAsyncContext().getAsyncResponse().initialRequestThreadFinished();
            jaxrsResponse = null; // we're handing response asynchronously
         }
      }
      catch (CompletionException e)
      {
         //logger.error("invoke() failed mapping exception", e);
         writeException(request, response, e.getCause(), t->{});
         return;
      }
      catch (Exception e)
      {

         //logger.error("invoke() failed mapping exception", e);
         invoker.getMethodStatisticsLogger().incFailureCnt();
         writeException(request, response, e, t->{});
         return;
      }

      if (jaxrsResponse != null) {
         writeResponse(request, response, jaxrsResponse);
      }
   }

   @Deprecated
   public void asynchronousDelivery(HttpRequest request, HttpResponse response, Response jaxrsResponse) throws IOException
   {
      asynchronousDelivery(request, response, jaxrsResponse, t -> {});
   }

   public void asynchronousDelivery(HttpRequest request, HttpResponse response, Response jaxrsResponse, Consumer<Throwable> onComplete) throws IOException
   {
      if (jaxrsResponse == null) return;
      try
      {
         pushContextObjects(request, response);
         ServerResponseWriter.writeNomapResponse((BuiltResponse) jaxrsResponse, request, response, providerFactory, onComplete);
      }
      finally
      {
         ResteasyContext.removeContextDataLevel();
      }
   }

   public void unhandledAsynchronousException(HttpResponse response, Throwable ex) {
      LogMessages.LOGGER.unhandledAsynchronousException(ex);
      // unhandled exceptions need to be processed as they can't be thrown back to the servlet container
      
      if (!response.isCommitted()) {
         try
         {
            response.reset();
            response.sendError(500);
         }
         catch (IOException e)
         {

         }
      }
   }

   @Deprecated
   public void asynchronousExceptionDelivery(HttpRequest request, HttpResponse response, Throwable exception)
   {
      asynchronousExceptionDelivery(request, response, exception, t -> {});
   }

   public void asynchronousExceptionDelivery(HttpRequest request, HttpResponse response, Throwable exception, Consumer<Throwable> onComplete)
   {
      try
      {
         pushContextObjects(request, response);
         writeException(request, response, exception, t -> {
            if(t != null)
               unhandledAsynchronousException(response, t);
            onComplete.accept(null);
            ResteasyContext.removeContextDataLevel();
         });
      }
      catch (Throwable ex)
      {
         unhandledAsynchronousException(response, ex);
         onComplete.accept(ex);
      }
   }


   protected void writeResponse(HttpRequest request, HttpResponse response, Response jaxrsResponse)
   {
      try
      {
         ServerResponseWriter.writeNomapResponse((BuiltResponse) jaxrsResponse, request, response, providerFactory,
            t -> {
               if(t != null) {
                  // if we're async we can't trust UnhandledException to be caught
                  if(request.getAsyncContext().isSuspended()
                        && !request.getAsyncContext().isOnInitialRequest()) {
                     try {
                        writeException(request, response, t, t2 -> {});
                     }catch(Throwable ex) {
                        unhandledAsynchronousException(response, ex);
                     }
                  } else {
                     rethrow(t);
                  }
               }
            });
      }
      catch (Exception e)
      {
         //logger.error("writeResponse() failed mapping exception", e);
         writeException(request, response, e, t -> {});
      }
      finally {
         RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
         tracingLogger.log("FINISHED", response.getStatus());
         tracingLogger.flush(response.getOutputHeaders());
      }
   }

   public void addHttpPreprocessor(HttpRequestPreprocessor httpPreprocessor)
   {
      requestPreprocessors.add(httpPreprocessor);
   }

}
