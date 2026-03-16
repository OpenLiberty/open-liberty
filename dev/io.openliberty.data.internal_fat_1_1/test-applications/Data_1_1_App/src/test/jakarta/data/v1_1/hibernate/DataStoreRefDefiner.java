/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.jakarta.data.v1_1.hibernate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;

/**
 * Defines a persistence unit reference with the name
 * java:app/env/data/dbref name so that Jakarta Data repositories
 * will use it.
 */
@ApplicationScoped
@PersistenceUnit(name = "java:app/env/data/dbref",
                 unitName = "HibernatePersistenceUnit")
public class DataStoreRefDefiner {
}
