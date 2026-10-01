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
package io.openliberty.persistence.hibernate;

/**
 * Holder for the Hibernate thirdparty bundle's classloader.
 * Set by HibernatePersistenceActivator.start() so that the JPA container can
 * inject the OSGi classloader as the first entry in Hibernate's
 * AggregatedClassLoader, preventing LinkageErrors caused by Hibernate-internal
 * classes being split across the AppClassLoader and EquinoxClassLoader.
 *
 * This class intentionally uses no imports beyond java.lang so that it can be
 * loaded safely by any classloader without triggering further wiring.
 */
public class LibertyHibernateBundle {
    /** The classloader of the Hibernate thirdparty OSGi bundle. Volatile for safe publication. */
    public static volatile ClassLoader classLoader;
}
