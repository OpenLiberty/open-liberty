// https://github.com/resteasy/resteasy/blob/fcbd0d4afb27e06bbe0750f39327060f417cdaef/resteasy-cdi/src/main/java/org/jboss/resteasy/cdi/CdiConstructorInjector.java
package org.jboss.resteasy.cdi;

import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.cdi.i18n.Messages;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.AmbiguousResolutionException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.WebApplicationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This ConstructorInjector implementation uses CDI's BeanManager to obtain
 * a contextual instance of a bean.
 *
 * @author Jozef Hartinger
 *
 */
public class CdiConstructorInjector implements ConstructorInjector
{
   private BeanManager manager;
   // Liberty Change Start
   private Collection<Type> types;
   private Constructor<?> constructor;

   public CdiConstructorInjector(final Collection<Type> type, final BeanManager manager, final Constructor<?> constructor)
   {
      this.types = type;
      this.manager = manager;
      this.constructor = constructor;
   }
   // Liberty Change End

   @Override
   public Object construct(boolean unwrapAsync)
   {
       // Liberty Change Start
       // Get the target bean class (first concrete class that's not Object)
       Class<?> targetBeanClass = null;
       for (Type type : types) {
          if (type instanceof Class<?>) {
             Class<?> clazz = (Class<?>) type;
             if (!clazz.isInterface() && !clazz.equals(Object.class)) {
                targetBeanClass = clazz;
                break;
             }
          }
       }
       
       // Try interfaces first, then concrete class
       // For EJBs with @Local interfaces not implemented by the class, the bean is a proxy
       // that only implements the interfaces. We must resolve using interface types.
       List<Type> interfaceTypes = new ArrayList<>();
       List<Type> classTypes = new ArrayList<>();
       
       for (Type type : types) {
          if (type instanceof Class && ((Class<?>) type).isInterface()) {
             interfaceTypes.add(type);
          } else {
             classTypes.add(type);
          }
       }
       
       // Try interface types first
       List<Type> orderedTypes = new ArrayList<>();
       orderedTypes.addAll(interfaceTypes);
       orderedTypes.addAll(classTypes);
       
       for (Type type : orderedTypes) {
          Set<Bean<?>> beans = manager.getBeans(type);
    
          if (beans.size() > 1)
          {
             Set<Bean<?>> modifiableBeans = new HashSet<Bean<?>>();
             modifiableBeans.addAll(beans);
             // Ambiguous dependency may occur if a resource has subclasses
             // Therefore we remove those beans
             
             // Check if type is an interface or class
             boolean isInterface = (type instanceof Class) && ((Class<?>) type).isInterface();
             
             for (Iterator<Bean<?>> iterator = modifiableBeans.iterator(); iterator.hasNext();)
             {
                Bean<?> bean = iterator.next();
                if (isInterface) {
                   // For interfaces, if we have a target bean class, keep only beans matching that class
                   // This disambiguates when multiple EJBs implement the same @Local interface
                   if (targetBeanClass != null && !bean.getBeanClass().equals(targetBeanClass)) {
                      iterator.remove();
                   } else if (!bean.getTypes().contains(type) && !bean.isAlternative()) {
                      // Keep beans that have the interface in their types
                      // Remove beans that don't have this interface in their type closure
                      iterator.remove();
                   }
                } else {
                   // For classes, use the original logic
                   if (!bean.getBeanClass().equals(type) && !bean.isAlternative()) {
                      // remove Beans that have clazz in their type closure but not as a base class
                      iterator.remove();
                   }
                }
             }
             beans = modifiableBeans;
          }
    
          if (LogMessages.LOGGER.isDebugEnabled()) //keep this check for performance reasons, as toString() is expensive on CDI Bean
          {
             LogMessages.LOGGER.debug(Messages.MESSAGES.beansFound(type, beans));
          }
    
          Bean<?> bean = manager.resolve(beans);
          if (bean != null) {
              CreationalContext<?> context = manager.createCreationalContext(bean);
              Object result = manager.getReference(bean, type, context);
              return result;
          }
       }
       
       // If no bean could be resolved from any type, fall back to regular constructor instantiation
       // This allows non-CDI beans to work while still supporting CDI injection when available
       try {
           LogMessages.LOGGER.debug("CDI bean lookup failed for all types, falling back to regular constructor instantiation");
           return constructor.newInstance();
       } catch (Exception e) {
           // Let exceptions from the constructor propagate to RESTEasy
           throw new RuntimeException("Failed to instantiate class using constructor", e);
       }
       // Liberty Change End
   }

   @Override
   public Object construct(HttpRequest request, HttpResponse response, boolean unwrapAsync) throws Failure, WebApplicationException, ApplicationException
   {
      return construct(unwrapAsync);
   }

   @Override
   public Object injectableArguments(boolean unwrapAsync)
   {
      return null;
   }

   @Override
   public Object injectableArguments(HttpRequest request, HttpResponse response, boolean unwrapAsync) throws Failure
   {
      return injectableArguments(unwrapAsync);
   }
}