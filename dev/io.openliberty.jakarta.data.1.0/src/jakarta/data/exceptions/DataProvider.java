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
package jakarta.data.exceptions;

import java.util.Set;

/**
 * To propose in Jakarta Data.
 */
public interface DataProvider {
    <R> R createRepository(Class<R> repositoryInterface, Class<?> entityClass);

    void disposeRepository(Object repository);

    String name();

    Set<DataConnectionException> supportedDatabaseTypes();
}
