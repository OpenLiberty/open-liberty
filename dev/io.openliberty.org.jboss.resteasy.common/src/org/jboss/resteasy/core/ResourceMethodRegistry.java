package org.jboss.resteasy.core;

import org.jboss.resteasy.core.registry.RootClassNode;
import org.jboss.resteasy.core.registry.RootNode;
import org.jboss.resteasy.plugins.server.resourcefactory.JndiResourceFactory;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.specimpl.ResteasyUriBuilderImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriBuilder;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.spi.metadata.ResourceMethod;
import org.jboss.resteasy.spi.statistics.StatisticsController;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.AnnotationResolver;
import org.jboss.resteasy.util.GetRestful;
import org.jboss.resteasy.util.IsHttpMethod;

import javax.ws.rs.Path;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of resources and methods/classes that can dispatch HTTP method requests.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
public class ResourceMethodRegistry implements Registry
{
   public static final String REGISTRY_MATCHING_EXCEPTION = "registry.matching.exception";

   protected ResteasyProviderFactory providerFactory;
   protected RootClassNode root = new RootClassNode();
   protected boolean widerMatching;
   protected RootNode rootNode = new RootNode();
   protected ResourceBuilder resourceBuilder;
   protected StatisticsController statisticsController;


   public ResourceMethodRegistry(final ResteasyProviderFactory providerFactory)
   {
      this.providerFactory = providerFactory;
      this.resourceBuilder = providerFactory.getResourceBuilder();
      this.statisticsController = providerFactory.getStatisticsController();
   }

   public boolean isWiderMatching()
   {
      return widerMatching;
   }

   public void setWiderMatching(boolean widerMatching)
   {
      this.widerMatching = widerMatching;
   }

   public void addPerRequestResource(Class clazz, String basePath)
   {
      addResourceFactory(new POJOResourceFactory(resourceBuilder, clazz), basePath);

   }

   /**
    * Register a vanilla JAX-RS resource class.
    *
    * @param clazz class
    */
   public void addPerRequestResource(Class clazz)
   {
      addResourceFactory(new POJOResourceFactory(resourceBuilder, clazz));
   }

   @Override
   public void addPerRequestResource(Class<?> clazz, ResourceBuilder resourceBuilder) {
      addResourceFactory(new POJOResourceFactory(resourceBuilder, clazz), resourceBuilder);
   }

   @Override
   public void addPerRequestResource(ResourceClass clazz)
   {
      POJOResourceFactory resourceFactory = new POJOResourceFactory(resourceBuilder, clazz);
      register(resourceFactory, null, clazz);
      resourceFactory.registered(providerFactory);
   }

   @Override
   public void addPerRequestResource(ResourceClass clazz, String basePath)
   {
      POJOResourceFactory resourceFactory = new POJOResourceFactory(resourceBuilder, clazz);
      register(resourceFactory, basePath, clazz);
      resourceFactory.registered(providerFactory);
   }

   public void addSingletonResource(Object singleton)
   {
      ResourceClass resourceClass = resourceBuilder.getRootResourceFromAnnotations(singleton.getClass());
      addResourceFactory(new SingletonResource(singleton, resourceClass));
   }

   public void addSingletonResource(Object singleton, String basePath)
   {
      ResourceClass resourceClass = resourceBuilder.getRootResourceFromAnnotations(singleton.getClass());
      addResourceFactory(new SingletonResource(singleton, resourceClass), basePath);
   }

   @Override
   public void addSingletonResource(Object singleton, ResourceClass resourceClass)
   {
      SingletonResource resourceFactory = new SingletonResource(singleton, resourceClass);
      register(resourceFactory, null, resourceClass);
      resourceFactory.registered(providerFactory);
   }

   @Override
   public void addSingletonResource(Object singleton, ResourceClass resourceClass, String basePath)
   {
      SingletonResource resourceFactory = new SingletonResource(singleton, resourceClass);
      register(resourceFactory, basePath, resourceClass);
      resourceFactory.registered(providerFactory);
   }


   public void addJndiResource(String jndiName)
   {
      addResourceFactory(new JndiResourceFactory(jndiName));
   }

