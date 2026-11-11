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
package io.openliberty.persistence.service.hibernate.fat.persistence.test;

import java.io.Writer;

/**
 * Shared interface between the OSGi consumer bundle and the test web application.
 */
public interface Consumer {

    void createTables();

    boolean personExists(long personId);

    long persistPerson(boolean newTran, int numCars);

    long persistPerson(boolean newTran, int numCars, String data);

    String[] getPersonName(long personId);

    /**
     * Jakarta Data 1.1-style: find all persons whose firstName matches the given value.
     * Executes a JPQL query (SELECT p FROM Person p WHERE p.firstName = :first)
     * which exercises the ANTLR-based HQL/JPQL parser in Hibernate 8.x.
     *
     * @return array of lastName values for matching persons
     */
    String[] findByFirstName(String firstName);

    void generateDDL(Writer out);

    String getDatabaseTerminationToken();

    void close();
}
