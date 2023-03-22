package org.jboss.resteasy.core;

import org.jboss.resteasy.plugins.providers.sse.SseImpl;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.LoggableFailure;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.util.Types;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("unchecked")
public class ContextParameterInjector implements ValueInjector
{
   private static Constructor<?> constructor;

   private Class<?> rawType;
   private Class<?> proxy;
   private ResteasyProviderFactory factory;
   private Type genericType;
   private Annotation[] annotations;
   private volatile boolean outputStreamWasWritten = false;

   static
   {
      constructor = AccessController.doPrivileged(new PrivilegedAction<Constructor<?>>()
      {
         @Override
         public Constructor<?> run()
         {
            try
            {
               Class.forName("jakarta.servlet.http.HttpServletResponse", false, Thread.currentThread().getContextClassLoader());
               Class<?> clazz = Class.forName("org.jboss.resteasy.core.ContextServletOutputStream");
               return clazz.getDeclaredConstructor(ContextParameterInjector.class, OutputStream.class);
            }
            catch (Exception e)
            {
               return null;
            }
         }
      });
   }

   public ContextParameterInjector(final Class<?> proxy, final Class<?> rawType, final Type genericType, final Annotation[] annotations, final ResteasyProviderFactory factory)
   {
      this.rawType = rawType;
      this.genericType = genericType;
      this.proxy = proxy;
      this.factory = factory;
      this.annotations = annotations;
   }

