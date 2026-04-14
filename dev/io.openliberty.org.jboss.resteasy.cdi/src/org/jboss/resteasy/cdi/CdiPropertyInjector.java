// https://github.com/resteasy/resteasy/blob/fcbd0d4afb27e06bbe0750f39327060f417cdaef/resteasy-cdi/src/main/java/org/jboss/resteasy/cdi/CdiPropertyInjector.java
package org.jboss.resteasy.cdi;

import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.weld.proxy.WeldClientProxy;

import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.WebApplicationException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * JAX-RS property injection is performed twice on CDI Beans. Firstly by the JaxrsInjectionTarget
 * wrapper and then again by RESTEasy (which operates on Weld proxies instead of the underlying instances).
 * To eliminate this, we enabled the injector only for non-CDI beans (JAX-RS components outside of BDA) or
 * CDI components that are not JAX-RS components.
 *
 * @author <a href="mailto:jharting@redhat.com">Jozef Hartinger</a>
 */
public class CdiPropertyInjector implements PropertyInjector
{
   private PropertyInjector delegate;
   private Class<?> clazz;
   private boolean injectorEnabled = true;

   public CdiPropertyInjector(final PropertyInjector delegate, final Class<?> clazz, final Map<Class<?>, Collection<Type>> sessionBeanInterface, final BeanManager manager) // Liberty Change
   {
      this.delegate = delegate;
      this.clazz = clazz;

      if (sessionBeanInterface.containsKey(clazz))
      {
         injectorEnabled = false;
      }
      if (!manager.getBeans(clazz).isEmpty() && Utils.isJaxrsComponent(clazz))
      {
         injectorEnabled = false;
      }
   }

   @Override
   public CompletionStage<Void> inject(Object target, boolean unwrapAsync)
   {
      if (injectorEnabled)
      {
         return delegate.inject(target, unwrapAsync);
      }
      return null;
   }

   @Override
   public CompletionStage<Void> inject(HttpRequest request, HttpResponse response, Object target, boolean unwrapAsync) throws Failure, WebApplicationException, ApplicationException
   {
      if (injectorEnabled)
      {
         Object actualTarget = target;
         if (actualTarget instanceof WeldClientProxy)
         {
            actualTarget = ((WeldClientProxy) target).getMetadata().getContextualInstance();
         }
         return delegate.inject(request, response, actualTarget, unwrapAsync);
      }
      return null;
   }

   @Override
   public String toString()
   {
      return "CdiPropertyInjector (enabled: " + injectorEnabled + ") for " + clazz;
   }
}