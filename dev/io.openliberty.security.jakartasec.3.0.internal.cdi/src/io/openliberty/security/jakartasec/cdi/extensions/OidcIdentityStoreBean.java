/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.jakartasec.identitystore.OidcIdentityStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.security.enterprise.identitystore.IdentityStore;

public class OidcIdentityStoreBean implements Bean<IdentityStore>, PassivationCapable {

    private static final TraceComponent tc = Tr.register(OidcIdentityStoreBean.class);

    private final Set<Annotation> qualifiers;
    private final Type type;
    private final Set<Type> types;
    private final String name;
    private final String id;

    public OidcIdentityStoreBean(BeanManager beanManager) {
        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {
        });
        type = new TypeLiteral<IdentityStore>() {
        }.getType();
        types = Collections.singleton(type);
        name = this.getClass().getName() + "@" + this.hashCode() + "[" + type + "]";
        id = beanManager.hashCode() + "#" + this.name;
    }

    @Override
    public IdentityStore create(CreationalContext<IdentityStore> arg0) {
        return new OidcIdentityStore();
    }

    @Override
    public void destroy(IdentityStore arg0, CreationalContext<IdentityStore> arg1) {
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
        return OidcIdentityStoreBean.class;
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
