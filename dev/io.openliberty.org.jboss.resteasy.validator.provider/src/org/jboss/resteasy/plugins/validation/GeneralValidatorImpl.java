package org.jboss.resteasy.plugins.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Constraint;
import javax.validation.ConstraintDeclarationException;
import javax.validation.ConstraintDefinitionException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.GroupDefinitionException;
import javax.validation.MessageInterpolator;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.jboss.logging.Logger;
import org.jboss.resteasy.api.validation.ConstraintType;
import org.jboss.resteasy.api.validation.ConstraintType.Type;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ResteasyViolationException;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.plugins.validation.i18n.LogMessages;
import org.jboss.resteasy.plugins.validation.i18n.Messages;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.validation.GeneralValidatorCDI;
import org.jboss.resteasy.util.GetRestful;

import com.fasterxml.classmate.Filter;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright May 23, 2013
 */
public class GeneralValidatorImpl implements GeneralValidatorCDI
{
   public static final String SUPPRESS_VIOLATION_PATH = "resteasy.validation.suppress.path";
   private static final Logger log = Logger.getLogger(GeneralValidatorImpl.class.getName());

   /**
    * Used for resolving type parameters. Thread-safe.
    */
   private TypeResolver typeResolver = new TypeResolver();
   private ValidatorFactory validatorFactory;
   private boolean isExecutableValidationEnabled;
   private ExecutableType[] defaultValidatedExecutableTypes;
   private boolean suppressPath;
   private boolean cdiActive;
   private static ConstraintTypeUtilImpl util = new ConstraintTypeUtilImpl();

   public GeneralValidatorImpl(final ValidatorFactory validatorFactory, final boolean isExecutableValidationEnabled, final Set<ExecutableType> defaultValidatedExecutableTypes)
   {
      this.validatorFactory = validatorFactory;
      this.isExecutableValidationEnabled = isExecutableValidationEnabled;
      this.defaultValidatedExecutableTypes = defaultValidatedExecutableTypes.toArray(new ExecutableType[]{});
      
      try
      {
         cdiActive = ResteasyCdiExtension.isCDIActive();
         LogMessages.LOGGER.debug(Messages.MESSAGES.resteasyCdiExtensionOnClasspath());
      }
      catch (Throwable t)
      {
         // In case ResteasyCdiExtension is not on the classpath.
         LogMessages.LOGGER.debug(Messages.MESSAGES.resteasyCdiExtensionNotOnClasspath());
      }

      ResteasyConfiguration context = ResteasyContext.getContextData(ResteasyConfiguration.class);
      if (context != null)
      {
         String s = context.getParameter(SUPPRESS_VIOLATION_PATH);
         if (s != null)
         {
            suppressPath = Boolean.parseBoolean(s);
         }
      }
   }

   @Override
   public void validate(HttpRequest request, Object object, Class<?>... groups)
   {
      Validator validator = getValidator(request);
      Set<ConstraintViolation<Object>> cvs = null;
      SimpleViolationsContainer violationsContainer = getViolationsContainer(request, object);
      if (alreadyFoundClassOrPropertyConstraint(violationsContainer))
      {
         return;
      }

      try
      {
         cvs = validator.validate(object, groups);
      }
      catch (Exception e)
      {
         violationsContainer.setException(e);
         violationsContainer.setFieldsValidated(true);
         throw toValidationException(e, violationsContainer);
      }

      violationsContainer.addViolations(cvs);
      violationsContainer.setFieldsValidated(true);
   }

   private ValidationException toValidationException(Exception exception, SimpleViolationsContainer simpleViolationsContainer)
   {
      if (exception instanceof ConstraintDeclarationException ||
         exception instanceof ConstraintDefinitionException  ||
         exception instanceof GroupDefinitionException)
      {
         return (ValidationException) exception;
      }
      return new ResteasyViolationExceptionImpl(simpleViolationsContainer);
   }

