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
package test.jakarta.data.jpa.web.hibernate;

import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

/**
 * For tests that only run on the Hibernate Persistence provider.
 * Also creates a datastore reference to the persistence unit used for the common set of tests.
 */
@PersistenceUnit(name = "java:app/env/data/DataStoreRef",
                 unitName = "HibernatePersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/DataJPAEclipseLinkServlet")
public class DataJPAHibernateServlet extends FATServlet {

}
