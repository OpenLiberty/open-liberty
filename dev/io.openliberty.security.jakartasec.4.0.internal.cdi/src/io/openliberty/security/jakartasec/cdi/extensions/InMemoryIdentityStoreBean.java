/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.openliberty.security.jakartasec40.identitystore.InMemoryIdentityStore;
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
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.security.enterprise.identitystore.InMemoryIdentityStoreDefinition;

/* ***
 * A CDI bean that is registered by the CDI extension (JakartaSecurity40CDIExtension)
 * when an @InMemoryIdentityStoreDefinition is found. Its main purpose is:
 * - hold an InMemoryIdentityStoreDefinition and pass it to the InMemoryIdentityStore upon creation,
 * - to create and manage the InMemoryIdentityStore instances, and generally,
 * - to bridge the CDI and identity store implementations.
 *
 *
 * Application Class (with @InMemoryIdentityStoreDefinition annotation)
 * `--> JakataSecurity40CDIExtension (scans for this annotation and if found ...)
 *      `--> InMemoryIdentityStoreBean (*THIS CLASS*) holding the annotation definition
 *           `--> DONE for now.
 *
 * <... eventually the authentication workflow is required, so ...>
 *
 *           `--> create() create a new InMemoryIdentityStore
 *           `--> destroy() destroys existing InMemoryIdentityStore
 */

public class InMemoryIdentityStoreBean implements Bean<IdentityStore>, PassivationCapable {

    private final Set<Annotation> qualifiers;
    private final Type type;
    private final Set<Type> types;
    private final String name;
    private final String id;

    // passed to the InMemoryIdentityStore upon create() invocation
    private final InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition;

    @SuppressWarnings("serial")
    public InMemoryIdentityStoreBean(BeanManager beanManager, InMemoryIdentityStoreDefinition inMemoryIdentityStoreDefinition) {
        this.inMemoryIdentityStoreDefinition = inMemoryIdentityStoreDefinition;
        qualifiers = new HashSet<Annotation>();
        qualifiers.add(new AnnotationLiteral<Default>() {
        });
        qualifiers.add(new AnnotationLiteral<Any>() {
        });
        type = new TypeLiteral<IdentityStore>() {
        }.getType();
        types = Collections.singleton(type);
        name = this.getClass().getName() + "@" + this.hashCode() + "[" + type + "]";
        id = beanManager.hashCode() + "#" + this.name;
    }

    @Override
    public IdentityStore create(CreationalContext<IdentityStore> arg0) {
        return new InMemoryIdentityStore(inMemoryIdentityStoreDefinition);
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
        return IdentityStore.class;
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
