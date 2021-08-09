/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi20;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.Prioritized;
import javax.enterprise.util.AnnotationLiteral;
import javax.interceptor.Interceptor.Priority;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.ibm.ws.microprofile.faulttolerance.cdi.FaultTolerance;
import com.ibm.ws.microprofile.faulttolerance.cdi.FaultToleranceInterceptor;

/**
 * Implementation of {@link Interceptor} for {@link FaultToleranceInterceptor}
 * <p>
 * The only reason we have this is so that we can implement {@link Prioritized} and set the priority dynamically
 */
public class FaultToleranceCDI20Interceptor implements Interceptor<FaultToleranceInterceptor>, Prioritized {

    private final InjectionTarget<FaultToleranceInterceptor> injectionTarget;
    private final BeanAttributes<FaultToleranceInterceptor> beanAttributes;
    private final Set<Annotation> interceptorBindings;

    private final static int DEFAULT_PRIORITY = Priority.PLATFORM_AFTER + 10;
    private final static int BEFORE_TX_PRIORITY = Priority.PLATFORM_BEFORE + 199;
    private final static String PRIORITY_CONFIG_KEY = "mp.fault.tolerance.interceptor.priority";
    private final static String BEFORE_TX_CONFIG_KEY = "com.ibm.ws.microprofile.faulttolerance.before.transactional";

    @SuppressWarnings("serial")
    private class FaultToleranceBinding extends AnnotationLiteral<FaultTolerance> {
    }

    public FaultToleranceCDI20Interceptor(BeanManager bm) {
        AnnotatedType<FaultToleranceInterceptor> type = bm.createAnnotatedType(FaultToleranceInterceptor.class);
        beanAttributes = bm.createBeanAttributes(type);
        interceptorBindings = Collections.singleton(new FaultToleranceBinding());
        injectionTarget = bm.getInjectionTargetFactory(type).createInjectionTarget(this);
    }

    @Override
    public Class<?> getBeanClass() {
        return FaultToleranceInterceptor.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionTarget.getInjectionPoints();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public FaultToleranceInterceptor create(CreationalContext<FaultToleranceInterceptor> cc) {
        FaultToleranceInterceptor instance = injectionTarget.produce(cc);
        cc.push(instance);
        injectionTarget.inject(instance, cc);
        injectionTarget.postConstruct(instance);
        return instance;
    }

    @Override
    public void destroy(FaultToleranceInterceptor instance, CreationalContext<FaultToleranceInterceptor> cc) {
        injectionTarget.preDestroy(instance);
        injectionTarget.dispose(instance);
        cc.release();
    }

    @Override
    public String getName() {
        return beanAttributes.getName();
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return beanAttributes.getQualifiers();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return beanAttributes.getScope();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return beanAttributes.getStereotypes();
    }

    @Override
    public Set<Type> getTypes() {
        return beanAttributes.getTypes();
    }

    @Override
    public boolean isAlternative() {
        return beanAttributes.isAlternative();
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return interceptorBindings;
    }

    @Override
    public Object intercept(InterceptionType interceptionType, FaultToleranceInterceptor instance, InvocationContext context) throws Exception {
        return instance.executeFT(context);
    }

    @Override
    public boolean intercepts(InterceptionType interceptionType) {
        return interceptionType == InterceptionType.AROUND_INVOKE;
    }

    @Override
    public int getPriority() {
        Config config = ConfigProvider.getConfig();
        boolean isBeforeTx = config.getOptionalValue(BEFORE_TX_CONFIG_KEY, Boolean.class).orElse(false);
        return config.getOptionalValue(PRIORITY_CONFIG_KEY, Integer.class).orElse(isBeforeTx ? BEFORE_TX_PRIORITY : DEFAULT_PRIORITY);
    }

}
