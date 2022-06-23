package org.jboss.resteasy.core;

import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class FormInjector implements ValueInjector
{
//   private Class type; // Liberty Change
   private ConstructorInjector constructorInjector;
   private PropertyInjector propertyInjector;

//   @SuppressWarnings(value = "unchecked") // Liberty Change
   public FormInjector(final Class<?> type, final ResteasyProviderFactory factory)
   {
//      this.type = type; // Liberty Change
      Constructor<?> constructor; // Liberty Change

      try
      {
         constructor = type.getDeclaredConstructor();
         //Liberty change - doPriv - requires above changes for proper constructor scope
         AccessController.doPrivileged((PrivilegedAction<Boolean>)() -> {
             constructor.setAccessible(true);
             return true;
         });
         ;
      }
      catch (NoSuchMethodException e)
      {
         throw new RuntimeException(Messages.MESSAGES.unableToInstantiateForm());
      }

      constructorInjector = factory.getInjectorFactory().createConstructor(constructor, factory);
      propertyInjector = factory.getInjectorFactory().createPropertyInjector(type, factory);

   }

   @Override
   public Object inject(boolean unwrapAsync)
   {
      throw new IllegalStateException(Messages.MESSAGES.cannotInjectIntoForm());
   }

   @Override
   public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync)
   {
      Object obj =  constructorInjector.construct(unwrapAsync);
      if (obj instanceof CompletionStage) {
         @SuppressWarnings("unchecked")
         CompletionStage<Object> stage = (CompletionStage<Object>)obj;
         return stage.thenCompose(target -> {
            CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, target, unwrapAsync);
            return propertyStage == null ? CompletableFuture.completedFuture(target) : propertyStage
                    .thenApply(v -> target);
         });
      }
      CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, obj, unwrapAsync);
      return propertyStage == null ? obj : propertyStage.thenApply(v -> obj);

   }
}