   @Override
   public void checkViolations(HttpRequest request)
   {
      // Called from resteasy-jaxrs only if two argument version of isValidatable() returns true.
      SimpleViolationsContainer violationsContainer = getViolationsContainer(request, null);
      Object target = violationsContainer.getTarget();
      if (target != null && violationsContainer.isFieldsValidated())
      {
         if (violationsContainer != null && violationsContainer.size() > 0)
         {
             ResteasyViolationExceptionImpl ViolationException = new ResteasyViolationExceptionImpl(violationsContainer, request.getHttpHeaders().getAcceptableMediaTypes());  //Liberty change
             log.warn(ViolationException.getLocalizedMessage());  //Liberty change
             throw ViolationException;  //Liberty change
         }
      }
   }

   @Override
   public void checkViolationsfromCDI(HttpRequest request)
   {
      if (request == null)
      {
         return;
      }

      SimpleViolationsContainer violationsContainer = SimpleViolationsContainer.class.cast(request.getAttribute(SimpleViolationsContainer.class.getName()));
      if (violationsContainer != null && violationsContainer.size() > 0)
      {
          ResteasyViolationExceptionImpl ViolationException = new ResteasyViolationExceptionImpl(violationsContainer, request.getHttpHeaders().getAcceptableMediaTypes());  //Liberty change
          log.warn(ViolationException.getLocalizedMessage());  //Liberty change
          throw ViolationException;  //Liberty change
      }
   }

   @Override
   public void validateAllParameters(HttpRequest request, Object object, Method method, Object[] parameterValues, Class<?>... groups)
   {
      if (method.getParameterTypes().length == 0)
      {
         checkViolations(request);
         return;
      }

      Validator validator = getValidator(request);
      SimpleViolationsContainer violationsContainer = getViolationsContainer(request, object);

      Set<ConstraintViolation<Object>> cvs = null;

      try
      {
         cvs = validator.forExecutables().validateParameters(object, method, parameterValues, groups);
      }
      catch (Exception e)
      {
         violationsContainer.setException(e);
         throw toValidationException(e, violationsContainer);
      }
      violationsContainer.addViolations(cvs);      
      if ((violationsContainer.isFieldsValidated()
            || !GetRestful.isRootResource(object.getClass())
            || GetRestful.isSubResourceClass(object.getClass())
            || hasApplicationScope(object))  //Liberty change
         && violationsContainer.size() > 0)
      {   
          ResteasyViolationExceptionImpl ViolationException = new ResteasyViolationExceptionImpl(violationsContainer, request.getHttpHeaders().getAcceptableMediaTypes());  //Liberty change
          log.warn(ViolationException.getLocalizedMessage());  //Liberty change
          throw ViolationException;
      }
   }

   @Override
   public void validateReturnValue(HttpRequest request, Object object, Method method, Object returnValue, Class<?>... groups)
   {
      Validator validator = getValidator(request);
      SimpleViolationsContainer violationsContainer = getViolationsContainer(request, object);
      Set<ConstraintViolation<Object>> cvs = null;
      
      //Liberty change start
      Object myReturnValue = returnValue;
      if (returnValue instanceof org.jboss.resteasy.specimpl.BuiltResponse) 
      {
          org.jboss.resteasy.specimpl.BuiltResponse builtResponse = (org.jboss.resteasy.specimpl.BuiltResponse) returnValue;
          myReturnValue =  builtResponse.getEntity();          
      }
      //Liberty change end
      
      try
      {
         cvs = validator.forExecutables().validateReturnValue(object, method, myReturnValue, groups);  //Liberty change         
      }
      catch (Exception e)
      {
         violationsContainer.setException(e);
         throw toValidationException(e, violationsContainer);
      }
      violationsContainer.addViolations(cvs);
      if (violationsContainer.size() > 0)
      {
          ResteasyViolationExceptionImpl ViolationException = new ResteasyViolationExceptionImpl(violationsContainer, request.getHttpHeaders().getAcceptableMediaTypes());  //Liberty change
          log.warn(ViolationException.getLocalizedMessage());  //Liberty change
          throw ViolationException;  //Liberty change
      }
   }

   @Override
   public boolean isValidatable(Class<?> clazz)
   {
      // Called from resteasy-jaxrs.
      if (cdiActive && !(hasEJBScope(clazz) && hasNoClassOrFieldOrPropertyConstraints(clazz)))
      {
         return false;
      }
      return true;
   }

