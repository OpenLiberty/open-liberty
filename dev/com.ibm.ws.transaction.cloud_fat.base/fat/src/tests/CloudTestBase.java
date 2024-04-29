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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.topology.utils.FATServletClient;

public class CloudTestBase extends FATServletClient {

    private static String[] testRecoveryTables = new String[] {
                                                                "WAS_PARTNER_LOGcloud0011",
                                                                "WAS_LEASES_LOG",
                                                                "WAS_TRAN_LOGcloud0011",
                                                                "WAS_PARTNER_LOGcloud0021",
                                                                "WAS_TRAN_LOGcloud0021"
    };

    protected static void dropTables() {
        Log.info(CloudTestBase.class, "dropTables", String.join(", ", testRecoveryTables));
        TxTestContainerSuite.dropTables(testRecoveryTables);
    }
}
