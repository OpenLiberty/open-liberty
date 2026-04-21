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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.WebApplicationException;

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
   private Collection<Type> types; // Liberty Change

   public CdiConstructorInjector(final Collection<Type> type, final BeanManager manager) // Liberty Change
   {
      this.types = type; // Liberty Change
      this.manager = manager;
   }

   @Override
   public Object construct(boolean unwrapAsync)
   {
       debug("types=" + types);

       // Liberty Change Start - Get the target bean class (first concrete class that's not Object)
       Class<?> targetBeanClass = null;
       for (Type type : types) {
          if (type instanceof Class<?>) {
             Class<?> clazz = (Class<?>) type;
             if (!clazz.isInterface() && !clazz.equals(Object.class)) {
                targetBeanClass = clazz;
                debug("Target bean class: " + targetBeanClass);
                break;
             }
          }
       }
       
       // Liberty Change Start - Try interfaces first, then concrete class
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
          debug("Trying type: " + type);
          Set<Bean<?>> beans = manager.getBeans(type);
          debug("Found " + beans.size() + " beans for type: " + type);
    
          if (beans.size() > 1)
          {
             Set<Bean<?>> modifiableBeans = new HashSet<Bean<?>>();
             modifiableBeans.addAll(beans);
             // Ambiguous dependency may occur if a resource has subclasses
             // Therefore we remove those beans
             
             // Check if type is an interface or class
             boolean isInterface = (type instanceof Class) && ((Class<?>) type).isInterface();
             debug("Type " + type + " is interface: " + isInterface + ", filtering " + modifiableBeans.size() + " beans");
             
             for (Iterator<Bean<?>> iterator = modifiableBeans.iterator(); iterator.hasNext();)
             {
                Bean<?> bean = iterator.next();
                if (isInterface) {
                   // For interfaces, if we have a target bean class, keep only beans matching that class
                   // This disambiguates when multiple EJBs implement the same @Local interface
                   if (targetBeanClass != null && !bean.getBeanClass().equals(targetBeanClass)) {
                      debug("  Removing bean " + bean.getBeanClass() + " (doesn't match target " + targetBeanClass + ")");
                      iterator.remove();
                   } else if (!bean.getTypes().contains(type) && !bean.isAlternative()) {
                      // Keep beans that have the interface in their types
                      // Remove beans that don't have this interface in their type closure
                      debug("  Removing bean " + bean.getBeanClass() + " (doesn't have type " + type + ")");
                      iterator.remove();
                   } else {
                      debug("  Keeping bean " + bean.getBeanClass());
                   }
                } else {
                   // For classes, use the original logic
                   if (!bean.getBeanClass().equals(type) && !bean.isAlternative()) {
                      // remove Beans that have clazz in their type closure but not as a base class
                      iterator.remove();
                   }
                }
             }
             debug("After filtering: " + modifiableBeans.size() + " beans remain");
             beans = modifiableBeans;
          }
    
          if (LogMessages.LOGGER.isDebugEnabled()) //keep this check for performance reasons, as toString() is expensive on CDI Bean
          {
             LogMessages.LOGGER.debug(Messages.MESSAGES.beansFound(type, beans));
          }
    
          Bean<?> bean = manager.resolve(beans);
          if (bean != null) {
              debug("Resolved bean: " + bean.getBeanClass());
              CreationalContext<?> context = manager.createCreationalContext(bean);
              try {
                  Object result = manager.getReference(bean, type, context);
                  debug("Got reference: " + (result != null ? result.getClass() : "null"));
                  return result;
              } catch (Exception e) {
                  debug("Exception getting reference: " + e.getMessage());
                  // Continue to next type if this one fails
              }
          } else {
              debug("No bean resolved for type: " + type);
          }
       }
       debug("Returning null - no beans found for any type");
       return null;
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
   
   public static void debug(String message) {
       boolean print = true; // allows me to turn of debug for this class without deleting statments
       if (print) {
           StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

           // Index 0 is getStackTrace(), 1 is this method (debug()), 2 is the caller
           StackTraceElement caller = stackTrace[2];

           String className = caller.getClassName();
           String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
           String methodName = caller.getMethodName();
           int lineNumber = caller.getLineNumber();

           System.out.println(simpleClassName + "." + methodName + "()#L" + lineNumber + " - " + message );
       }
   }
}