/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jakarta.data.web.hibernate;

import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

/**
 * For tests that run on the third party Persistence provider, which is
 * Hibernate. The third party provider cannot be used for record entities nor
 * any repositories that specify a DataSource as their dataStore.
 */
@PersistenceUnit(name = "java:module/env/data/DataStoreRef",
                 unitName = "HibernatePersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/DataHibernateServlet")
public class DataHibernateServlet extends FATServlet {

}