   @Override
   public boolean isValidatable(Class<?> clazz, InjectorFactory injectorFactory)
   {
      try
      {
         // Called from resteasy-jaxrs.
         if (cdiActive && injectorFactory instanceof CdiInjectorFactory
               && !(hasEJBScope(clazz) && hasNoClassOrFieldOrPropertyConstraints(clazz)))
         {
            return false;
         }
      }
      catch (NoClassDefFoundError e)
      {
      // Shouldn't get here. Deliberately empty.
      }
      return true;
   }

   @Override
   public boolean isValidatableFromCDI(Class<?> clazz)
   {
      return true;
   }

   @Override
   public boolean isMethodValidatable(Method m)
   {
      if (!isExecutableValidationEnabled)
      {
         return false;
      }

      ExecutableType[] types = null;
      List<ExecutableType[]> typesList = getExecutableTypesOnMethodInHierarchy(m);
      if (typesList.size() > 1)
      {
         throw new ValidationException(Messages.MESSAGES.validateOnExceptionOnMultipleMethod());
      }
      if (typesList.size() == 1)
      {
         types = typesList.get(0);
      }
      else
      {
         ValidateOnExecution voe = m.getDeclaringClass().getAnnotation(ValidateOnExecution.class);
         if (voe == null)
         {
            types = defaultValidatedExecutableTypes;
         }
         else
         {
            if (voe.type().length > 0)
            {
               types = voe.type();
            }
            else
            {
               types = defaultValidatedExecutableTypes;
            }
         }
      }

      boolean isGetterMethod = isGetter(m);
      for (int i = 0; i < types.length; i++)
      {
         switch (types[i])
         {
            case IMPLICIT:
            case ALL:
               return true;

            case NONE:
               continue;

            case NON_GETTER_METHODS:
               if (!isGetterMethod)
               {
                  return true;
               }
               continue;

            case GETTER_METHODS:
               if (isGetterMethod)
               {
                  return true;
               }
               continue;

            default:
               continue;
         }
      }
      return false;
   }

   protected List<ExecutableType[]> getExecutableTypesOnMethodInHierarchy(Method method)
   {
      Class<?> clazz = method.getDeclaringClass();
      List<ExecutableType[]> typesList = new ArrayList<ExecutableType[]>();

      while (clazz != null)
      {
         // We start by examining the method itself.
         Method superMethod = getSuperMethod(method, clazz);
         if (superMethod != null)
         {
            ExecutableType[] types = getExecutableTypesOnMethod(superMethod);
            if (types != null)
            {
               typesList.add(types);
            }
         }

         typesList.addAll(getExecutableTypesOnMethodInInterfaces(clazz, method));
         clazz = clazz.getSuperclass();
      }
      return typesList;
   }

   protected List<ExecutableType[]> getExecutableTypesOnMethodInInterfaces(Class<?> clazz, Method method)
   {
      List<ExecutableType[]> typesList = new ArrayList<ExecutableType[]>();
      Class<?>[] interfaces = clazz.getInterfaces();
      for (int i = 0; i < interfaces.length; i++)
      {
         Method interfaceMethod = getSuperMethod(method, interfaces[i]);
         if (interfaceMethod != null)
         {
            ExecutableType[] types = getExecutableTypesOnMethod(interfaceMethod);
            if (types != null)
            {
               typesList.add(types);
            }
         }
         List<ExecutableType[]> superList = getExecutableTypesOnMethodInInterfaces(interfaces[i], method);
         if (superList.size() > 0)
         {
            typesList.addAll(superList);
         }
      }
      return typesList;
   }

   protected static ExecutableType[] getExecutableTypesOnMethod(Method method)
   {
      ValidateOnExecution voe = method.getAnnotation(ValidateOnExecution.class);
      if (voe == null || voe.type().length == 0)
      {
         return null;
      }
      ExecutableType[] types = voe.type();
      if (types == null || types.length == 0)
      {
         return null;
      }
      return types;
   }

   protected static boolean isGetter(Method m)
   {
      String name = m.getName();
      Class<?> returnType = m.getReturnType();
      if (returnType.equals(Void.class))
      {
         return false;
      }
      if (m.getParameterTypes().length > 0)
      {
         return false;
      }
      if (name.startsWith("get"))
      {
         return true;
      }
      if (name.startsWith("is") && returnType.equals(boolean.class))
      {
         return true;
      }
      return false;
   }

