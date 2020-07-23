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
package com.ibm.ws.beanvalidation.v20;

import static org.hibernate.validator.internal.util.CollectionHelper.newArrayList;

import java.lang.annotation.ElementType;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.BootstrapConfiguration;
import javax.validation.Configuration;
import javax.validation.Validation;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.hibernate.validator.internal.properties.DefaultGetterPropertySelectionStrategy;
import org.hibernate.validator.internal.util.Contracts;
import org.hibernate.validator.internal.util.ExecutableHelper;
import org.hibernate.validator.internal.util.TypeResolutionHelper;
import org.hibernate.validator.internal.util.classhierarchy.ClassHierarchyHelper;
import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;
import org.hibernate.validator.internal.util.privilegedactions.GetMethods;
import org.hibernate.validator.spi.properties.ConstrainableExecutable;
import org.hibernate.validator.spi.properties.GetterPropertySelectionStrategy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.util.dopriv.SetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.beanvalidation.AbstractBeanValidation.ClassLoaderTuple;
import com.ibm.ws.beanvalidation.service.ConstrainedHelper;
import com.ibm.ws.beanvalidation.service.Validation20ClassLoader;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.classloading.ClassLoadingService;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true)
public class ConstrainedHelperImpl implements ConstrainedHelper {

    private static final TraceComponent tc = Tr.register(ConstrainedHelperImpl.class);
    private static final Log log = LoggerFactory.make(MethodHandles.lookup());
    private static final EnumSet<ExecutableType> ALL_EXECUTABLE_TYPES = EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS, ExecutableType.GETTER_METHODS);
    private static final EnumSet<ExecutableType> DEFAULT_EXECUTABLE_TYPES = EnumSet.of(ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS);

    private final ExecutableHelper executableHelper;
    private final GetterPropertySelectionStrategy getterPropertySelectionStrategy;

    private Set<ExecutableType> globalExecutableTypes;
    private boolean isExecutableValidationEnabled;

    @Reference
    private ClassLoadingService classLoadingService;

    private static class ConstrainableMethod implements ConstrainableExecutable {

        private final Method method;

        private ConstrainableMethod(Method method) {
            this.method = method;
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public Class<?> getReturnType() {
            return method.getReturnType();
        }
    }

    private static class InheritedMethodsHelper {

        public static List<Method> getAllMethods(Class<?> clazz) {
            Contracts.assertNotNull(clazz);

            List<Method> methods = newArrayList();

            for (Class<?> hierarchyClass : ClassHierarchyHelper.getHierarchy(clazz)) {
                Collections.addAll(methods, run(GetMethods.action(hierarchyClass)));
            }

            return methods;
        }

        private static <T> T run(PrivilegedAction<T> action) {
            return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
        }
    }

    public ConstrainedHelperImpl() {
        executableHelper = new ExecutableHelper(new TypeResolutionHelper());
        getterPropertySelectionStrategy = new DefaultGetterPropertySelectionStrategy();
    }

    private EnumSet<ExecutableType> commonExecutableTypeChecks(ValidateOnExecution validateOnExecutionAnnotation) {
        if (validateOnExecutionAnnotation == null) {
            return EnumSet.noneOf(ExecutableType.class);
        }

        EnumSet<ExecutableType> executableTypes = EnumSet.noneOf(ExecutableType.class);
        if (validateOnExecutionAnnotation.type().length == 0) {
            executableTypes.add(ExecutableType.NONE);
        } else {
            Collections.addAll(executableTypes, validateOnExecutionAnnotation.type());
        }

        // IMPLICIT cannot be mixed 10.1.2 of spec - Mixing IMPLICIT and other executable types is illegal
        if (executableTypes.contains(ExecutableType.IMPLICIT) && executableTypes.size() > 1) {
            throw log.getMixingImplicitWithOtherExecutableTypesException();
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

    @Override
    public boolean isConstructorConstrained(Constructor<?> constructor, BeanDescriptor beanDescriptor, ClassLoader moduleClassLoader, String moduleUri) {

        setupGlobalValidationSettings(moduleClassLoader, moduleUri);

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

        setupGlobalValidationSettings(moduleClassLoader, moduleUri);

        if (!isExecutableValidationEnabled) {
            return false;
        }

        List<Method> overriddenAndImplementedMethods = InheritedMethodsHelper.getAllMethods(method.getDeclaringClass());

        Optional<String> correspondingProperty = getterPropertySelectionStrategy.getProperty(new ConstrainableMethod(method));

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
            if (executableHelper.overrides(method, overriddenOrInterfaceMethod)) {
                if (method.getAnnotation(ValidateOnExecution.class) != null) {
                    throw log.getValidateOnExecutionOnOverriddenOrInterfaceMethodException(method);
                }
                return overriddenOrInterfaceMethod;
            }
        }

        return method;
    }

    private void setupGlobalValidationSettings(ClassLoader moduleClassLoader, String moduleUri) {
        SetContextClassLoaderPrivileged setClassLoader = null;
        ClassLoader oldClassLoader = null;
        ClassLoaderTuple tuple = null;
        try {
            tuple = configureBvalClassloader(moduleClassLoader);
            ClassLoader tcclClassLoaderTmp = tuple.classLoader;

            ClassLoader bvalClassLoader = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> new Validation20ClassLoader(tcclClassLoaderTmp, moduleUri));

            ThreadContextAccessor tca = AccessController.doPrivileged((PrivilegedAction<ThreadContextAccessor>) () -> ThreadContextAccessor.getThreadContextAccessor());

            // set the thread context class loader to be used, must be reset in finally block
            setClassLoader = new SetContextClassLoaderPrivileged(tca);
            oldClassLoader = setClassLoader.execute(bvalClassLoader);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Called setClassLoader with oldClassLoader of " + oldClassLoader + " and newClassLoader of " + bvalClassLoader);
            }

            Configuration<?> config = Validation.byDefaultProvider().configure();
            BootstrapConfiguration bootstrap = config.getBootstrapConfiguration();
            globalExecutableTypes = bootstrap.getDefaultValidatedExecutableTypes();
            isExecutableValidationEnabled = bootstrap.isExecutableValidationEnabled();

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
