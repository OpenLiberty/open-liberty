/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.microprofile.faulttolerance.cdi.FTUtils;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FallbackConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.MethodFinder;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackHandlerFactory;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

public class FallbackConfigImpl extends AbstractAnnotationConfig<Fallback> implements FallbackConfig {

    private static final TraceComponent tc = Tr.register(FallbackConfig.class);

    private static final SecureAction secure = AccessController.doPrivileged(SecureAction.get());

    @SuppressWarnings("rawtypes")
    private final AnnotationParameterConfig<Class<? extends FallbackHandler>> valueConfig = getParameterConfigClass("value", FallbackHandler.class);
    private final AnnotationParameterConfig<String> fallbackMethodConfig = getParameterConfig("fallbackMethod", String.class);

    public FallbackConfigImpl(Method annotatedMethod, Class<?> annotatedClass, Fallback annotation) {
        super(annotatedMethod, annotatedClass, annotation, Fallback.class);
    }

    public FallbackConfigImpl(Class<?> annotatedClass, Fallback annotation) {
        super(annotatedClass, annotation, Fallback.class);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends FallbackHandler<?>> value() {
        return (Class<? extends FallbackHandler<?>>) valueConfig.getValue();
    }

    private String fallbackMethod() {
        return fallbackMethodConfig.getValue();
    }

    @Override
    public void validate() {
        //validate the fallback annotation

        Method originalMethod = getAnnotatedMethod();
        Class<?> originalMethodReturnType = originalMethod.getReturnType();

        Class<? extends FallbackHandler<?>> fallbackClass = value();
        String fallbackMethodName = fallbackMethod();
        //If both fallback method and fallback class are set, it is an illegal state.
        if ((fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) && (fallbackMethodName != null && !"".equals(fallbackMethodName))) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.policy.conflicts.CWMFT5009E", FTDebug.formatMethod(originalMethod), fallbackClass,
                                                                         fallbackMethodName));
        } else if (fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) {
            //need to load the fallback class and then find out the method return type
            try {
                Method[] ms = fallbackClass.getMethods();
                Method handleMethod = FallbackHandler.class.getMethod(FTUtils.FALLBACKHANDLE_METHOD_NAME, ExecutionContext.class);
                boolean validFallbackHandler = false;
                for (Method m : ms) {
                    if (m.getName().equals(handleMethod.getName()) && (m.getParameterCount() == 1)) {
                        Class<?>[] params = m.getParameterTypes();
                        if (ExecutionContext.class.isAssignableFrom(params[0])) {
                            //now check the return type
                            if (originalMethodReturnType.isAssignableFrom(m.getReturnType())) {
                                validFallbackHandler = true;
                                break;
                            }
                        }
                    }
                }

                if (!validFallbackHandler) {
                    throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.policy.invalid.CWMFT5008E", FTDebug.formatMethod(originalMethod), fallbackClass,
                                                                                 originalMethodReturnType, originalMethod.getName()));
                }
            } catch (NoSuchMethodException e) {
                //should not happen
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "internal.error.CWMFT5998E"), e);
            } catch (SecurityException e) {
                //should not happen
                throw new FaultToleranceDefinitionException((Tr.formatMessage(tc, "internal.error.CWMFT5998E")), e);
            }

        } else if (fallbackMethodName != null && !"".equals(fallbackMethodName)) {

            Method fallbackMethod = MethodFinder.findMatchingMethod(originalMethod, fallbackMethodName);
            if (fallbackMethod == null) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.method.not.found.CWMFT5021E", fallbackMethodName,
                                                                             originalMethod.getName(), originalMethod.getDeclaringClass()));
            }
        }
    }

    @Override
    public FallbackPolicy generatePolicy(InvocationContext context, BeanManager beanManager) {
        FallbackPolicy fallbackPolicy = null;
        Class<? extends FallbackHandler<?>> fallbackClass = value();
        String fallbackMethodName = fallbackMethod();
        if (fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) {
            FallbackHandlerFactory fallbackHandlerFactory = getFallbackHandlerFactory(beanManager);
            fallbackPolicy = newFallbackPolicy(fallbackClass, fallbackHandlerFactory);
        } else if (fallbackMethodName != null && !"".equals(fallbackMethodName)) {
            Object beanInstance = context.getTarget();
            Method originalMethod = context.getMethod();

            Method fallbackMethod = MethodFinder.findMatchingMethod(originalMethod, fallbackMethodName);

            if (fallbackMethod == null) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.method.not.found.CWMFT5021E", fallbackMethodName,
                                                                             originalMethod.getName(), originalMethod.getDeclaringClass()));
            }

            if (!Modifier.isPublic(fallbackMethod.getModifiers()) && !fallbackMethod.isAccessible()) {
                secure.setAccessible(fallbackMethod, true);
            }

            Class<?> fallbackReturn = fallbackMethod.getReturnType();
            fallbackPolicy = newFallbackPolicy(beanInstance, fallbackMethod, fallbackReturn);
        } else {
            //shouldn't ever reach here since validation should have caught it earlier
            throw new FaultToleranceException(Tr.formatMessage(tc, "internal.error.CWMFT5998E"));
        }
        return fallbackPolicy;
    }

    private static FallbackHandlerFactory getFallbackHandlerFactory(BeanManager beanManager) {
        FallbackHandlerFactory factory = new FallbackHandlerFactory() {
            @Override
            public <F extends FallbackHandler<?>> F newHandler(Class<F> fallbackClass) {
                AnnotatedType<F> aType = beanManager.createAnnotatedType(fallbackClass);
                CreationalContext<F> cc = beanManager.createCreationalContext(null);
                InjectionTargetFactory<F> factory = beanManager.getInjectionTargetFactory(aType);
                InjectionTarget<F> injectionTarget = factory.createInjectionTarget(null);
                F instance = injectionTarget.produce(cc);
                injectionTarget.inject(instance, cc);
                injectionTarget.postConstruct(instance);
                return instance;
            }
        };
        return factory;
    }

    private static <R> FallbackPolicy newFallbackPolicy(Object beanInstance, Method fallbackMethod, Class<R> fallbackReturn) {
        FaultToleranceFunction<ExecutionContext, R> fallbackFunction = new FaultToleranceFunction<ExecutionContext, R>() {
            @SuppressWarnings("unchecked")
            @FFDCIgnore(InvocationTargetException.class)
            @Override
            public R execute(ExecutionContext context) throws Exception {
                R result = null;
                try {
                    result = (R) fallbackMethod.invoke(beanInstance, context.getParameters());
                } catch (InvocationTargetException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof Exception) {
                        // Unwrap the real exception to return it to the user
                        throw (Exception) cause;
                    } else {
                        throw ex;
                    }
                }
                return result;
            }
        };

        FallbackPolicy fallbackPolicy = newFallbackPolicy(fallbackFunction);
        return fallbackPolicy;
    }

    private static FallbackPolicy newFallbackPolicy(Class<? extends FallbackHandler<?>> fallbackHandlerClass, FallbackHandlerFactory factory) {
        FallbackPolicy fallbackPolicy = FaultToleranceProvider.newFallbackPolicy();
        fallbackPolicy.setFallbackHandler(fallbackHandlerClass, factory);
        return fallbackPolicy;
    }

    private static FallbackPolicy newFallbackPolicy(FaultToleranceFunction<ExecutionContext, ?> fallbackFunction) {
        FallbackPolicy fallbackPolicy = FaultToleranceProvider.newFallbackPolicy();
        fallbackPolicy.setFallbackFunction(fallbackFunction);
        return fallbackPolicy;
    }
}