   public void addJndiResource(String jndiName, String basePath)
   {
      addResourceFactory(new JndiResourceFactory(jndiName), basePath);
   }

   @Override
   public void addJndiResource(String jndiName, ResourceClass resourceClass)
   {
      JndiResourceFactory resourceFactory = new JndiResourceFactory(jndiName);
      register(resourceFactory, null, resourceClass);
      resourceFactory.registered(providerFactory);
   }

   @Override
   public void addJndiResource(String jndiName, ResourceClass resourceClass, String basePath)
   {
      JndiResourceFactory resourceFactory = new JndiResourceFactory(jndiName);
      register(resourceFactory, basePath, resourceClass);
      resourceFactory.registered(providerFactory);
   }


   /**
    * Bind an endpoint ResourceFactory.  ResourceFactory.getScannableClass() defines what class should be scanned
    * for JAX-RS annotations.  The class and any implemented interfaces are scanned for annotations.
    *
    * @param ref resource factory
    */
   public void addResourceFactory(ResourceFactory ref)
   {
      addResourceFactory(ref, (String)null);
   }

   public void addResourceFactory(ResourceFactory ref, ResourceBuilder resourceBuilder)
   {
      addResourceFactory(ref, resourceBuilder, null);
   }

   /**
    * ResourceFactory.getScannableClass() defines what class should be scanned
    * for JAX-RS annotations.    The class and any implemented interfaces are scanned for annotations.
    *
    * @param ref resource factory
    * @param base base URI path for any resources provided by the factory, in addition to rootPath
    */
   public void addResourceFactory(ResourceFactory ref, String base)
   {
      addResourceFactory(ref, resourceBuilder, base);
   }

   public void addResourceFactory(ResourceFactory ref, ResourceBuilder resourceBuilder, String base)
   {
//       Thread.dumpStack();
      Class<?> clazz = ref.getScannableClass();
      Class restful = AnnotationResolver.getClassWithAnnotation(clazz, resourceBuilder.getCorrespondingRootAnnotation());
      if (restful == null)
      {
         String msg = Messages.MESSAGES.classIsNotRootResource(clazz.getName());
         for (Class intf : clazz.getInterfaces())
         {
            msg += " " + intf.getName();
         }
         throw new RuntimeException(msg);
      }
      addResourceFactory(ref, resourceBuilder, base, restful);
   }

   /**
    * ResourceFactory.getScannableClass() is not used, only the clazz parameter and not any implemented interfaces
    * of the clazz parameter.
    *
    * @param ref resource factory
    * @param base  base URI path for any resources provided by the factory, in addition to rootPath
    * @param clazz specific class
    */
   public void addResourceFactory(ResourceFactory ref, String base, Class<?> clazz)
   {
      addResourceFactory(ref, resourceBuilder, base, clazz);
   }

   public void addResourceFactory(ResourceFactory ref, ResourceBuilder resourceBuilder, String base, Class<?> clazz)
   {
      Class<?>[] classes = {clazz};
      addResourceFactory(ref, resourceBuilder, base, classes);
   }

   /**
    * ResourceFactory.getScannableClass() is not used, only the clazz parameter and not any implemented interfaces
    * of the clazz parameter.
    *
    * @param ref resource factory
    * @param base    base URI path for any resources provided by the factory, in addition to rootPath
    * @param classes specific class
    */
   public void addResourceFactory(ResourceFactory ref, String base, Class<?>[] classes)
   {
      addResourceFactory(ref, resourceBuilder, base, classes);
   }

