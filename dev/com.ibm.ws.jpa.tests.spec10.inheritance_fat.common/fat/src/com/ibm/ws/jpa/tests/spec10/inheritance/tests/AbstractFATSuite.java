/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.inheritance.tests;

import org.junit.ClassRule;
import org.testcontainers.containers.JdbcDatabaseContainer;

import componenttest.containers.TestContainerSuite;
import componenttest.topology.database.container.DatabaseContainerFactory;

/**
 *
 */
public class AbstractFATSuite extends TestContainerSuite {
    public final static String[] JAXB_PERMS = { "// Required by JAXB",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind.v2.runtime.reflect\";",
                                                "permission java.lang.RuntimePermission \"accessClassInPackage.com.sun.xml.internal.bind\";",
                                                "permission java.lang.RuntimePermission \"accessDeclaredMembers\";",
                                                "",
                                                "// Required by jrt:/openjceplus for local runs with Java Semeru",
                                                "permission java.io.FilePermission \"${java.home}${/}-\", \"read\";",
                                                "permission java.lang.RuntimePermission \"loadLibrary.*\";",
                                                "permission java.util.PropertyPermission \"java.home\", \"read\";",
                                                "permission java.util.PropertyPermission \"java.security.debug\", \"read\";",
                                                "permission java.util.PropertyPermission \"java.security.auth.debug\", \"read\";",
                                                "permission java.util.PropertyPermission \"jgskit.library.path\", \"read\";",
                                                "permission java.util.PropertyPermission \"ock.library.path\", \"read\";",
                                                "permission java.security.SecurityPermission \"putProviderProperty.OpenJCEPlus\";" };

    @ClassRule
    public static JdbcDatabaseContainer<?> testContainer = DatabaseContainerFactory.create();
}
