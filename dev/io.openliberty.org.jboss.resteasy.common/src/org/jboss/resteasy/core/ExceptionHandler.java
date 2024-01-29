package org.jboss.resteasy.core;

import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.spi.NoLogWebApplicationException;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.UnhandledException;
import org.jboss.resteasy.spi.WriterException;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ExceptionHandler
{
   protected ResteasyProviderFactoryImpl providerFactory;
   protected Set<String> unwrappedExceptions = new HashSet<String>();
   protected boolean mapperExecuted;

   public ExceptionHandler(final ResteasyProviderFactory providerFactory, final Set<String> unwrappedExceptions)
   {
      this.providerFactory = (ResteasyProviderFactoryImpl)providerFactory;
      this.unwrappedExceptions = unwrappedExceptions;
   }

   public boolean isMapperExecuted()
   {
      return mapperExecuted;
   }

   /**
    * If there exists an Exception mapper for exception, execute it, otherwise, do NOT recurse up class hierarchy
    * of exception.
    *
    * @param exception exception
    * @param logger logger
    * @return response response object
    */
   @SuppressWarnings(value = "unchecked")
   protected Response executeExactExceptionMapper(Throwable exception, RESTEasyTracingLogger logger) {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();

      ExceptionMapper mapper = providerFactory.getExceptionMapperForClass(exception.getClass());
      if (mapper == null) return null;
      mapperExecuted = true;
      long timestamp = logger.timestamp("EXCEPTION_MAPPING");
      Response resp = mapper.toResponse(exception);
      logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception, exception.getLocalizedMessage(), resp);
      return resp;
   }

   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExactExceptionMapper(Throwable exception) {
      return executeExactExceptionMapper(exception, null);
   }

   @SuppressWarnings(value = "unchecked")
   protected Response executeExceptionMapperForClass(Throwable exception, Class clazz, RESTEasyTracingLogger logger)
   {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();
      ExceptionMapper mapper = providerFactory.getExceptionMapperForClass(clazz);
      if (mapper == null) return null;
      mapperExecuted = true;
      long timestamp = logger.timestamp("EXCEPTION_MAPPING");
      Response resp = mapper.toResponse(exception);
      logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception, exception.getLocalizedMessage(), resp);
      return resp;
   }

   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExceptionMapperForClass(Throwable exception, Class clazz)
   {
      return executeExceptionMapperForClass(exception, clazz, null);
   }

   protected Response handleApplicationException(HttpRequest request, ApplicationException e, RESTEasyTracingLogger logger)
   {
      Response jaxrsResponse = null;
      // See if there is a mapper for ApplicationException
      if ((jaxrsResponse = executeExceptionMapperForClass(e, ApplicationException.class, logger)) != null) {
         return jaxrsResponse;
      }
      jaxrsResponse = unwrapException(request, e, logger);
      if (jaxrsResponse == null) {
         throw new UnhandledException(e.getCause());
      }
      return jaxrsResponse;
   }

   /**
    * Execute an ExceptionMapper if one exists for the given exception.  Recurse to base class if not found.
    *
    * @param exception exception
    * @param logger logger
    * @return true if an ExceptionMapper was found and executed
    */
   @SuppressWarnings(value = "unchecked")
   protected Response executeExceptionMapper(Throwable exception, RESTEasyTracingLogger logger)
   {
      if (logger == null)
         logger = RESTEasyTracingLogger.empty();

      ExceptionMapper mapper = null;

      Class causeClass = exception.getClass();
      while (mapper == null) {
         if (causeClass == null) break;
         mapper = providerFactory.getExceptionMapperForClass(causeClass);
         if (mapper == null) causeClass = causeClass.getSuperclass();
      }

      if (mapper != null) {
         mapperExecuted = true;

         final long timestamp = logger.timestamp("EXCEPTION_MAPPING");
         Response jaxrsResponse = mapper.toResponse(exception);
         logger.logDuration("EXCEPTION_MAPPING", timestamp, mapper, exception, exception.getLocalizedMessage(), jaxrsResponse);

         if (jaxrsResponse == null) {
            jaxrsResponse = Response.status(204).build();
         }
         return jaxrsResponse;
      }
      return null;
   }

   @Deprecated
   @SuppressWarnings(value = "unchecked")
   public Response executeExceptionMapper(Throwable exception)
   {
      return executeExactExceptionMapper(exception, null);
   }


   protected Response unwrapException(HttpRequest request, Throwable e, RESTEasyTracingLogger logger)
   {
      Response jaxrsResponse = null;
      Throwable unwrappedException = e.getCause();

      /*
       *                If the response property of the exception does not
       *                contain an entity and an exception mapping provider
       *                (see section 4.4) is available for
       *                WebApplicationException an implementation MUST use the
       *                provider to create a new Response instance, otherwise
       *                the response property is used directly.
       */

      if (unwrappedException instanceof WebApplicationException) {
         WebApplicationException wae = (WebApplicationException) unwrappedException;
         Response response = wae.getResponse();
         if (response != null) {
            try {
               if (response.getEntity() != null) return response;
            }
            catch(IllegalStateException ise) {
               // IllegalStateException from ClientResponse.getEntity() means the response is closed and got no entity
            }
         }
      }

      jaxrsResponse = executeExceptionMapper(unwrappedException, logger);

      if (jaxrsResponse != null) {
         return jaxrsResponse;
      }
      if (unwrappedException instanceof WebApplicationException) {
         return handleWebApplicationException((WebApplicationException) unwrappedException);
      }
      else if (unwrappedException instanceof Failure) {
         return handleFailure(request, (Failure) unwrappedException);
      }
      else {
         if (unwrappedExceptions.contains(unwrappedException.getClass().getName()) && unwrappedException.getCause() != null) {
            return unwrapException(request, unwrappedException, logger);
         }
         else {
            return null;
         }
      }
   }

   protected Response handleFailure(HttpRequest request, Failure failure) {
      if (failure.isLoggable())
         LogMessages.LOGGER.failedExecutingError(request.getHttpMethod(), request.getUri().getPath(), failure);
      else
         LogMessages.LOGGER.failedExecutingDebug(request.getHttpMethod(), request.getUri().getPath(), failure);

      Response response = failure.getResponse();

      if (response != null) {
         return response;
      } else {
         Response.ResponseBuilder builder = Response.status(failure.getErrorCode());
         if (failure.getMessage() != null)
            builder.type(MediaType.TEXT_HTML).entity(failure.getMessage());
         Response resp = builder.build();
         return resp;
      }
   }
   protected Response handleClientErrorException(HttpRequest request,
                                                 ClientErrorException e) {

      LogMessages.LOGGER.failedExecutingDebug(request.getHttpMethod(),
              request.getUri().getPath(), e);

      Response response = e.getResponse();

      if (response != null)
      {
         BuiltResponse bResponse = (BuiltResponse)response;
         if (bResponse.getStatus() == HttpResponseCodes.SC_BAD_REQUEST
            || bResponse.getStatus() == HttpResponseCodes.SC_NOT_FOUND)
         {
            if (e.getMessage() != null)
            {
               Response.ResponseBuilder builder = bResponse.fromResponse(response);
               builder.type(MediaType.TEXT_HTML).entity(e.getMessage());
               return builder.build();
            }
         }
         return response;

      } else {

         Response.ResponseBuilder builder = Response.status(-1);
         if (e instanceof BadRequestException) {
            builder.status(HttpResponseCodes.SC_BAD_REQUEST);
         } else if (e instanceof NotFoundException) {
            builder.status(HttpResponseCodes.SC_NOT_FOUND);
         }

         if (e.getMessage() != null)
         {
            builder.type(MediaType.TEXT_HTML).entity(e.getMessage());
         }
         Response resp = builder.build();
         return resp;
      }
   }

   protected Response handleWriterException(HttpRequest request, WriterException e, RESTEasyTracingLogger logger)
   {
      Response jaxrsResponse = null;
      // See if there is a general mapper for WriterException
      if ((jaxrsResponse = executeExceptionMapperForClass(e, WriterException.class, logger)) != null) {
         return jaxrsResponse;
      }
      if (e.getResponse() != null || e.getErrorCode() > -1) {
         return handleFailure(request, e);
      }
      else if (e.getCause() != null) {
         if ((jaxrsResponse = unwrapException(request, e, logger)) != null) return jaxrsResponse;
      }
      e.setErrorCode(HttpResponseCodes.SC_INTERNAL_SERVER_ERROR);
      return handleFailure(request, e);
   }

   protected Response handleReaderException(HttpRequest request, ReaderException e, RESTEasyTracingLogger logger)
   {
      Response jaxrsResponse = null;
      // See if there is a general mapper for ReaderException
      if ((jaxrsResponse = executeExceptionMapperForClass(e, ReaderException.class, logger)) != null) {
         return jaxrsResponse;
      }
      if (e.getResponse() != null || e.getErrorCode() > -1) {
         return handleFailure(request, e);
      }
      else if (e.getCause() != null) {
         if ((jaxrsResponse = unwrapException(request, e, logger)) != null) return jaxrsResponse;
      }
      e.setErrorCode(HttpResponseCodes.SC_BAD_REQUEST);
      return handleFailure(request, e);
   }

   protected Response handleWebApplicationException(WebApplicationException wae)
   {
      if (wae instanceof NotFoundException)
      {
         LogMessages.LOGGER.failedToExecuteDebug(wae);
      }
      else if (!(wae instanceof NoLogWebApplicationException))
      {
         LogMessages.LOGGER.failedToExecute(wae);
      }
      Response response = wae.getResponse();
      return response;
   }

   public Response handleException(HttpRequest request, Throwable e) {

      Response jaxrsResponse = null;
      RESTEasyTracingLogger logger = RESTEasyTracingLogger.getInstance(request);

      // lookup mapper on class name of exception
      jaxrsResponse = executeExactExceptionMapper(e, logger);
      if (jaxrsResponse == null)
      {
         if (e instanceof ClientErrorException) {
            // These are BadRequestException and NotFoundException exceptions
            jaxrsResponse = executeExceptionMapper(e, logger);
            if (jaxrsResponse == null)
            {
               jaxrsResponse = handleClientErrorException(request, (ClientErrorException) e);
            }

         } else if (e instanceof WebApplicationException)
         {
            /*
             * If the response property of the exception does not
             * contain an entity and an exception mapping provider
             * (see section 4.4) is available for
             * WebApplicationException an implementation MUST use the
             * provider to create a new Response instance, otherwise
             * the response property is used directly.
             */
            WebApplicationException wae = (WebApplicationException) e;
            if (wae.getResponse() != null && wae.getResponse().getEntity() != null)
            {
               jaxrsResponse = wae.getResponse();
            } else
            {
               // look at exception's subClass tree for possible mappers
               jaxrsResponse = executeExceptionMapper(e, logger);
               if (jaxrsResponse == null)
               {
                  jaxrsResponse = handleWebApplicationException((WebApplicationException) e);
               }
            }
         } else if (e instanceof Failure)
         {
            // known exceptions that extend from Failure
            if (e instanceof WriterException)
            {
               jaxrsResponse = handleWriterException(request, (WriterException) e, logger);
            } else if (e instanceof ReaderException)
            {
               jaxrsResponse = handleReaderException(request, (ReaderException) e, logger);
            } else
            {
               jaxrsResponse = executeExceptionMapper(e, logger);
               if (jaxrsResponse == null)
               {
                  jaxrsResponse = handleFailure(request, (Failure) e);
               }
            }
         } else
         {
            if (e instanceof ApplicationException)
            {
               jaxrsResponse = handleApplicationException(request, (ApplicationException) e, logger);
            } else
            {
               jaxrsResponse = executeExceptionMapper(e, logger);
            }
         }
      }

      if (jaxrsResponse == null) {
         throw new UnhandledException(e);
      }
      return jaxrsResponse;
   }
}
