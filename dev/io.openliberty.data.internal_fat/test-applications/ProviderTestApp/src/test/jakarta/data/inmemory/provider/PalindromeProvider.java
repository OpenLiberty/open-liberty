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
package test.jakarta.data.inmemory.provider;

import java.util.Set;

import jakarta.data.provider.DataProvider;
import jakarta.data.provider.DatabaseType;

/**
 * A fake Jakarta Data provider that only produces a single repository class,
 * which is because it doesn't have a real implementation and is only for tests
 * that obtain a DataProvider from the ServiceLoader.
 */
public class PalindromeProvider implements DataProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass) {
        return (R) new PalindromeRepository();
    }

    @Override
    public void disposeRepository(Object repository) {
    }

    @Override
    public String name() {
        return "Palindrome Data Provider";
    }

    @Override
    public Set<DatabaseType> supportedDatabaseTypes() {
        return Set.of(DatabaseType.values());
    }

}
