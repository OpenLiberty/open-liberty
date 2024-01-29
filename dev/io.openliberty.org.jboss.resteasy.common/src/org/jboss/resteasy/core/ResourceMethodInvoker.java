package org.jboss.resteasy.core;

import org.jboss.resteasy.annotations.Stream;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.core.registry.SegmentNode;
import org.jboss.resteasy.plugins.server.resourcefactory.JndiComponentResourceFactory;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.BuiltResponseEntityNotBacked;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnhandledException;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistryListener;
import org.jboss.resteasy.spi.metadata.MethodParameter;
import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.ResourceMethod;
import org.jboss.resteasy.spi.statistics.MethodStatisticsLogger;
import org.jboss.resteasy.spi.validation.GeneralValidator;
import org.jboss.resteasy.spi.validation.GeneralValidatorCDI;
import org.jboss.resteasy.statistics.StatisticsControllerImpl;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.DynamicFeatureContextDelegate;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.sse.SseEventSink;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceMethodInvoker implements ResourceInvoker, JaxrsInterceptorRegistryListener
{
   protected MethodInjector methodInjector;
   protected InjectorFactory injector;
   protected ResourceFactory resource;
   protected ResteasyProviderFactory parentProviderFactory;
   protected ResteasyProviderFactory resourceMethodProviderFactory;
   protected ResourceMethod method;
   protected Annotation[] methodAnnotations;
   protected ContainerRequestFilter[] requestFilters;
   protected ContainerResponseFilter[] responseFilters;
   protected WriterInterceptor[] writerInterceptors;
   protected ConcurrentHashMap<String, AtomicLong> stats = new ConcurrentHashMap<String, AtomicLong>();
   protected GeneralValidator validator;
   protected boolean isValidatable;
   protected boolean methodIsValidatable;
   @SuppressWarnings("rawtypes")
   protected AsyncResponseProvider asyncResponseProvider;
   @SuppressWarnings("rawtypes")
   AsyncStreamProvider asyncStreamProvider;
   protected boolean isSse;
   protected boolean isAsyncStreamProvider;
   protected ResourceInfo resourceInfo;

   protected boolean expectsBody;
   protected final boolean hasProduces;
   protected MethodStatisticsLogger methodStatisticsLogger;



   public ResourceMethodInvoker(final ResourceMethod method, final InjectorFactory injector, final ResourceFactory resource, final ResteasyProviderFactory providerFactory)
   {
      this.injector = injector;
      this.resource = resource;
      this.parentProviderFactory = providerFactory;
      this.method = method;
      this.methodAnnotations = this.method.getAnnotatedMethod().getAnnotations();
      methodStatisticsLogger = StatisticsControllerImpl.EMPTY;

      resourceInfo = new ResourceInfo()
      {
         @Override
         public Method getResourceMethod()
         {
            return ResourceMethodInvoker.this.method.getMethod();
         }

         @Override
         public Class<?> getResourceClass()
         {
            return ResourceMethodInvoker.this.method.getResourceClass().getClazz();
         }
      };

      Set<DynamicFeature> serverDynamicFeatures = providerFactory.getServerDynamicFeatures();
      if (serverDynamicFeatures != null && !serverDynamicFeatures.isEmpty()) {
         this.resourceMethodProviderFactory = new ResteasyProviderFactoryImpl(RuntimeType.SERVER, providerFactory);
         for (DynamicFeature feature : serverDynamicFeatures)
         {
            feature.configure(resourceInfo, new DynamicFeatureContextDelegate(resourceMethodProviderFactory));
         }
         ((ResteasyProviderFactoryImpl)this.resourceMethodProviderFactory).lockSnapshots();
      } else {
         // if no dynamic features, we don't need to copy the parent.
         this.resourceMethodProviderFactory = providerFactory;
      }

      this.methodInjector = injector.createMethodInjector(method, resourceMethodProviderFactory);

      // hack for when message contentType == null
      // and @Consumes is on the class
      expectsBody = this.methodInjector.expectsBody();

      requestFilters = resourceMethodProviderFactory.getContainerRequestFilterRegistry().postMatch(method.getResourceClass().getClazz(), method.getAnnotatedMethod());
      responseFilters = resourceMethodProviderFactory.getContainerResponseFilterRegistry().postMatch(method.getResourceClass().getClazz(), method.getAnnotatedMethod());
      writerInterceptors = resourceMethodProviderFactory.getServerWriterInterceptorRegistry().postMatch(method.getResourceClass().getClazz(), method.getAnnotatedMethod());

      // register with parent to listen for redeploy events
      providerFactory.getContainerRequestFilterRegistry().getListeners().add(this);
      providerFactory.getContainerResponseFilterRegistry().getListeners().add(this);
      providerFactory.getServerWriterInterceptorRegistry().getListeners().add(this);
      ContextResolver<GeneralValidator> resolver = providerFactory.getContextResolver(GeneralValidator.class, MediaType.WILDCARD_TYPE);
      if (resolver != null)
      {
         validator = providerFactory.getContextResolver(GeneralValidator.class, MediaType.WILDCARD_TYPE).getContext(null);
      }
      if (validator != null)
      {
         Class<?> clazz = null;
         if (resource != null && resource.getScannableClass() != null)
         {
            clazz = resource.getScannableClass();
         }
         else
         {
            clazz = getMethod().getDeclaringClass();
         }
         if (resource instanceof JndiComponentResourceFactory)
         {
            isValidatable = true;
         }
         else
         {
            if (validator instanceof GeneralValidatorCDI)
            {
               isValidatable = GeneralValidatorCDI.class.cast(validator).isValidatable(clazz, injector);
            }
            else
            {
               isValidatable = validator.isValidatable(clazz);
            }
         }
         methodIsValidatable = validator.isMethodValidatable(getMethod());
      }

      asyncResponseProvider = resourceMethodProviderFactory.getAsyncResponseProvider(method.getReturnType());
      if(asyncResponseProvider == null){
         asyncStreamProvider = resourceMethodProviderFactory.getAsyncStreamProvider(method.getReturnType());
      }
      if (asyncStreamProvider != null)
      {
         for (Annotation annotation : method.getAnnotatedMethod().getAnnotations())
         {
            if (annotation.annotationType() == Stream.class)
            {
               Stream stream = (Stream)annotation;
               if (stream.value() == Stream.MODE.GENERAL)
               {
                  this.isAsyncStreamProvider = true;
               }
            }
         }
      }

      if (isSseResourceMethod(method))
      {
         isSse = true;
         method.markAsynchronous();
      }
      hasProduces = method.getMethod().isAnnotationPresent(Produces.class) || method.getMethod().getClass().isAnnotationPresent(Produces.class);
   }

   @Override
   public boolean hasProduces() {
      return hasProduces;
   }

   // spec section 9.3 Server API:
   // A resource method that injects an SseEventSink and
   // produces the media type text/event-stream is an SSE resource method.
   private boolean isSseResourceMethod(ResourceMethod resourceMethod) {

      // First exclusive condition to be a SSE resource method is to only
      // produce text/event-stream
      MediaType[] producedMediaTypes = resourceMethod.getProduces();
      boolean onlyProduceServerSentEventsMediaType = producedMediaTypes != null && producedMediaTypes.length == 1
            && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(producedMediaTypes[0]);
      if (!onlyProduceServerSentEventsMediaType)
      {
         return false;
      }
      // Second condition to be a SSE resource method is to be injected with a
      // SseEventSink parameter
      MethodParameter[] resourceMethodParameters = resourceMethod.getParams();
      if (resourceMethodParameters != null)
      {
         for (MethodParameter resourceMethodParameter : resourceMethodParameters)
         {
            if (Parameter.ParamType.CONTEXT.equals(resourceMethodParameter.getParamType())
                  && SseEventSink.class.equals(resourceMethodParameter.getType()))
            {
               return true;
            }
         }
      }

      // Resteasy specific:
      // Or the given application should register a
      // org.jboss.resteasy.spi.AsyncStreamProvider compatible with resource
      // method return type and the resource method must not be annotated with
      // any org.jboss.resteasy.annotations.Stream annotation
      if (asyncStreamProvider != null)
      {
         for (Annotation annotation : resourceMethod.getAnnotatedMethod().getAnnotations())
         {
            if (annotation.annotationType() == Stream.class)
            {
               return false;
            }
         }
         return true;
      }

      return false;
   }

   public void cleanup()
   {
      parentProviderFactory.getContainerRequestFilterRegistry().getListeners().remove(this);
      parentProviderFactory.getContainerResponseFilterRegistry().getListeners().remove(this);
      parentProviderFactory.getServerWriterInterceptorRegistry().getListeners().remove(this);
      for (ValueInjector param : methodInjector.getParams())
      {
         if (param instanceof MessageBodyParameterInjector)
         {
            parentProviderFactory.getServerReaderInterceptorRegistry().getListeners().remove(param);
         }
      }
   }

   @Override
   public void registryUpdated(JaxrsInterceptorRegistry registry, JaxrsInterceptorRegistry.InterceptorFactory factory)
   {
      if (registry.getIntf().equals(WriterInterceptor.class))
      {
         JaxrsInterceptorRegistry<WriterInterceptor> serverWriterInterceptorRegistry = this.resourceMethodProviderFactory
               .getServerWriterInterceptorRegistry();
         //Check to prevent StackOverflowError
         if (registry != serverWriterInterceptorRegistry)
         {
            serverWriterInterceptorRegistry.register(factory);
         }
         this.writerInterceptors = serverWriterInterceptorRegistry.postMatch(this.method.getResourceClass().getClazz(),
               this.method.getAnnotatedMethod());
      }
      else if (registry.getIntf().equals(ContainerRequestFilter.class))
      {
         JaxrsInterceptorRegistry<ContainerRequestFilter> containerRequestFilterRegistry = this.resourceMethodProviderFactory
               .getContainerRequestFilterRegistry();
         //Check to prevent StackOverflowError
         if (registry != containerRequestFilterRegistry)
         {
            containerRequestFilterRegistry.register(factory);
         }
         this.requestFilters = containerRequestFilterRegistry.postMatch(this.method.getResourceClass().getClazz(),
               this.method.getAnnotatedMethod());
      }
      else if (registry.getIntf().equals(ContainerResponseFilter.class))
      {
         JaxrsInterceptorRegistry<ContainerResponseFilter> containerResponseFilterRegistry = this.resourceMethodProviderFactory
               .getContainerResponseFilterRegistry();
         //Check to prevent StackOverflowError
         if (registry != containerResponseFilterRegistry)
         {
            containerResponseFilterRegistry.register(factory);
         }
         this.responseFilters = containerResponseFilterRegistry.postMatch(this.method.getResourceClass().getClazz(),
               this.method.getAnnotatedMethod());
      }
   }

   protected void incrementMethodCount(String httpMethod)
   {
      AtomicLong stat = stats.get(httpMethod);
      if (stat == null)
      {
         stat = new AtomicLong();
         AtomicLong old = stats.putIfAbsent(httpMethod, stat);
         if (old != null) stat = old;
      }
      stat.incrementAndGet();
   }

   /**
    * Key is httpMethod called.
    *
    * @return statistics map
    */
   public Map<String, AtomicLong> getStats()
   {
      return stats;
   }



   public ContainerRequestFilter[] getRequestFilters()
   {
      return requestFilters;
   }

   public ContainerResponseFilter[] getResponseFilters()
   {
      return responseFilters;
   }

   public WriterInterceptor[] getWriterInterceptors()
   {
      return writerInterceptors;
   }

   public Type getGenericReturnType()
   {
      return method.getGenericReturnType();
   }

   public Class<?> getResourceClass()
   {
      return method.getResourceClass().getClazz();
   }

   public Class<?> getReturnType()
   {
      return method.getReturnType();
   }

   public Annotation[] getMethodAnnotations()
   {
      return methodAnnotations;
   }



   @Override
   public Method getMethod()
   {
      return method.getMethod();
   }

   public CompletionStage<Object> invokeDryRun(HttpRequest request, HttpResponse response) {
      Object resource = this.resource.createResource(request, response, resourceMethodProviderFactory);
      if (resource instanceof CompletionStage) {
         @SuppressWarnings("unchecked")
         CompletionStage<Object> stage = (CompletionStage<Object>)resource;
         return stage
                 .thenCompose(target -> invokeDryRun(request, response, target));
      }
      return invokeDryRun(request, response, resource);
   }


   public BuiltResponse invoke(HttpRequest request, HttpResponse response)
   {
      Object resource = this.resource.createResource(request, response, resourceMethodProviderFactory);
      if (resource instanceof CompletionStage) {
         @SuppressWarnings("unchecked")
         CompletionStage<Object> stage = (CompletionStage<Object>)resource;
         return stage
                 .thenApply(target -> invoke(request, response, target)).toCompletableFuture().getNow(null);
      }
      return invoke(request, response, resource);
   }

   public CompletionStage<Object> invokeDryRun(HttpRequest request, HttpResponse response, Object target)
   {
      request.setAttribute(ResourceMethodInvoker.class.getName(), this);
      incrementMethodCount(request.getHttpMethod());
      ResteasyUriInfo uriInfo = (ResteasyUriInfo) request.getUri();
      if (method.getPath() != null)
      {
         uriInfo.pushMatchedURI(uriInfo.getMatchingPath());
      }
      uriInfo.pushCurrentResource(target);
      return invokeOnTargetDryRun(request, response, target);
   }

   public BuiltResponse invoke(HttpRequest request, HttpResponse response, Object target)
   {
      request.setAttribute(ResourceMethodInvoker.class.getName(), this);
      incrementMethodCount(request.getHttpMethod());
      ResteasyUriInfo uriInfo = (ResteasyUriInfo) request.getUri();
      if (method.getPath() != null)
      {
         uriInfo.pushMatchedURI(uriInfo.getMatchingPath());
      }
      uriInfo.pushCurrentResource(target);
      return invokeOnTarget(request, response, target);
   }

   @SuppressWarnings("unchecked")
   protected CompletionStage<Object> invokeOnTargetDryRun(HttpRequest request, HttpResponse response, Object target)
   {
      ResteasyContext.pushContext(ResourceInfo.class, resourceInfo);  // we don't pop so writer interceptors can get at this
      ResteasyContext.pushContext(Configuration.class, resourceMethodProviderFactory);

      try
      {
         Object rtn = internalInvokeOnTarget(request, response, target);
         if (rtn != null && rtn instanceof CompletionStage) {
            return (CompletionStage<Object>)rtn;
         } else {
            return CompletableFuture.completedFuture(rtn);
         }
      }
      catch (Failure failure) {
         throw failure;
      }
      catch (ApplicationException appException) {
         throw appException;
      }
      catch (RuntimeException ex)
      {
         throw new ProcessingException(ex);

      }
   }

   protected BuiltResponse invokeOnTarget(HttpRequest request, HttpResponse response, Object target) {
      final RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
      final long timestamp = tracingLogger.timestamp("METHOD_INVOKE");
      final long msTimeStamp = methodStatisticsLogger.timestamp();
      try {
         ResteasyContext.pushContext(ResourceInfo.class, resourceInfo);  // we don't pop so writer interceptors can get at this
         ResteasyContext.pushContext(Configuration.class, resourceMethodProviderFactory);
         if (requestFilters != null && requestFilters.length > 0) {
            PostMatchContainerRequestContext requestContext = new PostMatchContainerRequestContext(request, this, requestFilters,
                    () -> invokeOnTargetAfterFilter(request, response, target));
            // let it handle the continuation
            return requestContext.filter();
         } else {
            return invokeOnTargetAfterFilter(request, response, target);
         }
      } finally {
         methodStatisticsLogger.duration(msTimeStamp);
         if (resource instanceof SingletonResource) {
            tracingLogger.logDuration("METHOD_INVOKE", timestamp, ((SingletonResource) resource).traceInfo(), method.getMethod());
         } else {
            tracingLogger.logDuration("METHOD_INVOKE", timestamp, resource, method.getMethod());
         }
      }
   }

   protected BuiltResponse invokeOnTargetAfterFilter(HttpRequest request, HttpResponse response, Object target)
   {
      if (validator != null)
      {
         if (isValidatable)
         {
            validator.validate(request, target);
         }
         if (methodIsValidatable)
         {
            request.setAttribute(GeneralValidator.class.getName(), validator);
         }
         else if (isValidatable)
         {
            validator.checkViolations(request);
         }
      }

      final AsyncResponseConsumer asyncResponseConsumer;
      if (asyncResponseProvider != null)
      {
         asyncResponseConsumer = AsyncResponseConsumer.makeAsyncResponseConsumer(this, asyncResponseProvider);
      }
      else if (asyncStreamProvider != null)
      {
         asyncResponseConsumer = AsyncResponseConsumer.makeAsyncResponseConsumer(this, asyncStreamProvider);
      }
      else
      {
         asyncResponseConsumer = null;
      }

      try
      {
         Object ret = internalInvokeOnTarget(request, response, target);
         if (ret != null && ret instanceof CompletionStage) {
            @SuppressWarnings("unchecked")
            CompletionStage<Object> retStage = (CompletionStage<Object>)ret;
            CompletionStage<BuiltResponse> stage = retStage
                    .thenApply(rtn -> afterInvoke(request, asyncResponseConsumer, rtn));
            // if async isn't finished, return null.  Container will assume that its a suspended request
            return stage.toCompletableFuture().getNow(null);
         } else {
            return afterInvoke(request, asyncResponseConsumer, CompletionStageHolder.resolve(ret));
         }
      }
      catch (CompletionException ex)
      {
 
         if(ex.getCause() instanceof RuntimeException)
            return handleInvocationException(asyncResponseConsumer, request, (RuntimeException) ex.getCause());
         SynchronousDispatcher.rethrow(ex.getCause());
         // never reached
         return null;
      }
      catch (RuntimeException ex)
      {
         return handleInvocationException(asyncResponseConsumer, request, ex);
      }
   }

   private BuiltResponse afterInvoke(HttpRequest request, AsyncResponseConsumer asyncResponseConsumer, Object rtn)
   {
      if(asyncResponseConsumer != null)
      {
         asyncResponseConsumer.subscribe(rtn);
         return null;
      }
      if (request.getAsyncContext().isSuspended())
      {
         if(method.isAsynchronous())
            return null;
         // resume a sync request that got turned async by filters
         initializeAsync(request.getAsyncContext().getAsyncResponse());
         request.getAsyncContext().getAsyncResponse().resume(rtn);
         return null;
      }
      if (request.wasForwarded())
      {
         return null;
      }
      if (!contextOutputStreamWrittenTo() && (rtn == null || method.getReturnType().equals(void.class)))
      {
         BuiltResponse build = (BuiltResponse) Response.noContent().build();
         build.addMethodAnnotations(getMethodAnnotations());
         return build;
      }
      if (Response.class.isAssignableFrom(method.getReturnType()) || rtn instanceof Response)
      {
         if (!(rtn instanceof BuiltResponse))
         {
            Response r = (Response)rtn;
            Headers<Object> metadata = new Headers<Object>();
            metadata.putAll(r.getMetadata());
            rtn = new BuiltResponseEntityNotBacked(r.getStatus(), r.getStatusInfo().getReasonPhrase(),
                    metadata, r.getEntity(), null);
         }
         BuiltResponse rtn1 = (BuiltResponse) rtn;
         rtn1.addMethodAnnotations(getMethodAnnotations());
         if (rtn1.getGenericType() == null)
         {
            if (getMethod().getReturnType().equals(Response.class))
            {
               rtn1.setGenericType(rtn1.getEntityClass());
            }
            else
            {
               rtn1.setGenericType(method.getGenericReturnType());
            }
         }
         return rtn1;
      }

      Response.ResponseBuilder builder = Response.ok(rtn);
      BuiltResponse jaxrsResponse = (BuiltResponse)builder.build();
      if (jaxrsResponse.getGenericType() == null)
      {
         if (getMethod().getReturnType().equals(Response.class))
         {
            jaxrsResponse.setGenericType(jaxrsResponse.getEntityClass());
         }
         else
         {
            jaxrsResponse.setGenericType(method.getGenericReturnType());
         }
      }
      jaxrsResponse.addMethodAnnotations(getMethodAnnotations());
      return jaxrsResponse;
   }

   private BuiltResponse handleInvocationException(AsyncResponseConsumer asyncStreamResponseConsumer, HttpRequest request, RuntimeException ex)
   {
      if (asyncStreamResponseConsumer != null)
      {
         // WARNING: this can throw if the exception is not mapped by the user, in
         // which case we haven't completed the connection and called the callbacks
         try
         {
            AsyncResponseConsumer consumer = asyncStreamResponseConsumer;
            asyncStreamResponseConsumer.internalResume(ex, t -> consumer.complete(ex));
         }
         catch(UnhandledException x)
         {
            // make sure we call the callbacks before throwing to the container
            request.getAsyncContext().getAsyncResponse().completionCallbacks(ex);
            throw x;
         }
         return null;
      }
      else if (request.getAsyncContext().isSuspended())
      {
         try
         {
            request.getAsyncContext().getAsyncResponse().resume(ex);
         }
         catch (Exception e)
         {
            LogMessages.LOGGER.errorResumingFailedAsynchOperation(e);
         }
         return null;
      }
      else
      {
          throw ex;
      }
   }

   @SuppressWarnings("unchecked")
   private Object internalInvokeOnTarget(HttpRequest request, HttpResponse response, Object target) throws Failure, ApplicationException {
      PostResourceMethodInvokers postResourceMethodInvokers = ResteasyContext.getContextData(PostResourceMethodInvokers.class);
      try {
          Object methodResponse = this.methodInjector.invoke(request, response, target);
          CompletionStage<Object> stage = null;
          if (methodResponse != null && methodResponse instanceof CompletionStage) {
             stage = (CompletionStage<Object>)methodResponse;
             return stage
                     .handle((ret, exception) -> {
                        // on success
                        if (exception == null && postResourceMethodInvokers != null) {
                           postResourceMethodInvokers.getInvokers().forEach(e -> e.invoke());
                        }
                        // finally
                        if (postResourceMethodInvokers != null) {
                           postResourceMethodInvokers.clear();
                        }
                        if (exception != null) {
                           SynchronousDispatcher.rethrow(exception);
                           // never reached
                           return null;
                        }
                        return ret;
                     });
          } else {
             // on success
             if (postResourceMethodInvokers != null) {
                postResourceMethodInvokers.getInvokers().forEach(e -> e.invoke());
             }
             // finally
             if (postResourceMethodInvokers != null) {
                postResourceMethodInvokers.clear();
             }
             return methodResponse;
          }
      } catch (RuntimeException failure) {
         if (postResourceMethodInvokers != null) {
            postResourceMethodInvokers.clear();
         }
         throw failure;
      }
   }

   public void initializeAsync(ResteasyAsynchronousResponse asyncResponse)
   {
      asyncResponse.setAnnotations(method.getAnnotatedMethod().getAnnotations());
      asyncResponse.setWriterInterceptors(getWriterInterceptors());
      asyncResponse.setResponseFilters(getResponseFilters());
      if (asyncResponse instanceof ResourceMethodInvokerAwareResponse) {
         ((ResourceMethodInvokerAwareResponse)asyncResponse).setMethod(this);
      }
   }

   public boolean doesProduce(List<? extends MediaType> accepts)
   {
      if (accepts == null || accepts.size() == 0)
      {
         //System.out.println("**** no accepts " +" method: " + method);
         return true;
      }
      if (method.getProduces().length == 0)
      {
         //System.out.println("**** no produces " +" method: " + method);
         return true;
      }

      for (MediaType accept : accepts)
      {
         for (MediaType type : method.getProduces())
         {
            if (type.isCompatible(accept))
            {
               return true;
            }
         }
      }
      return false;
   }

   public boolean doesConsume(MediaType contentType)
   {
      boolean matches = false;
      if (method.getConsumes().length == 0 || (contentType == null && !expectsBody)) return true;

      if (contentType == null)
      {
         contentType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }
      for (MediaType type : method.getConsumes())
      {
         if (type.isCompatible(contentType))
         {
            matches = true;
            break;
         }
      }
      return matches;
   }

   public MediaType resolveContentType(HttpRequest in, Object entity)
   {
      MediaType chosen = (MediaType)in.getAttribute(SegmentNode.RESTEASY_CHOSEN_ACCEPT);
      if (chosen != null  && !chosen.equals(MediaType.WILDCARD_TYPE))
      {
         return chosen;
      }

      List<MediaType> accepts = in.getHttpHeaders().getAcceptableMediaTypes();

      if (accepts == null || accepts.size() == 0)
      {
         if (method.getProduces().length == 0) return MediaType.WILDCARD_TYPE;
         else return method.getProduces()[0];
      }

      if (method.getProduces().length == 0)
      {
         return resolveContentTypeByAccept(accepts, entity);
      }

      for (MediaType accept : accepts)
      {
         for (MediaType type : method.getProduces())
         {
            if (type.isCompatible(accept)) return type;
         }
      }
      return MediaType.WILDCARD_TYPE;
   }

   protected MediaType resolveContentTypeByAccept(List<MediaType> accepts, Object entity)
   {
      if (accepts == null || accepts.size() == 0 || entity == null)
      {
         return MediaType.WILDCARD_TYPE;
      }
      Class<?> clazz = entity.getClass();
      Type type = this.method.getGenericReturnType();
      if (entity instanceof GenericEntity)
      {
         GenericEntity<?> gen = (GenericEntity<?>) entity;
         clazz = gen.getRawType();
         type = gen.getType();
      }
      for (MediaType accept : accepts)
      {
         if (resourceMethodProviderFactory.getMessageBodyWriter(clazz, type, method.getAnnotatedMethod().getAnnotations(), accept) != null)
         {
            return accept;
         }
      }
      return MediaType.WILDCARD_TYPE;
   }

   /**
    * Checks if any bytes were written to a @Context HttpServletResponse
    * @see ContextParameterInjector for details
    * Fix for RESTEASY-1721
    */
   private boolean contextOutputStreamWrittenTo()
   {
      for (ValueInjector vi : methodInjector.getParams())
      {
         if (vi instanceof ContextParameterInjector)
         {
            return ((ContextParameterInjector) vi).isOutputStreamWasWritten();
         }
      }
      return false;
   }


   public Set<String> getHttpMethods()
   {
      return method.getHttpMethods();
   }

   public MediaType[] getProduces()
   {
      return method.getProduces();
   }

   public MediaType[] getConsumes()
   {
      return method.getConsumes();
   }

   public boolean isSse()
   {
      return isSse;
   }

   public boolean isAsyncStreamProvider()
   {
      return isAsyncStreamProvider;
   }

   public void markMethodAsAsync()
   {
      method.markAsynchronous();
   }

   public void setMethodStatisticsLogger(MethodStatisticsLogger msLogger) {
      methodStatisticsLogger = msLogger;
   }

   public MethodStatisticsLogger getMethodStatisticsLogger() {
      return methodStatisticsLogger;
   }
}
