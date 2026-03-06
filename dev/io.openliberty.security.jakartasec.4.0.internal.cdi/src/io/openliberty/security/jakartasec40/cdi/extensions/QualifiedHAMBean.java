/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec40.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.interceptor.InterceptorBinding;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

/**
 * Custom CDI bean for qualified HttpAuthenticationMechanism instances.
 */
public class QualifiedHAMBean implements Bean<HttpAuthenticationMechanism>, PassivationCapable {

    private final BeanManager beanManager;
    private final Class<? extends HttpAuthenticationMechanism> hamClass;
    private final Set<Annotation> qualifiers;
    private final Set<Type> types;
    private final Properties props;
    private final String name;
    private final String id;

    public QualifiedHAMBean(BeanManager beanManager,
                            Class<?> hamClass,
                            Set<Annotation> qualifiers,
                            Properties props) {
        this.beanManager = beanManager;
        this.hamClass = (Class<? extends HttpAuthenticationMechanism>) hamClass;
        this.qualifiers = qualifiers;
        this.props = props;

        Set<Type> typeSet = new HashSet<>();
        typeSet.add(hamClass);
        typeSet.add(HttpAuthenticationMechanism.class);
        typeSet.add(Object.class);
        this.types = Collections.unmodifiableSet(typeSet);

        this.name = this.getClass().getName() + "@" + this.hashCode() + "[" + hamClass.getName() + "]";
        this.id = beanManager.hashCode() + "#" + this.name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpAuthenticationMechanism create(CreationalContext<HttpAuthenticationMechanism> cc) throws DeploymentException {
        Class<HttpAuthenticationMechanism> hamClassRaw = (Class<HttpAuthenticationMechanism>) hamClass;

        // for interceptors - currently just LoginToContinue on Forms, but handled generically with metadata
        InterceptionFactory<HttpAuthenticationMechanism> factory = beanManager.createInterceptionFactory(cc, hamClassRaw);
        for (Annotation annotation : hamClass.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(InterceptorBinding.class)) {
                factory.configure().add(annotation);
            }
        }

        // create and configure HAM instance
        // do this without any specific HAM class reference, so it works for
        // Basic/Form/CustomForm and OpenID HAMs created via qualifiers
        HttpAuthenticationMechanism ham;
        try {
            ham = hamClass.getDeclaredConstructor().newInstance();
            Method setPropsMethod = hamClass.getMethod("setQualifiedProperties", Properties.class);
            setPropsMethod.invoke(ham, props);
        } catch (Exception e) {
            throw new DeploymentException("Failed to create HAM: " + hamClass.getName(), e);
        }

        return factory.createInterceptedInstance(ham);
    }

    @Override
    public void destroy(HttpAuthenticationMechanism instance, CreationalContext<HttpAuthenticationMechanism> ctx) {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return ApplicationScoped.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
        return types;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public Class<?> getBeanClass() {
        return hamClass;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return id;
    }
}
