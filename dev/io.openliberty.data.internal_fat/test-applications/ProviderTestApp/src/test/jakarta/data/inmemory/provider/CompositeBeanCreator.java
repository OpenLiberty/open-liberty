/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package test.jakarta.data.inmemory.provider;

import jakarta.data.repository.Repository;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;

/**
 * Creates beans for repositories for which the entity class has the CompositeEntity annotation.
 */
public class CompositeBeanCreator implements SyntheticBeanCreator<Object> {

    @Override
    public Object create(Instance<Object> instance, Parameters parameters) {
        String dataStore = parameters.get("dataStore", String.class);
        if (dataStore == Repository.DEFAULT_DATA_STORE)
            dataStore = "[DEFAULT_DATA_STORE]";

        System.out.println("Creating repository for " + instance + ", dataStore: " + dataStore);

        return new CompositeRepository();
    }
}
