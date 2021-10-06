/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.beanvalidation.tests;

import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.topology.database.container.DatabaseContainerFactory;

/**
 *
 */
public class AbstractFATSuite {
    public final static String[] JAXB_PERMS = { "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";" };

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();

    public static String repeatPhase = "";

    public static JPAPersistenceProvider provider = JPAPersistenceProvider.DEFAULT;
}
