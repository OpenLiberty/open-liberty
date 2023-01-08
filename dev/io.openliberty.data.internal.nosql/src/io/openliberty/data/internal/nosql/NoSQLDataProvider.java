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
package io.openliberty.data.internal.nosql;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.LibertyDataProvider;
import jakarta.data.Template;
import jakarta.data.provider.DatabaseType;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = LibertyDataProvider.class)
public class NoSQLDataProvider implements LibertyDataProvider {

    @Override
    public <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass) {
        // TODO replace this no-op instance with a real one that is supplied by Jakarta NoSQL
        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               new QueryHandler<>(repositoryInterface, entityClass)));
    }

    @Override
    public void disposeRepository(Object repository) {
        // TODO hopefully this entire class can be deleted in favor of directly invoking
        // the NoSQL DataProvider if that pattern gets added to the specification.
    }

    @Override
    @Trivial
    public void entitiesFound(String databaseId, ClassLoader loader, List<Class<?>> entities) {
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
    public Set<DatabaseType> supportedDatabaseTypes() {
        return Set.of(DatabaseType.COLUMN, DatabaseType.DOCUMENT, DatabaseType.GRAPH, DatabaseType.KEY_VALUE);
    }
}