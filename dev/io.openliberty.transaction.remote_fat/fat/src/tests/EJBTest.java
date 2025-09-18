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
package tests;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

public class EJBTest extends FATServletClient {

    static final String CLIENT_APP_NAME = "TestBeanClient";

    /**
     * @param servers
     * @throws PrivilegedActionException
     */
    static void afterClass(LibertyServer... servers) throws PrivilegedActionException {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                FATUtils.stopServers(new String[] { "CNTR0019E" }, servers);
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }
}