   protected static  String convertArrayToString(Object o)
   {
      String result = null;
      if (o instanceof Object[])
      {
         Object[] array = Object[].class.cast(o);
         StringBuffer sb = new StringBuffer("[").append(convertArrayToString(array[0]));
         for (int i = 1; i < array.length; i++)
         {
            sb.append(", ").append(convertArrayToString(array[i]));
         }
         sb.append("]");
         result = sb.toString();
      }
      else
      {
         result = (o == null ? "" : o.toString());
      }
      return result;
   }

   /**
    * Returns a super method, if any, of a method in a class.
    * Here, the "super" relationship is reflexive.  That is, a method
    * is a super method of itself.
    */
   protected Method getSuperMethod(Method method, final Class<?> clazz)
   {
      Method[] methods = new Method[0];
      try {
         if (System.getSecurityManager() == null) {
            methods = clazz.getDeclaredMethods();
         } else {
            methods = AccessController.doPrivileged(new PrivilegedExceptionAction<Method[]>() {
               @Override
               public Method[] run() throws Exception {
                  return clazz.getDeclaredMethods();
               }
            });
         }
      } catch (PrivilegedActionException pae) {

      }

      for (int i = 0; i < methods.length; i++)
      {
         if (overrides(method, methods[i]))
         {
            return methods[i];
         }
      }
      return null;
   }

   /**
    * Checks, whether {@code subTypeMethod} overrides {@code superTypeMethod}.
    *
    * N.B. "Override" here is reflexive. I.e., a method overrides itself.
    *
    * @param subTypeMethod   The sub type method (cannot be {@code null}).
    * @param superTypeMethod The super type method (cannot be {@code null}).
    *
    * @return Returns {@code true} if {@code subTypeMethod} overrides {@code superTypeMethod}, {@code false} otherwise.
    *
    * Taken from Hibernate Validator
    */
   protected boolean overrides(Method subTypeMethod, Method superTypeMethod)
   {
      if (subTypeMethod == null || superTypeMethod == null)
      {
         throw new RuntimeException(Messages.MESSAGES.expectTwoNonNullMethods());
      }

      if (!subTypeMethod.getName().equals(superTypeMethod.getName()))
      {
         return false;
      }

      if (subTypeMethod.getParameterTypes().length != superTypeMethod.getParameterTypes().length)
      {
         return false;
      }

      if (!superTypeMethod.getDeclaringClass().isAssignableFrom(subTypeMethod.getDeclaringClass()))
      {
         return false;
      }

      return parametersResolveToSameTypes(subTypeMethod, superTypeMethod);
   }

   /**
    * Taken from Hibernate Validator
    */
   protected boolean parametersResolveToSameTypes(Method subTypeMethod, Method superTypeMethod)
   {
      if (subTypeMethod.getParameterTypes().length == 0)
      {
         return true;
      }

      ResolvedType resolvedSubType = typeResolver.resolve(subTypeMethod.getDeclaringClass());
      MemberResolver memberResolver = new MemberResolver(typeResolver);
      memberResolver.setMethodFilter(new SimpleMethodFilter(subTypeMethod, superTypeMethod));
      final ResolvedTypeWithMembers typeWithMembers = memberResolver.resolve(resolvedSubType, null, null);
      ResolvedMethod[] resolvedMethods = new ResolvedMethod[0];
      try {
         if (System.getSecurityManager() == null) {
            resolvedMethods = typeWithMembers.getMemberMethods();
         } else {
            resolvedMethods = AccessController.doPrivileged(new PrivilegedExceptionAction<ResolvedMethod[]>() {
               @Override
               public ResolvedMethod[] run() throws Exception {
                  return typeWithMembers.getMemberMethods();
               }
            });
         }
      } catch (PrivilegedActionException pae) {

      }

      // The ClassMate doc says that overridden methods are flattened to one
      // resolved method. But that is the case only for methods without any
      // generic parameters.
      if (resolvedMethods.length == 1)
      {
         return true;
      }

      // For methods with generic parameters I have to compare the argument
      // types (which are resolved) of the two filtered member methods.
      for (int i = 0; i < resolvedMethods[0].getArgumentCount(); i++)
      {

         if (!resolvedMethods[0].getArgumentType(i).equals(resolvedMethods[1].getArgumentType(i)))
         {
            return false;
         }
      }

      return true;
   }

