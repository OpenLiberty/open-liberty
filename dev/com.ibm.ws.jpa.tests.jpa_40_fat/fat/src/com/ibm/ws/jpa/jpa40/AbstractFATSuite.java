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

package com.ibm.ws.jpa.jpa40;

import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;

public class AbstractFATSuite extends TestContainerSuite {

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    public static String repeatPhase = "";

    public static JPAPersistenceProvider provider = JPAPersistenceProvider.DEFAULT;
}
