/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.derby;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JDBCDerbyTest.class,
                JDBCDynamicConfigTest.class,
                JDBCDerbyPathLibTest.class,
                JDBCDerbyPathToFolderLibTest.class
})
public class FATSuite {

    static final String SERVER = "com.ibm.ws.jdbc.fat.derby";
    static final String jdbcapp = "jdbcapp";

    @BeforeClass
    public static void createArchives() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER);
        ShrinkHelper.defaultApp(server, jdbcapp, "jdbc.fat.derby.web");
        JavaArchive tranNoneDriver = ShrinkHelper.buildJavaArchive("trandriver.jar", "jdbc.tran.none.driver");
        ShrinkHelper.exportToServer(server, "../../shared/resources/derby", tranNoneDriver);
    }

}