   @Override
   @SuppressWarnings({"rawtypes", "unchecked"})
   public void checkForConstraintViolations(HttpRequest request, Exception e)
   {
      Throwable t = e;
      while (t != null && !(t instanceof ConstraintViolationException))
      {
         t = t.getCause();
      }

      if (t instanceof ResteasyViolationException)
      {
         throw ResteasyViolationException.class.cast(t);
      }

      if (t instanceof ConstraintViolationException)
      {
         SimpleViolationsContainer violationsContainer = getViolationsContainer(request, null);
         ConstraintViolationException cve = ConstraintViolationException.class.cast(t);
         Set cvs = cve.getConstraintViolations();
         violationsContainer.addViolations(cvs);
         if (violationsContainer.size() > 0)
         {
             ResteasyViolationExceptionImpl ViolationException = new ResteasyViolationExceptionImpl(violationsContainer, request.getHttpHeaders().getAcceptableMediaTypes());  //Liberty change
             log.warn(ViolationException.getLocalizedMessage());  //Liberty change
             throw ViolationException;  //Liberty change            
         }
      }

      return;
   }

   protected Validator getValidator(HttpRequest request)
   {
      Validator v = Validator.class.cast(request.getAttribute(Validator.class.getName()));
      if (v == null) {
         Locale locale = getLocale(request);
         if (locale == null)
         {
            v = validatorFactory.getValidator();
         }
         else
         {
            MessageInterpolator interpolator = new LocaleSpecificMessageInterpolator(validatorFactory.getMessageInterpolator(), locale);
            v = validatorFactory.usingContext().messageInterpolator(interpolator).getValidator();
         }
         request.setAttribute(Validator.class.getName(), v);
      }
      return v;
   }

   protected SimpleViolationsContainer getViolationsContainer(HttpRequest request, Object target)
   {
      if (request == null)
      {
         return new SimpleViolationsContainer(target);
      }

      SimpleViolationsContainer violationsContainer = SimpleViolationsContainer.class.cast(request.getAttribute(SimpleViolationsContainer.class.getName()));
      if (violationsContainer == null)
      {
         violationsContainer = new SimpleViolationsContainer(target);
         request.setAttribute(SimpleViolationsContainer.class.getName(), violationsContainer);
      }
      return violationsContainer;
   }

   private Locale getLocale(HttpRequest request) {
      if (request == null)
      {
         return null;
      }
      List<Locale> locales = request.getHttpHeaders().getAcceptableLanguages();
      Locale locale = locales == null || locales.isEmpty() ? null : locales.get(0);
      return locale;
   }

   /**
    * A filter implementation filtering methods matching given methods.
    *
    * @author Gunnar Morling
    *
    * Taken from Hibernate Validator
    */
   protected static class SimpleMethodFilter implements Filter<RawMethod>
   {
      private final Method method1;
      private final Method method2;

      private SimpleMethodFilter(final Method method1, final Method method2)
      {
         this.method1 = method1;
         this.method2 = method2;
      }

      @Override
      public boolean include(RawMethod element)
      {
         return element.getRawMember().equals(method1) || element.getRawMember().equals(method2);
      }
   }

   protected static class LocaleSpecificMessageInterpolator implements MessageInterpolator {
      private final MessageInterpolator interpolator;
      private final Locale locale;

      public LocaleSpecificMessageInterpolator(final MessageInterpolator interpolator, final Locale locale)
      {
         this.interpolator = interpolator;
         this.locale = locale;
      }

      @Override
      public String interpolate(String messageTemplate, Context context)
      {
         return interpolator.interpolate(messageTemplate, context, locale);
      }

      @Override
      public String interpolate(String messageTemplate, Context context, Locale locale)
      {
         return interpolator.interpolate(messageTemplate, context, locale);
      }
   }