   @Override
   public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync)
   {
      // we always inject a proxy for interface types just in case the per-request target is a pooled object
      // i.e. in the case of an SLSB
      if (rawType.equals(Providers.class)) return factory;
      if (!rawType.isInterface() || rawType.equals(SseEventSink.class) || hasAsyncContextData(factory, genericType))
      {
         return unwrapIfRequired(request, factory.getContextData(rawType, genericType, annotations, unwrapAsync), unwrapAsync);
      }
      else if (rawType.equals(Sse.class))
      {
         return new SseImpl();
      } else if (rawType == CompletionStage.class) {
         return new CompletionStageHolder((CompletionStage<?>)createProxy());
      }
      return createProxy();
   }

   private static boolean hasAsyncContextData(ResteasyProviderFactory factory, Type genericType)
   {
      return factory.getAsyncContextInjectors().containsKey(Types.boxPrimitives(genericType));
   }

   private Object unwrapIfRequired(HttpRequest request, Object contextData, boolean unwrapAsync)
   {
      if(unwrapAsync && rawType != CompletionStage.class && contextData instanceof CompletionStage) {
         // FIXME: do not unwrap if we have no request?
         if(request != null )
         {
            boolean resolved = ((CompletionStage<Object>) contextData).toCompletableFuture().isDone();
            if(!resolved)
            {
               // make request async
               if(!request.getAsyncContext().isSuspended())
                  request.getAsyncContext().suspend();

               Map<Class<?>, Object> contextDataMap = ResteasyContext.getContextDataMap();
               // Don't forget to restore the context
               return ((CompletionStage<Object>) contextData).thenApply(value -> {
                  ResteasyContext.pushContextDataMap(contextDataMap);
                  return value;
               });
            }
         }
         return (CompletionStage<Object>) contextData;
      } else if (rawType == CompletionStage.class && contextData instanceof CompletionStage) {
         return new CompletionStageHolder((CompletionStage<?>)contextData);
      } else if (!unwrapAsync && rawType != CompletionStage.class && contextData instanceof CompletionStage) {
         throw new LoggableFailure(Messages.MESSAGES.shouldBeUnreachable());
      }
      return contextData;
   }

   private class GenericDelegatingProxy implements InvocationHandler
   {
      public Object invoke(Object o, Method method, Object[] objects) throws Throwable
      {
         try
         {

            Object delegate = factory.getContextData(rawType, genericType, annotations, false);
            if (delegate == null)
            {
               String name = method.getName();
               if (o instanceof ResourceInfo && ("getResourceMethod".equals(name) || "getResourceClass".equals(name)))
               {
                  return null;
               }

               if ("getContextResolver".equals(name))
               {
                  return method.invoke(factory, objects);
               }
               throw new LoggableFailure(Messages.MESSAGES.unableToFindContextualData(rawType.getName()));
            }
            // Fix for RESTEASY-1721
            if ("jakarta.servlet.http.HttpServletResponse".equals(rawType.getName()))
            {
               if ("getOutputStream".equals(method.getName()))
               {
                  OutputStream sos = (OutputStream) method.invoke(delegate, objects);
                  return wrapServletOutputStream(sos);
               }
            }
            return method.invoke(delegate, objects);
         }
         catch (IllegalAccessException e)
         {
            throw new RuntimeException(e);
         }
         catch (IllegalArgumentException e)
         {
            throw new RuntimeException(e);
         }
         catch (InvocationTargetException e)
         {
            throw e.getCause();
         }
      }
   }

   @Override
   public Object inject(boolean unwrapAsync)
   {
      //if (type.equals(Providers.class)) return factory;
      if (rawType.equals(Application.class) || rawType.equals(SseEventSink.class) || hasAsyncContextData(factory, genericType))
      {
         return factory.getContextData(rawType, genericType, annotations, unwrapAsync);
      }
      else if (rawType.equals(Sse.class))
      {
         return new SseImpl();
      }
      else if (!rawType.isInterface())
      {
         Object delegate = factory.getContextData(rawType, genericType, annotations, unwrapAsync);
         if (delegate != null) return unwrapIfRequired(null, delegate, unwrapAsync);
         else throw new RuntimeException(Messages.MESSAGES.illegalToInjectNonInterfaceType());
      } else if (rawType == CompletionStage.class) {
         return new CompletionStageHolder((CompletionStage<?>)createProxy());
      }

      return createProxy();
   }

   protected Object createProxy()
   {
      if (proxy != null)
      {
         try
         {
            return proxy.getConstructors()[0].newInstance(new GenericDelegatingProxy());
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
      else
      {
         Object delegate = factory.getContextData(rawType, genericType, annotations, false);
         Class<?>[] intfs = computeInterfaces(delegate, rawType);
         ClassLoader clazzLoader = null;
         final SecurityManager sm = System.getSecurityManager();
         if (sm == null) {
             // liberty change - use this classloader
            //clazzLoader = delegate == null ? rawType.getClassLoader() : delegate.getClass().getClassLoader();
            clazzLoader = this.getClass().getClassLoader();
         } else {
            clazzLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
               @Override
               public ClassLoader run() {
                  //return delegate == null ? rawType.getClassLoader() : delegate.getClass().getClassLoader();
                  return this.getClass().getClassLoader(); //liberty change
               }
            });
         }
         return Proxy.newProxyInstance(clazzLoader, intfs, new GenericDelegatingProxy());
      }
   }

   protected Class<?>[] computeInterfaces(Object delegate, Class<?> cls)
   {
      ResteasyDeployment deployment = ResteasyContext.getContextData(ResteasyDeployment.class);
      if (deployment != null
         && Boolean.TRUE.equals(deployment.getProperty(ResteasyContextParameters.RESTEASY_PROXY_IMPLEMENT_ALL_INTERFACES)))
      {
         Set<Class<?>> set = new HashSet<>();
         set.add(cls);
         if (delegate != null) {
            Class<?> delegateClass = delegate.getClass();
            while (delegateClass != null) {
               for (Class<?> intf : delegateClass.getInterfaces()) {
                  set.add(intf);
                  for (Class<?> superIntf : intf.getInterfaces()) {
                     set.add(superIntf);
                  }
               }
               delegateClass = delegateClass.getSuperclass();
            }
         }
         return set.toArray(new Class<?>[]{});
      }
      return new Class<?>[]{cls};
   }

   OutputStream wrapServletOutputStream(OutputStream os)
   {
      if (constructor != null)
      {
         try
         {
            return (OutputStream) constructor.newInstance(this, os);
         }
         catch (Exception e)
         {
            return os;
         }
      }
      return os;
   }

   boolean isOutputStreamWasWritten()
   {
      return outputStreamWasWritten;
   }

   void setOutputStreamWasWritten(boolean outputStreamWasWritten)
   {
      this.outputStreamWasWritten = outputStreamWasWritten;
   }
}
