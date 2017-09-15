/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import java.lang.reflect.Method;

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
import com.ibm.ws.microprofile.faulttolerance.cdi.FTUtils;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackHandlerFactory;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceFunction;
import com.ibm.ws.microprofile.faulttolerance.spi.FaultToleranceProvider;

public class FallbackConfig extends AbstractAnnotationConfig<Fallback> implements Fallback {

    private static final TraceComponent tc = Tr.register(FallbackConfig.class);

    @SuppressWarnings("rawtypes")
    private final AnnotationParameterConfig<Class<? extends FallbackHandler>> valueConfig = getParameterConfigClass("value", FallbackHandler.class);
    private final AnnotationParameterConfig<String> fallbackMethodConfig = getParameterConfig("fallbackMethod", String.class);

    public FallbackConfig(Method annotatedMethod, Class<?> annotatedClass, Fallback annotation) {
        super(annotatedMethod, annotatedClass, annotation, Fallback.class);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends FallbackHandler<?>> value() {
        return (Class<? extends FallbackHandler<?>>) valueConfig.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public String fallbackMethod() {
        return fallbackMethodConfig.getValue();
    }

    @Override
    public void validate() {
        //validate the fallback annotation

        Method originalMethod = getAnnotatedMethod();
        Class<?> originalMethodReturnType = originalMethod.getReturnType();
        Class<?>[] originalMethodParamTypes = originalMethod.getParameterTypes();

        Class<? extends FallbackHandler<?>> fallbackClass = value();
        String fallbackMethodName = fallbackMethod();
        //If both fallback method and fallback class are set, it is an illegal state.
        if ((fallbackClass != null && fallbackClass != Fallback.DEFAULT.class) && (fallbackMethodName != null && !"".equals(fallbackMethodName))) {
            throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.policy.conflicts.CWMFT5009E", originalMethod, fallbackClass, fallbackMethodName));
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
                    throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.policy.invalid.CWMFT5008E", originalMethod, fallbackClass, originalMethodReturnType,
                                                                                 originalMethod));
                }
            } catch (NoSuchMethodException e) {
                //should not happen
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "internal.error.CWMFT5998E"), e);
            } catch (SecurityException e) {
                //should not happen
                throw new FaultToleranceDefinitionException((Tr.formatMessage(tc, "internal.error.CWMFT5998E")), e);
            }

        } else if (fallbackMethodName != null && !"".equals(fallbackMethodName)) {

            try {

                Method fallbackMethod = originalMethod.getDeclaringClass().getMethod(fallbackMethodName, originalMethodParamTypes);

                Class<?> fallbackReturn = fallbackMethod.getReturnType();
                if (!originalMethodReturnType.isAssignableFrom(fallbackReturn)) {
                    throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.policy.return.type.not.match.CWMFT5002E", fallbackMethod, originalMethod));
                }

            } catch (NoSuchMethodException e) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "fallback.method.not.found.CWMFT5003E", fallbackMethodName,
                                                                             originalMethod.getName(), originalMethod.getDeclaringClass()), e);
            } catch (SecurityException e) {
                throw new FaultToleranceDefinitionException((Tr.formatMessage(tc, "security.exception.acquiring.fallback.method.CWMFT5004E")), e);
            }

        }
    }

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
            Class<?>[] paramTypes = originalMethod.getParameterTypes();
            try {
                Method fallbackMethod = beanInstance.getClass().getMethod(fallbackMethodName, paramTypes);
                Class<?> originalReturn = originalMethod.getReturnType();
                Class<?> fallbackReturn = fallbackMethod.getReturnType();
                if (originalReturn.isAssignableFrom(fallbackReturn)) {
                    fallbackPolicy = newFallbackPolicy(beanInstance, fallbackMethod, fallbackReturn);
                }
            } catch (NoSuchMethodException e) {
                throw new FaultToleranceException(Tr.formatMessage(tc, "fallback.method.not.found.CWMFT5003E", fallbackMethodName, originalMethod.getName(),
                                                                   beanInstance.getClass()), e);
            } catch (SecurityException e) {
                throw new FaultToleranceException((Tr.formatMessage(tc, "security.exception.acquiring.fallback.method.CWMFT5004E")), e);
            }
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
            @Override
            public R execute(ExecutionContext context) throws Exception {
                @SuppressWarnings("unchecked")
                R result = (R) fallbackMethod.invoke(beanInstance, context.getParameters());
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
