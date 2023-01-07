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
