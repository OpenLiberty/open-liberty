// https://github.com/resteasy/resteasy/blob/fcbd0d4afb27e06bbe0750f39327060f417cdaef/resteasy-core-spi/src/main/java/org/jboss/resteasy/spi/ConstructorInjector.java
package org.jboss.resteasy.spi;

import javax.ws.rs.WebApplicationException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ConstructorInjector
{
   /**
    * Construct outside the scope of an HTTP request.  Useful for singleton factories.
    * @param unwrapAsync unwrap async
    * @return constructed object or a CompletionStage<Object> if construction is async
    */
   Object construct(boolean unwrapAsync);

   /**
    * Construct inside the scope of an HTTP request.
    *
    * @param request http request
    * @param response http response
    * @param unwrapAsync unwrap async
    * @return constructed object or a CompletionStage<Object> if construction is async
    * @throws Failure if failure occurred
    * @throws WebApplicationException if application exception occurred
    * @throws ApplicationException if application exception occurred
    */
   Object construct(HttpRequest request, HttpResponse response, boolean unwrapAsync) throws Failure, WebApplicationException, ApplicationException;

   /**
    * Create an arguments list from injectable tings outside the scope of an HTTP request.  Useful for singleton factories
    * in cases where the resource factory wants to allocate the object itself, but wants resteasy to populate
    * the arguments.
    *
    * @param unwrapAsync unwrap async
    * @return array of arguments or a CompletionStage<Object[]> if args is async
    */
   Object injectableArguments(boolean unwrapAsync);

   /**
    * Create an argument list inside the scope of an HTTP request.
    * Useful in cases where the resource factory wants to allocate the object itself, but wants resteasy to populate
    * the arguments.
    *
    * @param request http request
    * @param response http response
    * @param unwrapAsync unwrap async
    * @return array of arguments or a CompletionStage<Object[]> if args is async
    * @throws Failure if failure occurred
    */
   Object injectableArguments(HttpRequest request, HttpResponse response, boolean unwrapAsync) throws Failure;
}