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
package io.openliberty.data.internal.persistence;

import java.lang.reflect.Proxy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.data.internal.DataProvider;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DataProvider.class)
public class PersistenceDataProvider implements DataProvider {

    @Override
    public <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass) {
        // TODO replace this no-op instance with a real one that is based on Jakarta Persistence
        return repositoryInterface.cast(Proxy.newProxyInstance(repositoryInterface.getClassLoader(),
                                                               new Class<?>[] { repositoryInterface },
                                                               new QueryHandler<>(repositoryInterface, entityClass)));
    }
}