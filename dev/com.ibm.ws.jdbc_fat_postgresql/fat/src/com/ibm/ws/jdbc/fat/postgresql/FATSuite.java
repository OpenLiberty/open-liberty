/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.postgresql;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({
                PostgreSQLTest.class,
                PostgreSQLSSLTest.class,
//                ThreadLocalConnectionTest.class
})
public class FATSuite extends TestContainerSuite {
}