   public void addResourceFactory(ResourceFactory ref, ResourceBuilder resourceBuilder, String base, Class<?>[] classes)
   {
      if (ref != null) ref.registered(providerFactory);
      for (Class<?> clazz : classes)
      {
         if (Proxy.isProxyClass(clazz))
         {
            for (Class<?> intf : clazz.getInterfaces())
            {
               ResourceClass resourceClass = resourceBuilder.getRootResourceFromAnnotations(intf);
               debug("interface=" + intf.getName() + " resourceClass=" + resourceClass.getPath());
               register(ref, base, resourceClass);
            }
         }
         else
         {
            ResourceClass resourceClass = resourceBuilder.getRootResourceFromAnnotations(clazz);
            debug("clazz=" + clazz.getName() + " path=" + resourceClass.getPath() + " uh=" + resourceClass.getResourceMethods());
            register(ref, base, resourceClass);
         }
      }

      // https://issues.jboss.org/browse/JBPAPP-7871
      for (Class<?> clazz : classes)
      {
         for (Method method : getDeclaredMethods(clazz))
         {
            Method _method = resourceBuilder.getAnnotatedMethod(clazz, method);
            if (_method != null && !java.lang.reflect.Modifier.isPublic(_method.getModifiers()))
            {
               LogMessages.LOGGER.JAXRSAnnotationsFoundAtNonPublicMethod(method.getDeclaringClass().getName(), method.getName());
            }
         }
      }

   }

