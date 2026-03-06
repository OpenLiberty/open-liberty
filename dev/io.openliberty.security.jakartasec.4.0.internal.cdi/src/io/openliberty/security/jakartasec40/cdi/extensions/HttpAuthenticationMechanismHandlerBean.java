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
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import io.openliberty.security.jakartasec.handlers.HttpAuthenticationMechanismHandlerImpl;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;

/* ***
 * A CDI bean that is always registered by the CDI extension (JakartaSecurity40CDIExtension).
 * Its main purpose is:
 * - to create and manage the HttpAuthenticationMechanismHandler internal implementation, and
 * - to bridge the CDI and http authentication mechanism handler implementations.
 *
 * <... when required, this bean manages the internal HttpAuthenticationMechanismHandler implementation via ...>
 *
 *           `--> create() create a new HttpAuthenticationMechanismHandler internal implementation
 *           `--> destroy() destroys the existing HttpAuthenticationMechanismHandler internal implementation
 */

public class HttpAuthenticationMechanismHandlerBean implements Bean<HttpAuthenticationMechanismHandler>, PassivationCapable {

    private final Set<Annotation> qualifiers;
    private final Type type;
    private final Set<Type> types;
    private final String name;
    private final String id;

    @SuppressWarnings("serial")
    public HttpAuthenticationMechanismHandlerBean(BeanManager beanManager) {
        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {
        });
        qualifiers.add(new AnnotationLiteral<Any>() {
        });

        type = new TypeLiteral<HttpAuthenticationMechanismHandler>() {
        }.getType();
        types = Collections.singleton(type);
        name = this.getClass().getName() + "[" + type + "]";
        id = beanManager.hashCode() + "#" + this.name;
    }

    @Override
    public HttpAuthenticationMechanismHandler create(CreationalContext<HttpAuthenticationMechanismHandler> creationalContext) {
        return new HttpAuthenticationMechanismHandlerImpl();
    }

    @Override
    public void destroy(HttpAuthenticationMechanismHandler arg0, CreationalContext<HttpAuthenticationMechanismHandler> arg1) {
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
        return HttpAuthenticationMechanismHandlerBean.class;
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
