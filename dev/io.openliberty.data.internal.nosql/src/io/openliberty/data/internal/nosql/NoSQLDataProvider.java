/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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
package io.openliberty.data.internal.nosql;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.LibertyDataProvider;
import jakarta.data.Template;
import jakarta.nosql.mapping.Entity;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = LibertyDataProvider.class)
public class NoSQLDataProvider implements LibertyDataProvider {

    @Override
    @Trivial
    public void entitiesFound(String databaseId, ClassLoader loader, List<Class<?>> entities) {
    }

    @Override
    public <R> R getRepository(Class<R> repositoryInterface) {
        // TODO replace this no-op instance with a real one that is supplied by Jakarta NoSQL,
        // or better yet remove this whole provider entirely once Jakarta NoSQL provides its own.
        Class<?> entityClass = LibertyDataProvider.entityClass.get();
        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               new QueryHandler<>(repositoryInterface, entityClass)));
    }

    @Override
    @Trivial
    public Template getTemplate() {
        return null; // TODO return a Jakarta NoSQL Template
    }

    @Override
    public String name() {
        return "Open Liberty Mock NoSQL Data Provider";
    }

    @Override
    public void repositoryBeanDisposed(Object repository) {
        // TODO hopefully this entire class can be deleted in favor of directly invoking
        // the NoSQL DataProvider if that pattern gets added to the specification.
    }

    @Override
    public Set<Class<? extends Annotation>> supportedEntityAnnotations() {
        return Set.of(Entity.class);
    }
}