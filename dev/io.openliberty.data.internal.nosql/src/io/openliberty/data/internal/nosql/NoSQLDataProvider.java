/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data.internal.nosql;

import java.lang.reflect.Proxy;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.data.internal.DataProvider;
import jakarta.data.Template;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataProvider.class)
public class NoSQLDataProvider implements DataProvider {

    @Override
    public <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass) {
        // TODO replace this no-op instance with a real one that is supplied by Jakarta NoSQL
        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               new QueryHandler<>(repositoryInterface, entityClass)));
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
}