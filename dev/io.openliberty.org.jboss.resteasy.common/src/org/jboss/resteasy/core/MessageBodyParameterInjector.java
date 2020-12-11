package org.jboss.resteasy.core;

import org.jboss.resteasy.core.interception.jaxrs.AbstractReaderInterceptorContext;
import org.jboss.resteasy.core.interception.jaxrs.ServerReaderInterceptorContext;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.MarshalledEntity;
import org.jboss.resteasy.spi.ReaderException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ValueInjector;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry.InterceptorFactory;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistryListener;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.InputStreamToByteArray;
import org.jboss.resteasy.util.ThreadLocalStack;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ReaderInterceptor;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("rawtypes")
public class MessageBodyParameterInjector implements ValueInjector, JaxrsInterceptorRegistryListener
{
   private static ThreadLocalStack<Object> bodyStack = new ThreadLocalStack<Object>();

   public static void pushBody(Object o)
   {
      bodyStack.push(o);
   }

   public static Object getBody()
   {
      return bodyStack.get();
   }

   public static Object popBody()
   {
      return bodyStack.pop();
   }

   public static int bodyCount()
   {
      return bodyStack.size();
   }

   public static void clearBodies()
   {
      bodyStack.clear();
   }

   private Class type;
   private Type genericType;
   private Annotation[] annotations;
   private ResteasyProviderFactory factory;
   private Class declaringClass;
   private AccessibleObject target;
   private ReaderInterceptor[] interceptors;
   private boolean isMarshalledEntity;

   public MessageBodyParameterInjector(final Class declaringClass, final AccessibleObject target, final Class type, final Type genericType, final Annotation[] annotations, final ResteasyProviderFactory factory)
   {
      this.factory = factory;
      this.target = target;
      this.declaringClass = declaringClass;

      if (type.equals(MarshalledEntity.class))
      {
         if (genericType == null || !(genericType instanceof ParameterizedType))
         {
            throw new RuntimeException(Messages.MESSAGES.marshalledEntityMustHaveTypeInfo());
         }
         isMarshalledEntity = true;
         ParameterizedType param = (ParameterizedType) genericType;
         this.genericType = param.getActualTypeArguments()[0];
         this.type = Types.getRawType(this.genericType);
      }
      else
      {
         this.type = type;
         this.genericType = genericType;
      }
      this.annotations = annotations;
      this.interceptors = this.factory
              .getServerReaderInterceptorRegistry().postMatch(
                      this.declaringClass, this.target);

      // this is for when an interceptor is added after the creation of the injector
      this.factory.getServerReaderInterceptorRegistry().getListeners().add(this);
   }

   @Override
   public void registryUpdated(JaxrsInterceptorRegistry registry, InterceptorFactory factory)
   {
      this.interceptors = this.factory
              .getServerReaderInterceptorRegistry().postMatch(
                      declaringClass, target);
   }

   protected ReaderInterceptor[] getReaderInterceptors() {
      return this.interceptors;
   }

   public boolean isFormData(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      if (mediaType.isWildcardType() || mediaType.isWildcardSubtype() ||
                 !mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) return false;
      if (!MultivaluedMap.class.isAssignableFrom(type)) return false;
      if (genericType == null) return true;

      if (!(genericType instanceof ParameterizedType)) return false;
      ParameterizedType params = (ParameterizedType) genericType;
      if (params.getActualTypeArguments().length != 2) return false;
      return params.getActualTypeArguments()[0].equals(String.class) && params.getActualTypeArguments()[1].equals(String.class);
   }


   @Override
   public Object inject(HttpRequest request, HttpResponse response, boolean unwrapAsync)
   {
      Object o = getBody();
      if (o != null)
      {
         return o;
      }
      MediaType mediaType = request.getHttpHeaders().getMediaType();
      if (mediaType == null)
      {
         mediaType = MediaType.WILDCARD_TYPE;
         //throw new BadRequestException("content-type was null and expecting to extract a body into " + this.target);
      }

      InputStream is = null;
      if (MediaType.APPLICATION_FORM_URLENCODED_TYPE.equals(mediaType))
      {
         /*if (request.formParametersRead()) //Liberty change - commenting out section
         {
            MultivaluedMap<String, String> map = request.getDecodedFormParameters();
            if (map != null)
            {
               StringBuilder sb = new StringBuilder();
               for (Entry<String, List<String>> entry : map.entrySet())
               {
                  String key = entry.getKey();
                  sb.append(key);
                  List<String> values = entry.getValue();
                  for (String value : values)
                  {
                     if (!("".equals(value)))
                     {
                        sb.append("=").append(value);
                     }
                     sb.append("&");
                  }
               }
               if (sb.length() > 0 && '&' == sb.charAt(sb.length() - 1))
               {
                  sb.deleteCharAt(sb.length() - 1);
               }
               String charset = "UTF-8";
               if (mediaType.getParameters().get("charset") != null)
               {
                  charset = mediaType.getParameters().get("charset");
               }
               try
               {
                  is = new ByteArrayInputStream(sb.toString().getBytes(charset));
               }
               catch (Exception e)
               {
                  LogMessages.LOGGER.charsetUnavailable(charset);
               }
            }
         }*/
      }

      try
      {
         if (is == null)
         {
            is = request.getInputStream();
         }
         if (isMarshalledEntity)
         {
            is = new InputStreamToByteArray(is);

         }
         AbstractReaderInterceptorContext messageBodyReaderContext = new ServerReaderInterceptorContext(getReaderInterceptors(), factory, type,
                 genericType, annotations, mediaType, request
                 .getMutableHeaders(), is, request);

         RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
         final long timestamp = tracingLogger.timestamp("RI_SUMMARY");

         final Object obj;

         try {
            obj = messageBodyReaderContext.proceed();
         } finally {
            tracingLogger.logDuration("RI_SUMMARY", timestamp, messageBodyReaderContext.getProcessedInterceptorCount());
         }

         if (isMarshalledEntity)
         {
            InputStreamToByteArray isba = (InputStreamToByteArray) is;
            final byte[] bytes = isba.toByteArray();
            return new MarshalledEntity()
            {
               @Override
               public byte[] getMarshalledBytes()
               {
                  return bytes;
               }

               @Override
               public Object getEntity()
               {
                  return obj;
               }
            };
         }
         else
         {
            return obj;
         }
      }
      catch (Exception e)
      {
         if (e instanceof ReaderException)
         {
            throw (ReaderException) e;
         }
         else
         {
            throw new ReaderException(e);
         }
      }
   }

   @Override
   public Object inject(boolean unwrapAsync)
   {
      throw new RuntimeException(Messages.MESSAGES.illegalToInjectMessageBody(this.target));
   }
}
