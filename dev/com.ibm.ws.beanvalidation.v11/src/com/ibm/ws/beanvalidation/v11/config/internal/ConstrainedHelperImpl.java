/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.v11.config.internal;

import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.fasterxml.classmate.Filter;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.AbstractBeanValidation.ClassLoaderTuple;
import com.ibm.ws.beanvalidation.BVNLSConstants;
import com.ibm.ws.beanvalidation.service.BeanValidationExtensionHelper;
import com.ibm.ws.beanvalidation.service.ConstrainedHelper;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.classloading.ClassLoadingService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class ConstrainedHelperImpl implements ConstrainedHelper {

    private static final TraceNLS nls = TraceNLS.getTraceNLS(ConstrainedHelperImpl.class, BVNLSConstants.BV_RESOURCE_BUNDLE);
    private static final TraceComponent tc = Tr.register(ConstrainedHelperImpl.class);
    private static final EnumSet<ExecutableType> ALL_EXECUTABLE_TYPES = EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS);
    private static final EnumSet<ExecutableType> DEFAULT_EXECUTABLE_TYPES = EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS);
    private static final String GETTER_PREFIX_GET = "get";

    private static final String GETTER_PREFIX_IS = "is";
    private static final String GETTER_PREFIX_HAS = "has";
    private static final String WELD_PROXY_INTERFACE_NAME = "org.jboss.weld.bean.proxy.ProxyObject";

    private final TypeResolver typeResolver = new TypeResolver();

    private Configuration<?> config = null;
    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    @Reference
    private ClassLoadingService classLoadingService;

    private static class ConstrainableMethod {

        private final Method method;

        private ConstrainableMethod(Method method) {
            this.method = method;
        }

        public String getName() {
            return method.getName();
        }

        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        public Class<?> getReturnType() {
            return method.getReturnType();
        }
    }

    private static class InheritedMethodsHelper {

        /**
         * Get a list of all methods which the given class declares, implements,
         * overrides or inherits. Methods are added by adding first all methods of
         * the class itself and its implemented interfaces, then the super class and
         * its interfaces, etc.
         *
         * @param clazz the class for which to retrieve the methods
         *
         * @return set of all methods of the given class
         */
        public static List<Method> getAllMethods(Class<?> clazz) {
            List<Method> methods = new ArrayList<Method>();

            for (Class<?> hierarchyClass : getHierarchy(clazz)) {
                Method[] hierarchyClassMethods = AccessController.doPrivileged((PrivilegedAction<Method[]>) () -> hierarchyClass.getMethods());
                Collections.addAll(methods, hierarchyClassMethods);
            }

            return methods;
        }
    }

    private static class SimpleMethodFilter implements Filter<RawMethod> {
        private final Method method1;
        private final Method method2;

        private SimpleMethodFilter(Method method1, Method method2) {
            this.method1 = method1;
            this.method2 = method2;
        }

        @Override
        public boolean include(RawMethod element) {
            return element.getRawMember().equals(method1) || element.getRawMember().equals(method2);
        }
    }

    private static Set<ExecutableType> convertToRuntimeTypes(final Set<ExecutableType> defaultValidatedExecutableTypes) {
        final Set<ExecutableType> types = EnumSet.noneOf(ExecutableType.class);
        for (final ExecutableType type : defaultValidatedExecutableTypes) {
            if (ExecutableType.NONE == type) {
                continue;
            }
            if (ExecutableType.ALL == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
                types.add(ExecutableType.GETTER_METHODS);
                break;
            }
            if (ExecutableType.IMPLICIT == type) {
                types.add(ExecutableType.CONSTRUCTORS);
                types.add(ExecutableType.NON_GETTER_METHODS);
            } else {
                types.add(type);
            }
        }
        return types;
    }

    private static String decapitalize(String string) {
        if (string == null || string.isEmpty() || startsWithSeveralUpperCaseLetters(string)) {
            return string;
        } else {
            return string.substring(0, 1).toLowerCase(Locale.ROOT) + string.substring(1);
        }
    }

    private static <T> void getHierarchy(Class<? super T> clazz, List<Class<? super T>> classes) {
        for (Class<? super T> current = clazz; current != null; current = current.getSuperclass()) {
            if (classes.contains(current)) {
                return;
            }

            if (!isWeldProxy(current)) {
                classes.add(current);
            }

            for (Class<?> currentInterface : current.getInterfaces()) {
                //safe since interfaces are super-types
                @SuppressWarnings("unchecked")
                Class<? super T> currentInterfaceCasted = (Class<? super T>) currentInterface;
                getHierarchy(currentInterfaceCasted, classes);
            }
        }
    }

    private static <T> List<Class<? super T>> getHierarchy(Class<T> clazz) {
        List<Class<? super T>> classes = new ArrayList<Class<? super T>>();
        getHierarchy(clazz, classes);
        return classes;
    }

    private static boolean isMethodVisibleTo(Method visibleMethod, Method otherMethod) {
        return Modifier.isPublic(visibleMethod.getModifiers()) || Modifier.isProtected(visibleMethod.getModifiers())
               || visibleMethod.getDeclaringClass().getPackage().equals(otherMethod.getDeclaringClass().getPackage());
    }

    private static boolean isWeldProxy(Class<?> clazz) {
        for (Class<?> implementedInterface : clazz.getInterfaces()) {
            if (implementedInterface.getName().equals(WELD_PROXY_INTERFACE_NAME)) {
                return true;
            }
        }

        return false;
    }

    private static boolean startsWithSeveralUpperCaseLetters(String string) {
        return string.length() > 1 &&
               Character.isUpperCase(string.charAt(0)) &&
               Character.isUpperCase(string.charAt(1));
    }

    private EnumSet<ExecutableType> commonExecutableTypeChecks(ValidateOnExecution validateOnExecutionAnnotation) {
        if (validateOnExecutionAnnotation == null) {
            return EnumSet.noneOf(ExecutableType.class);
        }

        EnumSet<ExecutableType> executableTypes = EnumSet.noneOf(ExecutableType.class);
        if (validateOnExecutionAnnotation.type().length == 0) { // HV-757
            executableTypes.add(ExecutableType.NONE);
        } else {
            Collections.addAll(executableTypes, validateOnExecutionAnnotation.type());
        }

        // IMPLICIT cannot be mixed 10.1.2 of spec - Mixing IMPLICIT and other executable types is illegal
        if (executableTypes.contains(ExecutableType.IMPLICIT) && executableTypes.size() > 1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Mixing IMPLICIT and other executable types is not allowed.");
            throw new IllegalArgumentException(nls.getString("BVKEY_MIXING_IMPLICIT_TYPE_NOT_ALLOWED_CWNBV0008E",
                                                             "CWNBV0008E: Mixing IMPLICIT and other executable types is not allowed."));
        }

        // NONE can be removed 10.1.2 of spec - A list containing NONE and other types of executables is equivalent to a
        // list containing the types of executables without NONE.
        if (executableTypes.contains(ExecutableType.NONE) && executableTypes.size() > 1) {
            executableTypes.remove(ExecutableType.NONE);
        }

        // 10.1.2 of spec - A list containing ALL and other types of executables is equivalent to a list containing only ALL
        if (executableTypes.contains(ExecutableType.ALL)) {
            executableTypes = ALL_EXECUTABLE_TYPES;
        }

        return executableTypes;
    }

    private ClassLoaderTuple configureBvalClassloader(ClassLoader cl) {
        if (cl == null) {
            cl = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        }
        if (cl != null) {
            if (classLoadingService.isThreadContextClassLoader(cl)) {
                return ClassLoaderTuple.of(cl, false);
            } else if (classLoadingService.isAppClassLoader(cl)) {
                return ClassLoaderTuple.of(createTCCL(cl), true);
            }
        }
        return ClassLoaderTuple.of(createTCCL(ConstrainedHelper.class.getClassLoader()), true);
    }

    private ClassLoader createTCCL(ClassLoader parentCL) {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> classLoadingService.createThreadContextClassLoader(parentCL));
    }

    private EnumSet<ExecutableType> executableTypesDefinedOnConstructor(Constructor<?> constructor) {
        ValidateOnExecution validateOnExecutionAnnotation = constructor.getAnnotation(
                                                                                      ValidateOnExecution.class);
        EnumSet<ExecutableType> executableTypes = commonExecutableTypeChecks(validateOnExecutionAnnotation);

        if (executableTypes.contains(ExecutableType.IMPLICIT)) {
            executableTypes.add(ExecutableType.CONSTRUCTORS);
        }

        return executableTypes;
    }

    private EnumSet<ExecutableType> executableTypesDefinedOnMethod(Method method, boolean isGetter) {
        ValidateOnExecution validateOnExecutionAnnotation = method.getAnnotation(ValidateOnExecution.class);
        EnumSet<ExecutableType> executableTypes = commonExecutableTypeChecks(validateOnExecutionAnnotation);

        if (executableTypes.contains(ExecutableType.IMPLICIT)) {
            if (isGetter) {
                executableTypes.add(ExecutableType.GETTER_METHODS);
            } else {
                executableTypes.add(ExecutableType.NON_GETTER_METHODS);
            }
        }

        return executableTypes;
    }

    private EnumSet<ExecutableType> executableTypesDefinedOnType(Class<?> clazz) {
        ValidateOnExecution validateOnExecutionAnnotation = clazz.getAnnotation(ValidateOnExecution.class);
        EnumSet<ExecutableType> executableTypes = commonExecutableTypeChecks(validateOnExecutionAnnotation);

        if (executableTypes.contains(ExecutableType.IMPLICIT)) {
            return DEFAULT_EXECUTABLE_TYPES;
        }

        return executableTypes;
    }

    private Optional<String> getProperty(ConstrainableMethod executable) {
        if (executable.getParameterTypes().length != 0) {
            return Optional.empty();
        }
        String methodName = executable.getName();

        if (methodName.startsWith(GETTER_PREFIX_GET) && executable.getReturnType() != void.class) {
            return Optional.of(decapitalize(methodName.substring(GETTER_PREFIX_IS.length())));
        } else if (methodName.startsWith(GETTER_PREFIX_IS) && executable.getReturnType() == boolean.class) {
            return Optional.of(decapitalize(methodName.substring(GETTER_PREFIX_IS.length())));
        } else if (methodName.startsWith(GETTER_PREFIX_HAS) && executable.getReturnType() == boolean.class) {
            return Optional.of(decapitalize(methodName.substring(GETTER_PREFIX_HAS.length())));
        }

        return Optional.empty();
    }

    private boolean instanceMethodParametersResolveToSameTypes(Class<?> mainSubType, Method left, Method right) {
        if (left.getParameterTypes().length == 0) {
            return true;
        }

        ResolvedType resolvedSubType = typeResolver.resolve(mainSubType);

        MemberResolver memberResolver = new MemberResolver(typeResolver);
        memberResolver.setMethodFilter(new SimpleMethodFilter(left, right));
        ResolvedTypeWithMembers typeWithMembers = memberResolver.resolve(
                                                                         resolvedSubType,
                                                                         null,
                                                                         null);

        ResolvedMethod[] resolvedMethods = AccessController.doPrivileged((PrivilegedAction<ResolvedMethod[]>) () -> typeWithMembers.getMemberMethods());

        // The ClassMate doc says that overridden methods are flattened to one
        // resolved method. But that is the case only for methods without any
        // generic parameters.
        if (resolvedMethods.length == 1) {
            return true;
        }

        // For methods with generic parameters I have to compare the argument
        // types (which are resolved) of the two filtered member methods.
        for (int i = 0; i < resolvedMethods[0].getArgumentCount(); i++) {
            if (!resolvedMethods[0].getArgumentType(i).equals(resolvedMethods[1].getArgumentType(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean instanceMethodParametersResolveToSameTypes(Method subTypeMethod, Method superTypeMethod) {
        return instanceMethodParametersResolveToSameTypes(subTypeMethod.getDeclaringClass(), subTypeMethod, superTypeMethod);
    }

    @Override
    public boolean isConstructorConstrained(Constructor<?> constructor, BeanDescriptor beanDescriptor, ClassLoader moduleClassLoader, String moduleUri) {

        setupGlobalValidationSettings(moduleClassLoader);

        if (!isExecutableValidationEnabled) {
            return false;
        }

        EnumSet<ExecutableType> classLevelExecutableTypes = executableTypesDefinedOnType(constructor.getDeclaringClass());
        EnumSet<ExecutableType> memberLevelExecutableType = executableTypesDefinedOnConstructor(constructor);

        if (veto(classLevelExecutableTypes, memberLevelExecutableType, ExecutableType.CONSTRUCTORS)) {
            return false;
        }

        if (beanDescriptor.getConstraintsForConstructor(constructor.getParameterTypes()) != null) {
            return true;
        }

        return false;
    }

    private boolean isGetterConstrained(BeanDescriptor beanDescriptor, Method method, String property) {
        PropertyDescriptor propertyDescriptor = beanDescriptor.getConstraintsForProperty(property);
        return propertyDescriptor != null && propertyDescriptor.findConstraints().declaredOn(ElementType.METHOD).hasConstraints();
    }

    @Override
    public boolean isMethodConstrained(Method method, BeanDescriptor beanDescriptor, ClassLoader moduleClassLoader, String moduleUri) {

        setupGlobalValidationSettings(moduleClassLoader);

        if (!isExecutableValidationEnabled) {
            return false;
        }

        List<Method> overriddenAndImplementedMethods = InheritedMethodsHelper.getAllMethods(method.getDeclaringClass());

        Optional<String> correspondingProperty = getProperty(new ConstrainableMethod(method));

        // obtain @ValidateOnExecution from the top-most method in the hierarchy
        Method methodForExecutableTypeRetrieval = replaceWithOverriddenOrInterfaceMethod(method, overriddenAndImplementedMethods);

        EnumSet<ExecutableType> classLevelExecutableTypes = executableTypesDefinedOnType(methodForExecutableTypeRetrieval.getDeclaringClass());
        EnumSet<ExecutableType> memberLevelExecutableType = executableTypesDefinedOnMethod(methodForExecutableTypeRetrieval,
                                                                                           correspondingProperty.isPresent());

        ExecutableType currentExecutableType = correspondingProperty.isPresent() ? ExecutableType.GETTER_METHODS : ExecutableType.NON_GETTER_METHODS;

        // validation is enabled per default, so explicit configuration can just veto whether
        // validation occurs
        if (veto(classLevelExecutableTypes, memberLevelExecutableType, currentExecutableType)) {
            return false;
        }

        boolean needsValidation;
        if (correspondingProperty.isPresent()) {
            needsValidation = isGetterConstrained(beanDescriptor, method, correspondingProperty.get());
        } else {
            needsValidation = isNonGetterConstrained(beanDescriptor, method);
        }

        return needsValidation;
    }

    private boolean isNonGetterConstrained(BeanDescriptor beanDescriptor, Method method) {
        return beanDescriptor.getConstraintsForMethod(method.getName(), method.getParameterTypes()) != null;
    }

    private boolean overrides(Method subTypeMethod, Method superTypeMethod) {

        if (subTypeMethod.equals(superTypeMethod)) {
            return false;
        }

        if (!subTypeMethod.getName().equals(superTypeMethod.getName())) {
            return false;
        }

        if (subTypeMethod.getParameterTypes().length != superTypeMethod.getParameterTypes().length) {
            return false;
        }

        if (!superTypeMethod.getDeclaringClass().isAssignableFrom(subTypeMethod.getDeclaringClass())) {
            return false;
        }

        if (Modifier.isStatic(superTypeMethod.getModifiers()) || Modifier.isStatic(
                                                                                   subTypeMethod.getModifiers())) {
            return false;
        }

        if (subTypeMethod.isBridge()) {
            return false;
        }

        if (Modifier.isPrivate(superTypeMethod.getModifiers())) {
            return false;
        }

        if (!isMethodVisibleTo(superTypeMethod, subTypeMethod)) {
            return false;
        }

        return instanceMethodParametersResolveToSameTypes(subTypeMethod, superTypeMethod);
    }

    private void releaseLoader(ClassLoader tccl) {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            classLoadingService.destroyThreadContextClassLoader(tccl);
            return null;
        });
    }

    private Method replaceWithOverriddenOrInterfaceMethod(Method method, List<Method> allMethodsOfType) {
        LinkedList<Method> list = new LinkedList<>(allMethodsOfType);
        Iterator<Method> iterator = list.descendingIterator();
        while (iterator.hasNext()) {
            Method overriddenOrInterfaceMethod = iterator.next();
            if (overrides(method, overriddenOrInterfaceMethod)) {
                if (method.getAnnotation(ValidateOnExecution.class) != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "@ValidateOnExecution is not allowed on methods overriding a superclass method or implementing an interface. Check configuration for " + method);
                    throw new ValidationException(nls.getFormattedMessage("BVKEY_VALIDATE_ON_EXECUTION_NOT_ALLOWED_CWNBV0007E", new Object[] { method },
                                                                          "CWNBV0007E: @ValidateOnExecution is not allowed on methods overriding a superclass method or implementing an interface. Check configuration for "
                                                                                                                                                         + method));
                }
                return overriddenOrInterfaceMethod;
            }
        }

        return method;
    }

    private void setupGlobalValidationSettings(ClassLoader appCl) {
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoader classLoader = null;
        ClassLoaderTuple tuple = null;

        try {
            ThreadContextAccessor tca = AccessController.doPrivileged((PrivilegedAction<ThreadContextAccessor>) () -> ThreadContextAccessor.getThreadContextAccessor());
            tuple = configureBvalClassloader(appCl);
            classLoader = tuple.classLoader;

            //Use customer class loader to handle multiple validation.xml being in the same ear.
            classLoader = BeanValidationExtensionHelper.newValidationClassLoader(classLoader);

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(classLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of " + oldClassLoader + " and newClassLoader of " + classLoader);
            }

            config = Validation.byDefaultProvider().configure();
            try {
                final BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
                globalExecutableTypes = Collections.unmodifiableSet(convertToRuntimeTypes(bootstrap.getDefaultValidatedExecutableTypes()));
                isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();
            } catch (final Exception e) { // custom providers can throw an exception
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, e.getMessage(), e);
                }

                globalExecutableTypes = Collections.emptySet();
                isExecutableValidationEnabled = false;
            }

        } catch (ValidationException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Returning a null Configuration: " + e.getMessage());
            }
        } finally {
            if (setClassLoader != null) {
                setClassLoader.execute(oldClassLoader);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set Class loader back to " + oldClassLoader);
                }
            }
            if (tuple != null && tuple.wasCreatedViaClassLoadingService) {
                releaseLoader(tuple.classLoader);
            }
        }
    }

    private boolean veto(EnumSet<ExecutableType> classLevelExecutableTypes,
                         EnumSet<ExecutableType> memberLevelExecutableType,
                         ExecutableType currentExecutableType) {
        if (!memberLevelExecutableType.isEmpty()) {
            return !memberLevelExecutableType.contains(currentExecutableType)
                   && !memberLevelExecutableType.contains(ExecutableType.IMPLICIT);
        }

        if (!classLevelExecutableTypes.isEmpty()) {
            return !classLevelExecutableTypes.contains(currentExecutableType)
                   && !classLevelExecutableTypes.contains(ExecutableType.IMPLICIT);
        }

        return !globalExecutableTypes.contains(currentExecutableType);
    }
}
