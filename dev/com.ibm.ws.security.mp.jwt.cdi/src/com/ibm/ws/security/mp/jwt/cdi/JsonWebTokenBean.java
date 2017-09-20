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
package com.ibm.ws.security.mp.jwt.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.TypeLiteral;
import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.context.SubjectManager;

/**
 *
 */
public class JsonWebTokenBean implements Bean<JsonWebToken>, PassivationCapable {

    private static final TraceComponent tc = Tr.register(JsonWebTokenBean.class);

    private final String name;
    private final String id;
    private final Class<? extends Annotation> scope = RequestScoped.class;
    private final HashSet<Annotation> qualifiers;
    private final Type type;
    private final Set<Type> types;
    private final Set<InjectionPoint> injectionPoints;

    public JsonWebTokenBean(BeanManager beanManager) {
        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {});
        type = new TypeLiteral<JsonWebToken>() {}.getType();
        types = Collections.singleton(type);
        injectionPoints = Collections.emptySet(); //Collections.singleton(ip);

        this.name = this.getClass().getName() + "[" + type + "]";
        this.id = beanManager.hashCode() + "#" + this.name;
    }

    /** {@inheritDoc} */
    @Override
    public JsonWebToken create(CreationalContext<JsonWebToken> creationalContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "create", creationalContext);
        }

        JsonWebToken jsonWebToken = null;

        Subject subject = new SubjectManager().getCallerSubject();
        Set<JsonWebToken> jsonWebTokenPrincipal = subject.getPrincipals(JsonWebToken.class);

        if (!jsonWebTokenPrincipal.isEmpty()) {
            jsonWebToken = jsonWebTokenPrincipal.iterator().next();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "create", jsonWebToken);
        }
        return jsonWebToken;
    }

    /** {@inheritDoc} */
    @Override
    public void destroy(JsonWebToken arg0, CreationalContext<JsonWebToken> creationalContext) {
        creationalContext.release();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /** {@inheritDoc} */
    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    /** {@inheritDoc} */
    @Override
    public Set<Type> getTypes() {
        return types;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAlternative() {
        // TODO: Determine if this is an alternative to Principal
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Class<?> getBeanClass() {
        return JsonWebToken.class;
    }

    /** {@inheritDoc} */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isNullable() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

}
