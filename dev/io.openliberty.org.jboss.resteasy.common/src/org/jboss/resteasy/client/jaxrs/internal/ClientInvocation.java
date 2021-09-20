package org.jboss.resteasy.client.jaxrs.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.WriterInterceptor;

import org.jboss.resteasy.client.exception.WebApplicationExceptionWrapper;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine.ResultExtractor;
import org.jboss.resteasy.client.jaxrs.engines.ReactiveClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ClientInvoker;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
import org.jboss.resteasy.core.interception.jaxrs.AbstractWriterInterceptorContext;
import org.jboss.resteasy.core.interception.jaxrs.ClientWriterInterceptorContext;
import org.jboss.resteasy.plugins.providers.sse.EventInput;
import org.jboss.resteasy.specimpl.MultivaluedTreeMap;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.DelegatingOutputStream;
import org.reactivestreams.Publisher;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientInvocation implements Invocation
{
   protected RESTEasyTracingLogger tracingLogger;

   protected ResteasyClient client;

   protected ClientRequestHeaders headers;

   protected String method;

   protected Object entity;

   protected Type entityGenericType;

   protected Class<?> entityClass;

   protected Annotation[] entityAnnotations;

   protected ClientConfiguration configuration;

   protected URI uri;

   protected boolean chunked;

   protected ClientInvoker clientInvoker;

   protected WebTarget actualTarget;

   // todo need a better solution for this.  Apache Http Client 4 does not let you obtain the OutputStream before executing this request.
   // That is problematic for wrapping the output stream in e.g. a RequestFilter for transparent compressing.
   protected DelegatingOutputStream delegatingOutputStream = new DelegatingOutputStream();

   protected OutputStream entityStream = delegatingOutputStream;

   public ClientInvocation(final ResteasyClient client, final URI uri, final ClientRequestHeaders headers, final ClientConfiguration parent)
   {
      this.uri = uri;
      this.client = client;
      this.configuration = new ClientConfiguration(parent);
      this.headers = headers;

      initTracingSupport();
   }

   private void initTracingSupport() {
      final RESTEasyTracingLogger tracingLogger;

      if (RESTEasyTracingLogger.isTracingConfigALL(configuration)) {
         tracingLogger = RESTEasyTracingLogger.create(this.toString(),
                 configuration,
                 this.toString());
      } else {
         tracingLogger = RESTEasyTracingLogger.empty();
      }

      this.tracingLogger = tracingLogger;

   }

   protected ClientInvocation(final ClientInvocation clientInvocation)
   {
      this.client = clientInvocation.client;
      this.configuration = new ClientConfiguration(clientInvocation.configuration);
      this.headers = new ClientRequestHeaders(this.configuration);
      MultivaluedTreeMap.copy(clientInvocation.headers.getHeaders(), this.headers.headers);
      this.method = clientInvocation.method;
      this.entity = clientInvocation.entity;
      this.entityGenericType = clientInvocation.entityGenericType;
      this.entityClass = clientInvocation.entityClass;
      this.entityAnnotations = clientInvocation.entityAnnotations;
      this.uri = clientInvocation.uri;
      this.chunked = clientInvocation.chunked;
      this.tracingLogger = clientInvocation.tracingLogger;
      this.clientInvoker = clientInvocation.clientInvoker;
   }

   /**
    * Extracts result from response throwing an appropriate exception if not a successful response.
    *
    * @param responseType generic type
    * @param response response entity
    * @param annotations array of annotations
    * @param <T> type
    * @return extracted result of type T
    */
   public static <T> T extractResult(GenericType<T> responseType, Response response, Annotation[] annotations)
   {
      int status = response.getStatus();
      if (status >= 200 && status < 300)
      {
         try
         {
            // Liberty change start - removing early exit if media type is null
            T rtn = response.readEntity(responseType, annotations);
            if (InputStream.class.isInstance(rtn) || Reader.class.isInstance(rtn)
                  || EventInput.class.isInstance(rtn) || Publisher.class.isInstance(rtn))
            {
               if (response instanceof ClientResponse)
               {
                  ClientResponse clientResponse = (ClientResponse) response;
                  clientResponse.noReleaseConnection();
               }
            }
            return rtn;
              
         }
         catch (WebApplicationException wae)
         {
            throw wae;
         }
         catch (Throwable throwable)
         {
            throw new ResponseProcessingException(response, throwable);
         }
         finally
         {
               response.close();
         }
         // Liberty change end - also just closing in the finally block - not every catch block
      }
      try
      {
         // Buffer the entity for any exception thrown as the response may have any entity the user wants
         // We don't want to leave the connection open though.
         String s = String.class.cast(response.getHeaders().getFirst("resteasy.buffer.exception.entity"));
         if (s == null || Boolean.parseBoolean(s))
         {
            response.bufferEntity();
         }
         else
         {
            // close connection
            if (response instanceof ClientResponse)
            {
               try
               {
                  ClientResponse.class.cast(response).releaseConnection();
               }
               catch (IOException e)
               {
                  // Ignore
               }
            }
         }
         if (status >= 300 && status < 400)
         {
            throw WebApplicationExceptionWrapper.wrap(new RedirectionException(response));
         }

         return handleErrorStatus(response);
      }
      finally
      {
         // close if no content
         if (response.getMediaType() == null)
            response.close();
      }

   }

   /**
    * Throw an exception.  Expecting a status of 400 or greater.
    *
    * @param response response entity
    * @param <T> type
    * @return unreachable
    */
   public static <T> T handleErrorStatus(Response response)
   {
      final int status = response.getStatus();
      switch (status)
      {
         case 400 :
            throw WebApplicationExceptionWrapper.wrap(new BadRequestException(response));
         case 401 :
            throw WebApplicationExceptionWrapper.wrap(new NotAuthorizedException(response));
         case 403 :
            throw WebApplicationExceptionWrapper.wrap(new ForbiddenException(response));
         case 404 :
            throw WebApplicationExceptionWrapper.wrap(new NotFoundException(response));
         case 405 :
            throw WebApplicationExceptionWrapper.wrap(new NotAllowedException(response));
         case 406 :
            throw WebApplicationExceptionWrapper.wrap(new NotAcceptableException(response));
         case 415 :
            throw WebApplicationExceptionWrapper.wrap(new NotSupportedException(response));
         case 500 :
            throw WebApplicationExceptionWrapper.wrap(new InternalServerErrorException(response));
         case 503 :
            throw WebApplicationExceptionWrapper.wrap(new ServiceUnavailableException(response));
         default :
            break;
      }

      if (status >= 400 && status < 500)
         throw WebApplicationExceptionWrapper.wrap(new ClientErrorException(response));
      if (status >= 500)
         throw WebApplicationExceptionWrapper.wrap(new ServerErrorException(response));

      throw WebApplicationExceptionWrapper.wrap(new WebApplicationException(response));
   }

   public ClientConfiguration getClientConfiguration()
   {
      return configuration;
   }

   public ResteasyClient getClient()
   {
      return client;
   }

   public DelegatingOutputStream getDelegatingOutputStream()
   {
      return delegatingOutputStream;
   }

   public void setDelegatingOutputStream(DelegatingOutputStream delegatingOutputStream)
   {
      this.delegatingOutputStream = delegatingOutputStream;
   }

   public OutputStream getEntityStream()
   {
      return entityStream;
   }

   public void setEntityStream(OutputStream entityStream)
   {
      this.entityStream = entityStream;
   }

   public URI getUri()
   {
      return uri;
   }

   public void setUri(URI uri)
   {
      this.uri = uri;
   }

   public Annotation[] getEntityAnnotations()
   {
      return entityAnnotations;
   }

   public void setEntityAnnotations(Annotation[] entityAnnotations)
   {
      this.entityAnnotations = entityAnnotations;
   }

   public String getMethod()
   {
      return method;
   }

   public void setMethod(String method)
   {
      this.method = method;
   }

   public void setHeaders(ClientRequestHeaders headers)
   {
      this.headers = headers;
   }

   public Map<String, Object> getMutableProperties()
   {
      return configuration.getMutableProperties();
   }

   public Object getEntity()
   {
      return entity;
   }

   public Type getEntityGenericType()
   {
      return entityGenericType;
   }

   public Class<?> getEntityClass()
   {
      return entityClass;
   }

   public ClientRequestHeaders getHeaders()
   {
      return headers;
   }

   public void setEntity(Entity<?> entity)
   {
      if (entity == null)
      {
         this.entity = null;
         this.entityAnnotations = null;
         this.entityClass = null;
         this.entityGenericType = null;
      }
      else
      {
         Object ent = entity.getEntity();
         setEntityObject(ent);
         this.entityAnnotations = entity.getAnnotations();
         Variant v = entity.getVariant();
         headers.setMediaType(v.getMediaType());
         headers.setLanguage(v.getLanguage());
         headers.header("Content-Encoding", null);
         headers.header("Content-Encoding", v.getEncoding());
      }

   }

   public void setEntityObject(Object ent)
   {
      if (ent instanceof GenericEntity)
      {
         GenericEntity<?> genericEntity = (GenericEntity<?>) ent;
         entityClass = genericEntity.getRawType();
         entityGenericType = genericEntity.getType();
         this.entity = genericEntity.getEntity();
      }
      else
      {
         if (ent == null)
         {
            this.entity = null;
            this.entityClass = null;
            this.entityGenericType = null;
         }
         else
         {
            this.entity = ent;
            this.entityClass = ent.getClass();
            this.entityGenericType = ent.getClass();
         }
      }
   }

   public void writeRequestBody(OutputStream outputStream) throws IOException
   {
      if (entity == null)
      {
         return;
      }

      WriterInterceptor[] interceptors = getWriterInterceptors();
      AbstractWriterInterceptorContext ctx = new ClientWriterInterceptorContext(interceptors,
            configuration.getProviderFactory(), entity, entityClass, entityGenericType, entityAnnotations,
            headers.getMediaType(), headers.getHeaders(), outputStream, getMutableProperties(), tracingLogger);

      final long timestamp = tracingLogger.timestamp("WI_SUMMARY");
      try {
         ctx.proceed();
      } finally {
         tracingLogger.logDuration("WI_SUMMARY", timestamp,
                 ctx.getProcessedInterceptorCount());
      }
   }

   public WriterInterceptor[] getWriterInterceptors()
   {
      return configuration.getWriterInterceptors(null, null);
   }

   public ClientRequestFilter[] getRequestFilters()
   {
      return configuration.getRequestFilters(null, null);
   }

   public ClientResponseFilter[] getResponseFilters()
   {
      return configuration.getResponseFilters(null, null);
   }

   // Invocation methods

   public Configuration getConfiguration()
   {
      return configuration;
   }

   public boolean isChunked()
   {
      return chunked;
   }

   public void setChunked(boolean chunked)
   {
      this.chunked = chunked;
   }

   @Override
   public ClientResponse invoke()
   {
      try(CloseableContext ctx = pushProvidersContext())
      {
         ClientRequestContextImpl requestContext = new ClientRequestContextImpl(this);
         ClientResponse aborted = filterRequest(requestContext);

         // spec requires that aborted response go through filter/interceptor chains.
         ClientResponse response = (aborted != null) ? aborted : (ClientResponse)client.httpEngine().invoke(this);
         return filterResponse(requestContext, response);
      }
      catch (ResponseProcessingException e)
      {
         if (e.getResponse() != null)
         {
            e.getResponse().close();
         }
         throw e;
      }
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T invoke(Class<T> responseType)
   {
      Response response = invoke();
      if (Response.class.equals(responseType))
         return (T) response;
      return extractResult(new GenericType<T>(responseType), response, null);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T invoke(GenericType<T> responseType)
   {
      Response response = invoke();
      if (responseType.getRawType().equals(Response.class))
         return (T) response;
      return extractResult(responseType, response, null);
   }

   @Override
   public Future<Response> submit()
   {
      return doSubmit(false, null, result -> result);
   }

   @Override
   public <T> Future<T> submit(final Class<T> responseType)
   {
      return doSubmit(false, null, getResponseTypeExtractor(responseType));
   }

   @Override
   public <T> Future<T> submit(final GenericType<T> responseType)
   {
      return doSubmit(false, null, getGenericTypeExtractor(responseType));
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   @Override
   public <T> Future<T> submit(final InvocationCallback<T> callback)
   {
      GenericType<T> genericType = (GenericType<T>) new GenericType<Object>()
      {
      };
      Type[] typeInfo = Types.getActualTypeArgumentsOfAnInterface(callback.getClass(), InvocationCallback.class);
      if (typeInfo != null)
      {
         genericType = new GenericType(typeInfo[0]);
      }

      return doSubmit(true, callback, getGenericTypeExtractor(genericType));
   }

   private <T> Future<T> doSubmit(boolean buffered, InvocationCallback<T> callback, ResultExtractor<T> extractor) {
      if (client.httpEngine() instanceof AsyncClientHttpEngine)
      {
         return asyncSubmit(getFutureExtractorFunction(buffered, callback), extractor,
               getAsyncAbortedFunction(callback), getAsyncExceptionFunction(callback));
      }
      else
      {
         return executorSubmit(asyncInvocationExecutor(), callback, extractor);
      }
   }

   public ExecutorService asyncInvocationExecutor() {
       return client.asyncInvocationExecutor();
   }

   private <T> Function<ResultExtractor<T>, CompletableFuture<T>> getCompletableFutureExtractorFunction(boolean buffered) {
      final ClientHttpEngine httpEngine = client.httpEngine();
      return (httpEngine instanceof AsyncClientHttpEngine)
            ? ext -> ((AsyncClientHttpEngine) httpEngine).submit(this, buffered, ext, asyncInvocationExecutor()) : null;
   }

   private <T> Function<ResultExtractor<T>, Future<T>> getFutureExtractorFunction(boolean buffered, InvocationCallback<T> callback) {
      final ClientHttpEngine httpEngine = client.httpEngine();
      return (httpEngine instanceof AsyncClientHttpEngine)
            ? ext -> ((AsyncClientHttpEngine) httpEngine).submit(this, buffered, callback, ext) : null;
   }

   private <T> Function<ResultExtractor<T>, Publisher<T>> getPublisherExtractorFunction(boolean buffered) {
      final ClientHttpEngine httpEngine = client.httpEngine();
      return (httpEngine instanceof ReactiveClientHttpEngine)
          ? ext -> ((ReactiveClientHttpEngine) httpEngine).submitRx(this, buffered, ext) : null;
   }

   private static <T> Function<T, Future<T>> getAsyncAbortedFunction(InvocationCallback<T> callback) {
      return result -> {
         callCompletedNoThrow(callback, result);
         return CompletableFuture.completedFuture(result);
      };
   }

   private static <T> Function<Exception, Future<T>> getAsyncExceptionFunction(InvocationCallback<T> callback) {
      return ex -> {
         callFailedNoThrow(callback, ex);
         CompletableFuture<T> completableFuture = new CompletableFuture<>();
         completableFuture.completeExceptionally(new ExecutionException(ex));
         return completableFuture;
      };
   }

   @SuppressWarnings("unchecked")
   protected static <T> ResultExtractor<T> getGenericTypeExtractor(GenericType<T> responseType) {
      return response -> {
         if (responseType.getRawType().equals(Response.class))
            return (T) response;
         return ClientInvocation.extractResult(responseType, response, null);
      };
   }

   @SuppressWarnings("unchecked")
   protected static <T> ResultExtractor<T> getResponseTypeExtractor(Class<T> responseType) {
      return response -> {
         if (Response.class.equals(responseType))
            return (T) response;
         return ClientInvocation.extractResult(new GenericType<T>(responseType), response, null);
      };
   }

   public CompletableFuture<Response> submitCF()
   {
      return doSubmit(response -> response, false);
   }

   public <T> CompletableFuture<T> submitCF(final Class<T> responseType)
   {
      return doSubmit(getResponseTypeExtractor(responseType), true);
   }

   public <T> CompletableFuture<T> submitCF(final GenericType<T> responseType)
   {
      return doSubmit(getGenericTypeExtractor(responseType), true);
   }

   private <T> CompletableFuture<T> doSubmit(ResultExtractor<T> extractor, boolean buffered) {
      if (client.httpEngine() instanceof AsyncClientHttpEngine)
      {
         return asyncSubmit(getCompletableFutureExtractorFunction(buffered),
               extractor,
               CompletableFuture::completedFuture,
               ex -> {
                  CompletableFuture<T> completableFuture = new CompletableFuture<>();
                  completableFuture.completeExceptionally(new ExecutionException(ex));
                  return completableFuture;
               });
      }
      else
      {
         return executorSubmit(asyncInvocationExecutor(), null, extractor);
      }
   }

   class ReactiveInvocation {
      private final ReactiveClientHttpEngine reactiveEngine;

      ReactiveInvocation(final ReactiveClientHttpEngine reactiveEngine) {
         this.reactiveEngine = reactiveEngine;
      }

      public Publisher<Response> submit()
      {
         return doSubmitRx(response -> response, false);
      }

      public <T> Publisher<T> submit(final Class<T> responseType)
      {
         return doSubmitRx(getResponseTypeExtractor(responseType), true);
      }

      public <T> Publisher<T> submit(final GenericType<T> responseType)
      {
         return doSubmitRx(getGenericTypeExtractor(responseType), true);
      }

      private <T> Publisher<T> doSubmitRx(ResultExtractor<T> extractor, boolean buffered) {
         return rxSubmit(
             reactiveEngine,
             getPublisherExtractorFunction(buffered),
             extractor
         );
      }

      private <T> Publisher<T> rxSubmit(
          final ReactiveClientHttpEngine reactiveEngine,
          final Function<ResultExtractor<T>, Publisher<T>> asyncHttpEngineSubmitFn,
          final ResultExtractor<T> extractor
      ) {
         final ClientRequestContextImpl requestContext = new ClientRequestContextImpl(ClientInvocation.this);
         try(CloseableContext ctx = pushProvidersContext())
         {
            ClientResponse aborted = filterRequest(requestContext);
            if (aborted != null)
            {
               // spec requires that aborted response go through filter/interceptor chains.
               aborted = filterResponse(requestContext, aborted);
               T result = extractor.extractResult(aborted);
               return reactiveEngine.just(result);
            }
         }
         catch (Exception ex)
         {
            return reactiveEngine.error(ex);
         }

         return asyncHttpEngineSubmitFn.apply(response -> {
            try(CloseableContext ctx = pushProvidersContext())
            {
               return extractor.extractResult(filterResponse(requestContext, response));
            }
         });
      }
   }

   /**
    * If the client's HTTP engine implements {@link ReactiveClientHttpEngine} then you can access
    * the latter's {@link Publisher} via this method.
    */
   public Optional<ReactiveInvocation> reactive() {
      if (client.httpEngine() instanceof ReactiveClientHttpEngine) {
         return Optional.of(new ReactiveInvocation((ReactiveClientHttpEngine)client.httpEngine()));
      }
      return Optional.empty();
   }

   @Override
   public Invocation property(String name, @Sensitive Object value)
   {
      configuration.property(name, value);
      return this;
   }

   public ClientInvoker getClientInvoker() {
      return clientInvoker;
   }

   public void setClientInvoker(ClientInvoker clientInvoker) {
      this.clientInvoker = clientInvoker;
   }
   // internals

   private CloseableContext pushProvidersContext()
   {
      CloseableContext ret = ResteasyContext.addCloseableContextDataLevel();
      ResteasyContext.pushContext(Providers.class, configuration);
      return ret;
   }

   protected ClientResponse filterRequest(ClientRequestContextImpl requestContext)
   {
      ClientRequestFilter[] requestFilters = getRequestFilters();
      ClientResponse aborted = null;
      if (requestFilters != null && requestFilters.length > 0)
      {
         for (ClientRequestFilter filter : requestFilters)
         {
            try
            {
               filter.filter(requestContext);
               if (requestContext.getAbortedWithResponse() != null)
               {
                  aborted = new AbortedResponse(configuration, requestContext.getAbortedWithResponse());
                  break;
               }
            }
            catch (ProcessingException e)
            {
               throw e;
            }
            catch (Throwable e)
            {
               throw new ProcessingException(e);
            }
         }
      }
      return aborted;
   }

   protected ClientResponse filterResponse(ClientRequestContextImpl requestContext, ClientResponse response)
   {
      response.setProperties(configuration.getMutableProperties());

      ClientResponseFilter[] responseFilters = getResponseFilters();
      if (responseFilters != null && responseFilters.length > 0)
      {
         ClientResponseContextImpl responseContext = new ClientResponseContextImpl(response);
         for (ClientResponseFilter filter : responseFilters)
         {
            try
            {
               filter.filter(requestContext, responseContext);
            }
            catch (ResponseProcessingException e)
            {
               throw e;
            }
            catch (Throwable e)
            {
               throw new ResponseProcessingException(response, e);
            }
         }
      }
      return response;
   }

   private <Q extends Future<T>, T> Q asyncSubmit(
           final Function<ResultExtractor<T>, Q> asyncHttpEngineSubmitFn,
           final ResultExtractor<T> extractor,
           final Function<T, Q> abortedFn,
           final Function<Exception, Q> exceptionFn)
   {
      final ClientRequestContextImpl requestContext = new ClientRequestContextImpl(this);
      try(CloseableContext ctx = pushProvidersContext())
      {
         ClientResponse aborted = filterRequest(requestContext);
         if (aborted != null)
         {
            // spec requires that aborted response go through filter/interceptor chains.
            aborted = filterResponse(requestContext, aborted);
            T result = extractor.extractResult(aborted);
            return abortedFn.apply(result);
         }
      }
      catch (Exception ex)
      {
         return exceptionFn.apply(ex);
      }

      return asyncHttpEngineSubmitFn.apply(response -> {
         try(CloseableContext ctx = pushProvidersContext())
         {
            return extractor.extractResult(filterResponse(requestContext, response));
         }
      });
   }



   private <T> CompletableFuture<T> executorSubmit(ExecutorService executor, final InvocationCallback<T> callback,
         final ResultExtractor<T> extractor)
   {
      return CompletableFuture.supplyAsync(() -> {
          // FIXME: why does this have no context?
         // ensure the future and the callback see the same result
         ClientResponse response = null;
         T result = null; // Liberty change - expanding scope for result variable
         try
         {
            response = invoke(); // does filtering too
            result = extractor.extractResult(response);
            callCompletedNoThrow(callback, result);
            return result;
         }
         catch (Exception e)
         {
            callFailedNoThrow(callback, e);
            throw e;
         }
         finally
         {
            if (response != null && callback != null) {
                if (result == null || !Response.class.isAssignableFrom(result.getClass()))// Liberty change - don't close when result is a Response type
                    response.close();
            }
         }
      }, executor);
   }

   private static <T> void callCompletedNoThrow(InvocationCallback<T> callback, T result)
   {
      if (callback != null)
      {
         try
         {
            callback.completed(result);
         }
         catch (Exception e)
         {
            //logger.error("ignoring exception in InvocationCallback", e);
         }
      }
   }

   private static <T> void callFailedNoThrow(InvocationCallback<T> callback, Exception exception)
   {
      if (callback != null)
      {
         try
         {
            callback.failed(exception);
         }
         catch (Exception e)
         {
            //logger.error("ignoring exception in InvocationCallback", e);
         }
      }
   }

   public RESTEasyTracingLogger getTracingLogger() {
      return tracingLogger;
   }

   public void setActualTarget(WebTarget target)
   {
      this.actualTarget = target;
   }

   public WebTarget getActualTarget()
   {
      return this.actualTarget;
   }
}
