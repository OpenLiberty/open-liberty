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
 * TODO Remove this class once Liberty's Jakarta Persistence container
 * integration code implements the requirement to automatically bind the
 * EntityManagerFactory for a persistence unit in JNDI as
 * java:module/persistence/{unit-name}/EntityManagerFactory and
 * java:app/persistence/{unit-name}/EntityManagerFactory and
 */
@ApplicationScoped
@PersistenceUnit(name = "java:module/persistence/MyDataStore/EntityManagerFactory",
                 unitName = "MyDataStore")
public class DataStoreRefDefiner {
}
