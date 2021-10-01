package org.jboss.resteasy.spi.metadata;

import static org.jboss.resteasy.spi.util.FindAnnotation.findAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.annotations.Body;
import org.jboss.resteasy.annotations.Form;
import org.jboss.resteasy.annotations.Query;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriBuilder;
import org.jboss.resteasy.spi.util.MethodHashing;
import org.jboss.resteasy.spi.util.PickConstructor;
import org.jboss.resteasy.spi.util.Types;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings(value = "unchecked")
public class ResourceBuilder
{
   public static class ResourceClassBuilder
   {
      final DefaultResourceClass resourceClass;

      List<FieldParameter> fields = new ArrayList<FieldParameter>();

      List<SetterParameter> setters = new ArrayList<SetterParameter>();

      List<ResourceMethod> resourceMethods = new ArrayList<ResourceMethod>();

      List<ResourceLocator> resourceLocators = new ArrayList<ResourceLocator>();

      public ResourceClassBuilder(final Class<?> root, final String path)
      {
         this.resourceClass = new DefaultResourceClass(root, path);
      }

      public ResourceMethodBuilder method(Method method)
      {
         return new ResourceMethodBuilder(this, method, method);
      }

      public ResourceMethodBuilder method(Method method, Method annotatedMethod)
      {
         return new ResourceMethodBuilder(this, method, annotatedMethod);
      }

      public ResourceLocatorBuilder locator(Method method)
      {
         return new ResourceLocatorBuilder(this, method, method);
      }

      public ResourceLocatorBuilder locator(Method method, Method annotatedMethod)
      {
         return new ResourceLocatorBuilder(this, method, annotatedMethod);
      }

      public FieldParameterBuilder field(Field field)
      {
         FieldParameter param = new FieldParameter(resourceClass, field);
         return new FieldParameterBuilder(this, param);
      }

      public SetterParameterBuilder setter(Method method)
      {
         SetterParameter param = new SetterParameter(resourceClass, method, method);
         return new SetterParameterBuilder(this, param);
      }

      public ResourceConstructorBuilder constructor(Constructor constructor)
      {
         return new ResourceConstructorBuilder(this, constructor);
      }

      public ResourceClass buildClass()
      {
         resourceClass.fields = fields.toArray(new FieldParameter[fields.size()]);
         resourceClass.setters = setters.toArray(new SetterParameter[setters.size()]);
         resourceClass.resourceMethods = resourceMethods.toArray(new ResourceMethod[resourceMethods.size()]);
         resourceClass.resourceLocators = resourceLocators.toArray(new ResourceLocator[resourceLocators.size()]);

         return resourceClass;
      }
   }

   public static class ParameterBuilder<T extends ParameterBuilder<T>>
   {
      final Parameter parameter;

      public ParameterBuilder(final Parameter parameter)
      {
         this.parameter = parameter;
      }

      public Parameter getParameter() {
         return parameter;
      }

      public T type(Class<?> type)
      {
         parameter.type = type;
         return (T) this;
      }

      public T genericType(Type type)
      {
         parameter.genericType = type;
         return (T) this;
      }

      public T type(GenericType type)
      {
         parameter.type = type.getRawType();
         parameter.genericType = type.getType();
         return (T) this;
      }

      public T beanParam()
      {
         parameter.paramType = Parameter.ParamType.BEAN_PARAM;
         return (T) this;
      }

      public T context()
      {
         parameter.paramType = Parameter.ParamType.CONTEXT;
         return (T) this;
      }

      public T messageBody()
      {
         parameter.paramType = Parameter.ParamType.MESSAGE_BODY;
         return (T) this;
      }

      public T encoded()
      {
         parameter.encoded = true;
         return (T) this;
      }

      public T defaultValue(String defaultValue)
      {
         parameter.defaultValue = defaultValue;
         return (T) this;
      }