   private Method[] getDeclaredMethods(final Class<?> clazz) {
      Method[] methodList = new Method[0];
      try {
         if (System.getSecurityManager() == null) {
            methodList = clazz.getDeclaredMethods();
         } else {
            methodList = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
               @Override
               public Method[] run() throws Exception {
                  return clazz.getDeclaredMethods();
               }
            });
         }
      } catch (PrivilegedActionException pae) {

      }
      return methodList;
   }

   @Override
   public void addResourceFactory(ResourceFactory rf, String base, ResourceClass resourceClass)
   {
      if (rf != null) rf.registered(providerFactory);
      register(rf, base, resourceClass);
   }

   protected void register(ResourceFactory rf, String base, ResourceClass resourceClass)
   {
       Thread.dumpStack();
       debug("rf=" + rf + " base=" + base + " resourceClass=" + resourceClass);
      for (ResourceMethod method : resourceClass.getResourceMethods())
      {
          debug("ResourceMethod method=" + method);
         processMethod(rf, base, method);
      }
      for (ResourceLocator method : resourceClass.getResourceLocators())
      {
          debug("ResourceLocator method=" + method);
         processMethod(rf, base, method);
      }
   }

   /**
    * Resteasy 2.x does not properly handle sub-resource and sub-resource locator
    * endpoints with the same uri.  Resteasy 3.x does handle this properly.  In
    * assisting customers identify this issue during an upgrade from Resteasy 2 to 3
    * provides a waring when the situation is found.
    */
   public void checkAmbiguousUri()
   {
      for (Map.Entry<String, List<ResourceInvoker>> entry : this.root.getBounded().entrySet())
      {
         List<ResourceInvoker> values = entry.getValue();
         if (values.size() > 1) {
            int locatorCnt = 0;
            int methodCnt = 0;
            for(ResourceInvoker rInvoker : values)
            {
               if (rInvoker instanceof ResourceLocatorInvoker)
               {
                  locatorCnt++;
               } else if (rInvoker instanceof ResourceMethodInvoker)
               {
                  methodCnt++;
               }
            }
            if (methodCnt > 0 && locatorCnt > 0)
            {
               StringBuilder sb = new StringBuilder();
               int cnt = values.size();
               for (int i=0; i < cnt; i++) {
                  ResourceInvoker exp = values.get(i);
                  sb.append(exp.getMethod().getDeclaringClass().getName())
                          .append(".")
                          .append(exp.getMethod().getName());
                  if (i < cnt-1)
                  {
                     sb.append(", ");
                  }
               }
               LogMessages.LOGGER.uriAmbiguity(entry.getKey(), sb.toString());
            }
         }
      }
   }

   protected void processMethod(ResourceFactory rf, String base, ResourceLocator method)
   {
       debug("base=" + base);
      ResteasyUriBuilder builder = new ResteasyUriBuilderImpl();
      if (base != null)
         builder.path(base);
      builder.path(method.getFullpath());
      String fullpath = builder.getPath();
      if (fullpath == null)
         fullpath = "";
      debug("fullpath=" + fullpath);

      builder = new ResteasyUriBuilderImpl();
      if (base != null)
         builder.path(base);
      builder.path(method.getResourceClass().getPath());
      debug("method.getResourceClass().getPath()=" + method.getResourceClass().getPath());
      String classExpression = builder.getPath();
      if (classExpression == null)
         classExpression = "";
      debug("classExpression=" + classExpression);

      InjectorFactory injectorFactory = providerFactory.getInjectorFactory();
      if (method instanceof ResourceMethod)
      {
         ResourceMethodInvoker invoker
                 = new ResourceMethodInvoker((ResourceMethod) method, injectorFactory, rf, providerFactory);
         if (widerMatching) {
             debug("if");
            rootNode.addInvoker(fullpath, invoker);
         }
         else {
             debug("else");
             root.addInvoker(classExpression, fullpath, invoker);
         }
         statisticsController.register(invoker);
      }
      else
      {
         ResourceLocatorInvoker locator = new ResourceLocatorInvoker(rf, injectorFactory, providerFactory, method);
         if (widerMatching)
            rootNode.addInvoker(fullpath, locator);
         else root.addInvoker(classExpression, fullpath, locator);
         statisticsController.register(locator);
      }
   }

   /**
    * Find all endpoints reachable by clazz and unregister them.
    *
    * @param clazz class
    */
   public void removeRegistrations(Class clazz)
   {
      removeRegistrations(clazz, null);
   }

   public void removeRegistrations(Class clazz, String base)
   {
      Class restful = GetRestful.getRootResourceClass(clazz);
      removeRegistration(base, restful);
   }

   @Override
   public void removeRegistrations(ResourceClass resourceClass)
   {
      for (ResourceMethod method : resourceClass.getResourceMethods())
      {
         if (widerMatching) rootNode.removeBinding(method.getFullpath(), method.getMethod());
         else root.removeBinding(resourceClass.getPath(), method.getFullpath(), method.getMethod());
      }
      for (ResourceLocator method : resourceClass.getResourceLocators())
      {
         if (widerMatching) rootNode.removeBinding(method.getFullpath(), method.getMethod());
         else root.removeBinding(resourceClass.getPath(), method.getFullpath(), method.getMethod());
      }
   }

   private void removeRegistration(String base, Class<?> clazz)
   {
      for (Method method : clazz.getMethods())
      {
         Path path = method.getAnnotation(Path.class);
         Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
         if (path == null && httpMethods == null) continue;

         ResteasyUriBuilder builder = new ResteasyUriBuilderImpl();
         if (base != null) builder.path(base);
         if (clazz.isAnnotationPresent(Path.class)) builder.path(clazz);
         String classExpression = builder.getPath();
         if (path != null) builder.path(method);
         String fullpath = builder.getPath();
         if (fullpath == null) fullpath = "";

         if (widerMatching) rootNode.removeBinding(fullpath, method);
         else root.removeBinding(classExpression, fullpath, method);
      }
   }

   public Map<String, List<ResourceInvoker>> getBounded()
   {
      if (widerMatching) return rootNode.getBounded();
      else return root.getBounded();
   }

   /**
    * Number of endpoints registered.
    *
    * @return number of endpoints registered
    */
   public int getSize()
   {
      if (widerMatching) return rootNode.getSize();
      else return root.getSize();
   }

   /**
    * Find a resource to invoke on.
    *
    * @return resource invoker
    */
   public ResourceInvoker getResourceInvoker(HttpRequest request)
   {
      RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
      final long timestamp = tracingLogger.timestamp("MATCH_SUMMARY");
      try {
         if (widerMatching) {
            return rootNode.match(request, 0);
         } else {
            return root.match(request, 0);
         }
      } catch (RuntimeException e) {
         throw e;
      }
      finally {
         tracingLogger.logDuration("MATCH_SUMMARY", timestamp);
      }
   }
   
   private void debug(String message) {
       boolean print = true; // allows me to turn of debug for this class without deleting statments
       if (print) {
           StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
           // Index 0 is getStackTrace(), 1 is this method (debug), 2 is the caller
           StackTraceElement caller = stackTrace[2];
           String methodName = caller.getMethodName();
           int lineNumber = caller.getLineNumber();
           System.out.println("Adam: " + this.getClass().getSimpleName() + "." + methodName + "()#L" + lineNumber + " - " + message );
       }
   }
}