   ResteasyConstraintViolation createResteasyConstraintViolation(ConstraintViolation<?> cv, Type ct)
   {
      String path = (suppressPath ? "*" : cv.getPropertyPath().toString());
      ResteasyConstraintViolation rcv = new ResteasyConstraintViolation(ct, path, cv.getMessage(), (cv.getInvalidValue() == null ? "null" :cv.getInvalidValue().toString()));
      return rcv;
   }

   private boolean hasApplicationScope(Object o)
   {
      Class<?> clazz = o.getClass();
      return clazz.getAnnotation(ApplicationScoped.class) != null;
   }

   private static boolean hasNoClassOrFieldOrPropertyConstraints(Class<?> clazz)
   {
      return !hasClassConstraint(clazz) && !hasFieldConstraint(clazz) && !hasPropertyConstraint(clazz);
   }

   private static boolean hasPropertyConstraint(Class<?> clazz)
   {
      for (Method method : clazz.getDeclaredMethods())
      {
         if (isGetter(method))
         {
            for (Annotation annotation : method.getAnnotations())
            {
               if (isConstraintAnnotation(annotation.annotationType()))
               {
                  return true;
               }
            }
         }
      }
      for (Class<?> intf : clazz.getInterfaces())
      {
         if (hasPropertyConstraint(intf))
         {
            return true;
         }
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null && !superClass.equals(Object.class))
      {
         return hasPropertyConstraint(superClass);
      }
      return false;
   }

   private static boolean hasFieldConstraint(Class<?> clazz)
   {
      for (Field field : clazz.getDeclaredFields())
      {
         for (Annotation annotation : field.getAnnotations())
         {
            if (isConstraintAnnotation(annotation.annotationType()))
            {
               return true;
            }
         }
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null && !superClass.equals(Object.class))
      {
         return hasFieldConstraint(superClass);
      }
      return false;
   }

   private static boolean hasClassConstraint(Class<?> clazz)
   {
      if (classHasConstraintAnnotation(clazz))
      {
         return true;
      }
      for (Class<?> intf : clazz.getInterfaces())
      {
         if (classHasConstraintAnnotation(intf))
         {
            return true;
         }
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != Object.class && superClass != null)
      {
         return hasClassConstraint(superClass);
      }
      return false;
   }

   private static boolean classHasConstraintAnnotation(Class<?> clazz)
   {
      for (Annotation annotation : clazz.getAnnotations())
      {
         if (isConstraintAnnotation(annotation.annotationType()))
         {
            return true;
         }
      }
      return false;
   }

   private static boolean isConstraintAnnotation(Class<?> clazz)
   {
      return clazz.isAnnotation() && clazz.getAnnotation(Constraint.class) != null;
   }

   private static boolean hasEJBScope(Class<?> clazz)
   {
      return classHasAnnotations(clazz, new String[] {"javax.ejb.Stateless", "javax.ejb.Stateful", "javax.ejb.Singleton"});
   }

   private static boolean classHasAnnotations(Class<?> clazz, String[] names)
   {
      for (String name : names)
      {
         if (isAnnotationPresent(clazz, name))
         {
            return true;
         }
         for (Class<?> intf : clazz.getInterfaces())
         {
            if (isAnnotationPresent(intf, name))
            {
               return true;
            }
         }
      }
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != Object.class && superClass != null)
      {
         return classHasAnnotations(superClass, names);
      }
      return false;
   }

   private static boolean isAnnotationPresent(Class<?> clazz, String name)
   {
      for (Annotation annotation : clazz.getAnnotations())
      {
         if (annotation.annotationType().getName().equals(name))
         {
            return true;
         }
      }
      return false;
   }

   private static boolean alreadyFoundClassOrPropertyConstraint(SimpleViolationsContainer container)
   {
      Set<ConstraintViolation<Object>> set = container.getViolations();
      if (set.isEmpty())
      {
         return false;
      }
      Iterator<ConstraintViolation<Object>> it = set.iterator();
      for (ConstraintViolation<?> cv = it.next(); it.hasNext(); cv = it.next())
      {
         ConstraintType.Type type = util.getConstraintType(cv);
         if ((ConstraintType.Type.CLASS.equals(type) || ConstraintType.Type.PROPERTY.equals(type)))
         {
            return true;
         }
      }
      return false;
   }
}