      public T cookieParam(String name)
      {
         parameter.paramType = Parameter.ParamType.COOKIE_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      public T formParam(String name)
      {
         parameter.paramType = Parameter.ParamType.FORM_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      /**
       * Resteasy @Form specific injection parameter.
       *
       * @param prefix prefix
       * @return parameter builder
       */
      public T form(String prefix)
      {
         parameter.paramType = Parameter.ParamType.FORM;
         parameter.paramName = prefix;
         return (T) this;
      }

      /**
       * Resteasy @Form specific injection parameter.
       *
       * @return parameter builder
       */
      public T form()
      {
         parameter.paramType = Parameter.ParamType.FORM;
         parameter.paramName = "";
         return (T) this;
      }

      public T headerParam(String name)
      {
         parameter.paramType = Parameter.ParamType.HEADER_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      public T matrixParam(String name)
      {
         parameter.paramType = Parameter.ParamType.MATRIX_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      public T pathParam(String name)
      {
         parameter.paramType = Parameter.ParamType.PATH_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      public T queryParam(String name)
      {
         parameter.paramType = Parameter.ParamType.QUERY_PARAM;
         parameter.paramName = name;
         return (T) this;
      }

      @SuppressWarnings("deprecation")
      public T fromAnnotations()
      {
         Annotation[] annotations = parameter.getAnnotations();
         AccessibleObject injectTarget = parameter.getAccessibleObject();
         Class<?> type = parameter.getResourceClass().getClazz();

         parameter.encoded = findAnnotation(annotations, Encoded.class) != null
               || injectTarget.isAnnotationPresent(Encoded.class) || type.isAnnotationPresent(Encoded.class);
         DefaultValue defaultValue = findAnnotation(annotations, DefaultValue.class);
         if (defaultValue != null)
            parameter.defaultValue = defaultValue.value();

         QueryParam queryParam;
         org.jboss.resteasy.annotations.jaxrs.QueryParam queryParam2;
         Query query;
         HeaderParam header;
         org.jboss.resteasy.annotations.jaxrs.HeaderParam header2;
         MatrixParam matrix;
         org.jboss.resteasy.annotations.jaxrs.MatrixParam matrix2;
         PathParam uriParam;
         org.jboss.resteasy.annotations.jaxrs.PathParam uriParam2;
         CookieParam cookie;
         org.jboss.resteasy.annotations.jaxrs.CookieParam cookie2;
         FormParam formParam;
         org.jboss.resteasy.annotations.jaxrs.FormParam formParam2;
         Form form;
         Suspended suspended;

         if ((queryParam = findAnnotation(annotations, QueryParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.QUERY_PARAM;
            parameter.paramName = queryParam.value();
         }
         else if ((queryParam2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.QueryParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.QUERY_PARAM;
            if (queryParam2.value() != null && queryParam2.value().length() > 0)
            {
               parameter.paramName = queryParam2.value();
            }
         }
         else if ((query = findAnnotation(annotations, Query.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.QUERY;
            parameter.paramName = ""; // TODO query.prefix();
         }
         else if ((header = findAnnotation(annotations, HeaderParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.HEADER_PARAM;
            parameter.paramName = header.value();
         }
         else if ((header2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.HeaderParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.HEADER_PARAM;
            if (header2.value() != null && header2.value().length() > 0)
            {
               parameter.paramName = header2.value();
            }
         }
         else if ((formParam = findAnnotation(annotations, FormParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.FORM_PARAM;
            parameter.paramName = formParam.value();
         }
         else if ((formParam2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.FormParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.FORM_PARAM;
            if (formParam2.value() != null && formParam2.value().length() > 0)
            {
               parameter.paramName = formParam2.value();
            }
         }
         else if ((cookie = findAnnotation(annotations, CookieParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.COOKIE_PARAM;
            parameter.paramName = cookie.value();
         }
         else if ((cookie2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.CookieParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.COOKIE_PARAM;
            if (cookie2.value() != null && cookie2.value().length() > 0)
            {
               parameter.paramName = cookie2.value();
            }
         }
         else if ((uriParam = findAnnotation(annotations, PathParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.PATH_PARAM;
            parameter.paramName = uriParam.value();
         }
         else if ((uriParam2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.PathParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.PATH_PARAM;
            if (uriParam2.value() != null && uriParam2.value().length() > 0)
            {
               parameter.paramName = uriParam2.value();
            }
         }
         else if ((form = findAnnotation(annotations, Form.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.FORM;
            parameter.paramName = form.prefix();
         }
         else if (findAnnotation(annotations, BeanParam.class) != null)
         {
            parameter.paramType = Parameter.ParamType.BEAN_PARAM;
         }
         else if ((matrix = findAnnotation(annotations, MatrixParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.MATRIX_PARAM;
            parameter.paramName = matrix.value();
         }
         else if ((matrix2 = findAnnotation(annotations,
               org.jboss.resteasy.annotations.jaxrs.MatrixParam.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.MATRIX_PARAM;
            if (matrix2.value() != null && matrix2.value().length() > 0)
            {
               parameter.paramName = matrix2.value();
            }
         }
         else if (findAnnotation(annotations, Context.class) != null)
         {
            parameter.paramType = Parameter.ParamType.CONTEXT;
         }
         else if ((suspended = findAnnotation(annotations, Suspended.class)) != null)
         {
            parameter.paramType = Parameter.ParamType.SUSPENDED;
         }
         else if (javax.ws.rs.container.AsyncResponse.class.isAssignableFrom(type))
         {
            parameter.paramType = Parameter.ParamType.SUSPENDED;
         }
         else if (findAnnotation(annotations, Body.class) != null)
         {
            parameter.paramType = Parameter.ParamType.MESSAGE_BODY;
         }
         else
         {
            parameter.paramType = Parameter.ParamType.UNKNOWN;
         }
         return (T) this;
      }
   }

   public static class ConstructorParameterBuilder extends ParameterBuilder<ConstructorParameterBuilder>
   {
      final ResourceConstructorBuilder constructor;

      final ConstructorParameter param;

      public ConstructorParameterBuilder(final ResourceConstructorBuilder builder, final ConstructorParameter param)
      {
         super(param);
         this.constructor = builder;
         this.param = param;
      }

      public ConstructorParameterBuilder param(int i)
      {
         return constructor.param(i);
      }

      public ResourceClassBuilder buildConstructor()
      {
         return constructor.buildConstructor();
      }

   }

   public static class LocatorMethodParameterBuilder<T extends LocatorMethodParameterBuilder<T>>
         extends
            ParameterBuilder<T>
   {
      final ResourceLocatorBuilder locator;

      final MethodParameter param;

      public LocatorMethodParameterBuilder(final ResourceLocatorBuilder method, final MethodParameter param)
      {
         super(param);
         this.locator = method;
         this.param = param;
      }

      public T param(int i)
      {
         return (T) locator.param(i);
      }

      public ResourceClassBuilder buildMethod()
      {
         return locator.buildMethod();
      }

   }

   public static class ResourceMethodParameterBuilder
         extends
            LocatorMethodParameterBuilder<ResourceMethodParameterBuilder>
   {
      final ResourceMethodBuilder method;

      public ResourceMethodParameterBuilder(final ResourceMethodBuilder method, final MethodParameter param)
      {
         super(method, param);
         this.method = method;
      }

      public ResourceMethodParameterBuilder suspended()
      {
         method.method.asynchronous = true;
         parameter.paramType = Parameter.ParamType.SUSPENDED;
         return this;
      }

      @Override
      public ResourceMethodParameterBuilder fromAnnotations()
      {
         doFromAnnotations();
         if (param.paramType == Parameter.ParamType.SUSPENDED)
         {
            method.method.asynchronous = true;
         }
         else if (param.paramType == Parameter.ParamType.UNKNOWN)
         {
            param.paramType = Parameter.ParamType.MESSAGE_BODY;
         }
         return this;
      }

      protected void doFromAnnotations() {
         super.fromAnnotations();
      }
   }

   public static class ResourceConstructorBuilder
   {

      ResourceConstructor constructor;

      ResourceClassBuilder resourceClassBuilder;

      public ResourceConstructorBuilder(final ResourceClassBuilder resourceClassBuilder, final Constructor constructor)
      {
         this.resourceClassBuilder = resourceClassBuilder;
         this.constructor = new DefaultResourceConstructor(resourceClassBuilder.resourceClass, constructor);
      }

      public ConstructorParameterBuilder param(int i)
      {
         return new ConstructorParameterBuilder(this, constructor.getParams()[i]);
      }

      public ResourceClassBuilder buildConstructor()
      {
         resourceClassBuilder.resourceClass.constructor = constructor;
         return resourceClassBuilder;
      }
   }

   public static class ResourceLocatorBuilder<T extends ResourceLocatorBuilder<T>>
   {

      DefaultResourceLocator locator;

      ResourceClassBuilder resourceClassBuilder;

      ResourceLocatorBuilder()
      {
      }

      public ResourceLocatorBuilder(final ResourceClassBuilder resourceClassBuilder, final Method method, final Method annotatedMethod)
      {
         this.resourceClassBuilder = resourceClassBuilder;
         this.locator = new DefaultResourceLocator(resourceClassBuilder.resourceClass, method, annotatedMethod);
      }

      public DefaultResourceLocator getLocator() {
         return locator;
      }

      public T returnType(Class<?> type)
      {
         locator.returnType = type;
         return (T) this;
      }

      public T genericReturnType(Type type)
      {
         locator.genericReturnType = type;
         return (T) this;
      }

      public T returnType(GenericType type)
      {
         locator.returnType = type.getRawType();
         locator.genericReturnType = type.getType();
         return (T) this;
      }

      public LocatorMethodParameterBuilder param(int i)
      {
         return new LocatorMethodParameterBuilder(this, locator.getParams()[i]);
      }

      public ResourceClassBuilder buildMethod()
      {
         ResteasyUriBuilder builder = (ResteasyUriBuilder) RuntimeDelegate.getInstance().createUriBuilder();
         if (locator.resourceClass.getPath() != null)
            builder.path(locator.resourceClass.getPath());
         if (locator.path != null)
            builder.path(locator.path);
         String pathExpression = builder.getPath();
         if (pathExpression == null)
            pathExpression = "";
         locator.fullpath = pathExpression;
         if (locator.resourceClass.getClazz().isAnonymousClass())
         {
            locator.getMethod().setAccessible(true);
         }
         resourceClassBuilder.resourceLocators.add(locator);
         return resourceClassBuilder;
      }

      public T path(String path)
      {
         locator.path = path;
         return (T) this;
      }
   }

   public static class ResourceMethodBuilder extends ResourceLocatorBuilder<ResourceMethodBuilder>
   {
      DefaultResourceMethod method;

      public ResourceMethodBuilder(final ResourceClassBuilder resourceClassBuilder, final Method method, final Method annotatedMethod)
      {
         this.method = new DefaultResourceMethod(resourceClassBuilder.resourceClass, method, annotatedMethod);
         this.locator = this.method;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceMethodBuilder httpMethod(String httpMethod)
      {
         method.httpMethods.add(httpMethod.toUpperCase());
         return this;
      }

      public ResourceMethodBuilder get()
      {
         method.httpMethods.add(HttpMethod.GET);
         return this;
      }

      public ResourceMethodBuilder put()
      {
         method.httpMethods.add(HttpMethod.PUT);
         return this;
      }

      public ResourceMethodBuilder post()
      {
         method.httpMethods.add(HttpMethod.POST);
         return this;
      }

      public ResourceMethodBuilder delete()
      {
         method.httpMethods.add(HttpMethod.DELETE);
         return this;
      }

      public ResourceMethodBuilder options()
      {
         method.httpMethods.add(HttpMethod.OPTIONS);
         return this;
      }

      public ResourceMethodBuilder head()
      {
         method.httpMethods.add(HttpMethod.HEAD);
         return this;
      }

      public ResourceMethodBuilder produces(MediaType... produces)
      {
         method.produces = produces;
         return this;
      }

      public ResourceMethodBuilder produces(String... produces)
      {
         MediaType[] types = parseMediaTypes(produces);
         method.produces = types;
         for (MediaType mt : types)
         {
            if (!mt.getParameters().containsKey(MediaType.CHARSET_PARAMETER))
            {
               if (isTextLike(mt))
               {
                  ResteasyDeployment deployment = ResteasyProviderFactory.getInstance().getContextData(ResteasyDeployment.class);
                  if (deployment != null && !deployment.isAddCharset())
                  {
                     LogMessages.LOGGER.mediaTypeLacksCharset(mt, method.getMethod().getName());
                  }
               }
            }
         }
         return this;
      }

      private static boolean isTextLike(MediaType mediaType)
      {
         return "text".equalsIgnoreCase(mediaType.getType()) || ("application".equalsIgnoreCase(mediaType.getType())
               && mediaType.getSubtype().toLowerCase().startsWith("xml"));
      }

      protected MediaType[] parseMediaTypes(String[] produces)
      {
         List<MediaType> mediaTypes = new ArrayList<MediaType>();
         for (String produce : produces)
         {
            String[] split = produce.split(",");
            for (String s : split)
               mediaTypes.add(MediaType.valueOf(s));
         }
         MediaType[] types = new MediaType[mediaTypes.size()];
         types = mediaTypes.toArray(types);
         return types;
      }

      public ResourceMethodBuilder consumes(MediaType... consumes)
      {
         method.consumes = consumes;
         return this;
      }

      public ResourceMethodBuilder consumes(String... consumes)
      {
         MediaType[] types = parseMediaTypes(consumes);
         method.consumes = types;
         return this;
      }

      public ResourceMethodParameterBuilder param(int i)
      {
         return new ResourceMethodParameterBuilder(this, locator.getParams()[i]);
      }

      public ResourceClassBuilder buildMethod()
      {
         ResteasyUriBuilder builder = (ResteasyUriBuilder) RuntimeDelegate.getInstance().createUriBuilder();
         if (method.resourceClass.getPath() != null)
            builder.path(method.resourceClass.getPath());
         if (method.path != null)
            builder.path(method.path);
         String pathExpression = builder.getPath();
         if (pathExpression == null)
            pathExpression = "";
         method.fullpath = pathExpression;
         if (method.resourceClass.getClazz().isAnonymousClass())
         {
            method.getMethod().setAccessible(true);
         }
         resourceClassBuilder.resourceMethods.add(method);
         return resourceClassBuilder;
      }

      public DefaultResourceMethod getMethod() {
         return method;
      }
   }

   public static class FieldParameterBuilder extends ParameterBuilder<FieldParameterBuilder>
   {
      FieldParameter field;

      ResourceClassBuilder resourceClassBuilder;

      FieldParameterBuilder(final ResourceClassBuilder resourceClassBuilder, final FieldParameter parameter)
      {
         super(parameter);
         this.field = parameter;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceClassBuilder buildField()
      {
         field.field.setAccessible(true);
         resourceClassBuilder.fields.add(field);
         return resourceClassBuilder;
      }
   }

   public static class SetterParameterBuilder extends ParameterBuilder<SetterParameterBuilder>
   {
      SetterParameter setter;

      ResourceClassBuilder resourceClassBuilder;

      SetterParameterBuilder(final ResourceClassBuilder resourceClassBuilder, final SetterParameter parameter)
      {
         super(parameter);
         this.setter = parameter;
         this.resourceClassBuilder = resourceClassBuilder;
      }

      public ResourceClassBuilder buildSetter()
      {
         setter.setter.setAccessible(true);
         resourceClassBuilder.setters.add(setter);
         return resourceClassBuilder;
      }
   }

   private final Map<Integer, List<ResourceClassProcessor>> processors = new TreeMap<>(Comparator.reverseOrder());

   /**
    * Register a new {@link ResourceClassProcessor} which will be used to post-process all
    * {@link ResourceClass} instances created from the builder.
    * @param processor resource class processor
    * @param priority processor priority
    */
   public void registerResourceClassProcessor(ResourceClassProcessor processor, int priority)
   {
      List<ResourceClassProcessor> l = processors.get(priority);
      if (l == null)
      {
         l = new LinkedList<ResourceClassProcessor>();
         processors.put(priority, l);
      }
      l.add(processor);
   }

   @Deprecated
   public static ResourceClassBuilder rootResource(Class<?> root)
   {
      return new ResourceBuilder().buildRootResource(root);
   }

   public ResourceClassBuilder buildRootResource(Class<?> root)
   {
      return new ResourceClassBuilder(root, "/");
   }

   @Deprecated
   public static ResourceClassBuilder rootResource(Class<?> root, String path)
   {
      return new ResourceBuilder().buildRootResource(root, path);
   }

   protected ResourceClassBuilder buildRootResource(Class<?> root, String path)
   {
      return new ResourceClassBuilder(root, path);
   }

   @Deprecated
   public static ResourceClassBuilder locator(Class<?> root)
   {
      return new ResourceBuilder().buildLocator(root);
   }

   protected ResourceClassBuilder buildLocator(Class<?> root)
   {
      return new ResourceClassBuilder(root, null);
   }

   @Deprecated
   public static ResourceConstructor constructor(Class<?> annotatedResourceClass)
   {
      return new ResourceBuilder().getConstructor(annotatedResourceClass);
   }

   public Class<? extends Annotation> getCorrespondingRootAnnotation() {
      return Path.class;
   }

   /**
    * Picks a constructor from an annotated resource class based on spec rules.
    *
    * @param annotatedResourceClass annotated resource class
    * @return {@link ResourceConstructor}
    */
   public ResourceConstructor getConstructor(Class<?> annotatedResourceClass)
   {
      Constructor constructor = PickConstructor.pickPerRequestConstructor(annotatedResourceClass);
      // Liberty change start
      if (constructor == null) {
          constructor = checkForAtInjectConstructor(annotatedResourceClass);
      }
      // Liberty change end
      if (constructor == null)
      {
         throw new RuntimeException(Messages.MESSAGES.couldNotFindConstructor(annotatedResourceClass.getName()));
      }
      ResourceConstructorBuilder builder = buildRootResource(annotatedResourceClass).constructor(constructor);
      if (constructor.getParameterTypes() != null)
      {
         for (int i = 0; i < constructor.getParameterTypes().length; i++)
            builder.param(i).fromAnnotations();
      }
      ResourceClass resourceClass = applyProcessors(builder.buildConstructor().buildClass());
      return resourceClass.getConstructor();
   }

   // Liberty change start
   private Constructor<?> checkForAtInjectConstructor(Class<?> resourceClass) {
       Constructor<?> ctor = null;
       int numParams = -1;
       for (Constructor<?> c : resourceClass.getConstructors()) {
           if (c.isAnnotationPresent(Inject.class)) {
               int paramCount = c.getParameterCount();
               if (paramCount > numParams) {
                   ctor = c;
                   numParams = paramCount;
               }
           }
       }
       return ctor;
   }
   // Liberty change end

   @Deprecated
   public static ResourceClass rootResourceFromAnnotations(Class<?> clazz)
   {
      return new ResourceBuilder().getRootResourceFromAnnotations(clazz);
   }

   /**
    * Build metadata from annotations on classes and methods.
    *
    * @param clazz class
    * @return resource class
    */
   public ResourceClass getRootResourceFromAnnotations(Class<?> clazz)
   {
      return fromAnnotations(false, clazz);
   }

   @Deprecated
   public static ResourceClass locatorFromAnnotations(Class<?> clazz)
   {
      return new ResourceBuilder().getLocatorFromAnnotations(clazz);
   }

   public ResourceClass getLocatorFromAnnotations(Class<?> clazz)
   {
      return fromAnnotations(true, clazz);
   }

   private ResourceClass fromAnnotations(boolean isLocator, Class<?> clazz)
   {
      // stupid hack for Weld as it loses generic type information, but retains annotations.
      if (!clazz.isInterface() && clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Object.class)
            && clazz.isSynthetic())
      {
         clazz = clazz.getSuperclass();
      }

      ResourceClassBuilder builder = null;
      if (isLocator)
         builder = buildLocator(clazz);
      else
      {
         builder = createResourceClassBuilder(clazz);
      }
      for (Method method : clazz.getMethods())
      {
         if (!method.isSynthetic() && !method.getDeclaringClass().equals(Object.class))
            processMethod(isLocator, builder, clazz, method);

      }
      if (!clazz.isInterface())
      {
         processFields(builder, clazz);
      }
      processSetters(builder, clazz);
      return applyProcessors(builder.buildClass());
   }

   protected ResourceClassBuilder createResourceClassBuilder(Class<?> clazz) {
      ResourceClassBuilder builder;
      Path path = clazz.getAnnotation(Path.class);
      if (path == null)
         builder = buildRootResource(clazz, null);
      else
         builder = buildRootResource(clazz, path.value());
      return builder;
   }

   private static Set<String> getHttpMethods(Method method)
   {
      HashSet<String> methods = new HashSet<String>();
      for (Annotation annotation : method.getAnnotations())
      {
         HttpMethod http = annotation.annotationType().getAnnotation(HttpMethod.class);
         if (http != null)
            methods.add(http.value());
      }
      if (methods.size() == 0)
         return null;
      return methods;
   }

   private static boolean isHttpMethod(Method method)
   {
      for (Annotation annotation : method.getAnnotations())
      {
         HttpMethod http = annotation.annotationType().getAnnotation(HttpMethod.class);
         if (http != null)
            return true;
      }
      return false;
   }

   @Deprecated
   public static Method findAnnotatedMethod(final Class<?> root, final Method implementation)
   {
      return new ResourceBuilder().getAnnotatedMethod(root, implementation);
   }

   /**
    * Find the annotated resource method or sub-resource method / sub-resource locator in the class hierarchy.
    *
    * @param root The root resource class.
    * @param implementation The resource method or sub-resource method / sub-resource locator implementation
    * @return The annotated resource method or sub-resource method / sub-resource locator.
    */
   public Method getAnnotatedMethod(final Class<?> root, final Method implementation)
   {
      if (implementation.isSynthetic())
      {
         return null;
      }

      // Check the method itself for JAX-RS annotations
      if (implementation.isAnnotationPresent(Path.class) || isHttpMethod(implementation))
      {
         return implementation;
      }

      // Per http://download.oracle.com/auth/otn-pub/jcp/jaxrs-1.0-fr-oth-JSpec/jaxrs-1.0-final-spec.pdf
      // Section 3.2 Annotation Inheritance

      if (implementation.isAnnotationPresent(Produces.class) || implementation.isAnnotationPresent(Consumes.class))
      {
         // Abort the search for inherited annotations as specified by the JAX-RS specification.
         // If a implementation method has any JAX-RS annotations then all the annotations
         // on the superclass or interface method are ignored.
         // Therefore a method can be omitted if it is neither a resource method nor a sub-resource method /
         // sub-resource locator but is annotated with other JAX-RS annotations.
         return null;
      }

      // Check super-classes for inherited annotations
      for (Class<?> clazz = implementation.getDeclaringClass().getSuperclass(); clazz != null; clazz = clazz
            .getSuperclass())
      {
         final Method overriddenMethod = Types.findOverriddenMethod(implementation.getDeclaringClass(), clazz,
               implementation);
         if (overriddenMethod == null)
         {
            continue;
         }

         if (overriddenMethod.isAnnotationPresent(Path.class) || isHttpMethod(overriddenMethod))
         {
            return overriddenMethod;
         }
         if (overriddenMethod.isAnnotationPresent(Produces.class)
               || overriddenMethod.isAnnotationPresent(Consumes.class))
         {
            // Abort the search for inherited annotations as specified by the JAX-RS specification.
            // If a implementation method has any JAX-RS annotations then all the annotations
            // on the superclass or interface method are ignored.
            // Therefore a method can be omitted if it is neither a resource method nor a sub-resource method /
            // sub-resource locator but is annotated with other JAX-RS annotations.
            return null;
         }
      }

      // Check implemented interfaces for inherited annotations
      for (Class<?> clazz = root; clazz != null; clazz = clazz.getSuperclass())
      {
         Method overriddenMethod = null;

         for (Class<?> classInterface : clazz.getInterfaces())
         {
            final Method overriddenInterfaceMethod = Types.getImplementedInterfaceMethod(root, classInterface,
                  implementation);
            if (overriddenInterfaceMethod == null)
            {
               continue;
            }
            if (!overriddenInterfaceMethod.isAnnotationPresent(Path.class)
                  && !isHttpMethod(overriddenInterfaceMethod))
            {
               if (overriddenInterfaceMethod.isAnnotationPresent(Produces.class)
                     || overriddenInterfaceMethod.isAnnotationPresent(Consumes.class))
               {
                  // Abort the search for inherited annotations as specified by the JAX-RS specification.
                  // If a implementation method has any JAX-RS annotations then all the annotations
                  // on the superclass or interface method are ignored.
                  // Therefore a method can be omitted if it is neither a resource method nor a sub-resource method /
                  // sub-resource locator but is annotated with other JAX-RS annotations.
                  return null;
               }
               else
               {
                  continue;
               }
            }
            // Ensure no redefinition by peer interfaces (ambiguous) to preserve logic found in
            // original implementation
            if (overriddenMethod != null && !overriddenInterfaceMethod.equals(overriddenMethod))
            {
               throw new RuntimeException(Messages.MESSAGES.ambiguousInheritedAnnotations(implementation));
            }

            overriddenMethod = overriddenInterfaceMethod;
         }

         if (overriddenMethod != null)
         {
            return overriddenMethod;
         }
      }

      return null;
   }

   protected void processFields(ResourceClassBuilder resourceClassBuilder, Class<?> root)
   {
      do
      {
         processDeclaredFields(resourceClassBuilder, root);
         root = root.getSuperclass();
         //      } while (root.getSuperclass() != null && !root.getSuperclass().equals(Object.class));
      }
      while (root != null && !root.equals(Object.class));
   }

   protected void processSetters(ResourceClassBuilder resourceClassBuilder, Class<?> root)
   {
      HashSet<Long> hashes = new HashSet<Long>();
      do
      {
         processDeclaredSetters(resourceClassBuilder, root, hashes);
         root = root.getSuperclass();
      }
      while (root != null && !root.equals(Object.class));
   }

   protected void processDeclaredFields(ResourceClassBuilder resourceClassBuilder, final Class<?> root)
   {
      Field[] fieldList = new Field[0];
      try
      {
         if (System.getSecurityManager() == null)
         {
            fieldList = root.getDeclaredFields();
         }
         else
         {
            fieldList = AccessController.doPrivileged(new PrivilegedExceptionAction<Field[]>()
            {
               @Override
               public Field[] run() throws Exception
               {
                  return root.getDeclaredFields();
               }
            });
         }
      }
      catch (PrivilegedActionException pae)
      {

      }

      for (Field field : fieldList)
      {
         FieldParameterBuilder builder = resourceClassBuilder.field(field).fromAnnotations();
         if (builder.field.paramType == Parameter.ParamType.MESSAGE_BODY && !field.isAnnotationPresent(Body.class))
            continue;
         if (builder.field.paramType == Parameter.ParamType.UNKNOWN)
            continue;
         builder.buildField();
      }
   }

   protected void processDeclaredSetters(ResourceClassBuilder resourceClassBuilder, final Class<?> root,
         Set<Long> visitedHashes)
   {
      Method[] methodList = new Method[0];
      try
      {
         if (System.getSecurityManager() == null)
         {
            methodList = root.getDeclaredMethods();
         }
         else
         {
            methodList = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>()
            {
               @Override
               public Method[] run() throws Exception
               {
                  return root.getDeclaredMethods();
               }
            });
         }
      }
      catch (PrivilegedActionException pae)
      {

      }

      for (Method method : methodList)
      {
         if (!method.getName().startsWith("set"))
            continue;
         if (method.getParameterTypes().length != 1)
            continue;
         long hash = 0;
         try
         {
            hash = MethodHashing.methodHash(method);
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
         if (!Modifier.isPrivate(method.getModifiers()) && visitedHashes.contains(hash))
            continue;
         visitedHashes.add(hash);
         SetterParameterBuilder builder = resourceClassBuilder.setter(method).fromAnnotations();
         if (builder.setter.paramType == Parameter.ParamType.MESSAGE_BODY && !method.isAnnotationPresent(Body.class))
            continue;
         if (builder.setter.paramType == Parameter.ParamType.UNKNOWN)
            continue;
         builder.buildSetter();
      }
   }

   protected void processMethod(boolean isLocator, ResourceClassBuilder resourceClassBuilder, Class<?> root,
         Method implementation)
   {
      Method method = getAnnotatedMethod(root, implementation);
      if (method != null)
      {
         Set<String> httpMethods = getHttpMethods(method);

         ResourceLocatorBuilder resourceLocatorBuilder;

         if (httpMethods == null)
         {
            resourceLocatorBuilder = resourceClassBuilder.locator(implementation, method);
         }
         else
         {
            ResourceMethodBuilder resourceMethodBuilder = resourceClassBuilder.method(implementation, method);
            resourceLocatorBuilder = resourceMethodBuilder;

            for (String httpMethod : httpMethods)
            {
               if (httpMethod.equalsIgnoreCase(HttpMethod.GET))
                  resourceMethodBuilder.get();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.PUT))
                  resourceMethodBuilder.put();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.POST))
                  resourceMethodBuilder.post();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.DELETE))
                  resourceMethodBuilder.delete();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.OPTIONS))
                  resourceMethodBuilder.options();
               else if (httpMethod.equalsIgnoreCase(HttpMethod.HEAD))
                  resourceMethodBuilder.head();
               else
                  resourceMethodBuilder.httpMethod(httpMethod);
            }
            Produces produces = method.getAnnotation(Produces.class);
            if (produces == null)
               produces = resourceClassBuilder.resourceClass.getClazz().getAnnotation(Produces.class);
            if (produces == null)
               produces = method.getDeclaringClass().getAnnotation(Produces.class);
            if (produces != null)
               resourceMethodBuilder.produces(produces.value());

            Consumes consumes = method.getAnnotation(Consumes.class);
            if (consumes == null)
               consumes = resourceClassBuilder.resourceClass.getClazz().getAnnotation(Consumes.class);
            if (consumes == null)
               consumes = method.getDeclaringClass().getAnnotation(Consumes.class);
            if (consumes != null)
               resourceMethodBuilder.consumes(consumes.value());
         }
         Path methodPath = method.getAnnotation(Path.class);
         if (methodPath != null)
            resourceLocatorBuilder.path(methodPath.value());
         for (int i = 0; i < resourceLocatorBuilder.locator.params.length; i++)
         {
            resourceLocatorBuilder.param(i).fromAnnotations();
         }
         resourceLocatorBuilder.buildMethod();
      }
   }

   /**
    * Apply the list of {@link ResourceClassProcessor} to the supplied {@link ResourceClass}.
    */
   private ResourceClass applyProcessors(ResourceClass original)
   {
      ResourceClass current = original;
      for (List<ResourceClassProcessor> l : processors.values())
      {
         for (ResourceClassProcessor processor : l)
         {
            current = processor.process(current);
            Objects.requireNonNull(current, "ResourceClassProcessor must not return null");
         }
      }
      return current;
   }

}
