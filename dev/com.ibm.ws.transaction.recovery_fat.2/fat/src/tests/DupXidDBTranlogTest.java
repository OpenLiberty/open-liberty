/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package tests;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class DupXidDBTranlogTest extends DupXidTestBase {

    @Server("recovery1.dblog")
    public static LibertyServer s1;

    @Server("recovery2.dblog")
    public static LibertyServer s2;

    @BeforeClass
    public static void setup() throws Exception {
        Log.info(DupXidDBTranlogTest.class, "subBefore", "");
        // Delete existing DB files, so that the tables that support transaction recovery
        // are created from scratch.
        s1.deleteFileFromLibertyInstallRoot("/usr/shared/resources/data/tranlogdb");
        setup(s1, s2, APP_NAME + "/DupXidDBTranlogServlet");
    }
}