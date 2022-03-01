/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.fail;

import java.util.List;

import com.ibm.ws.transaction.fat.util.TxFATServletClient;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class FailoverTest extends TxFATServletClient {

    public void runInServletAndCheck(LibertyServer server, String path, String method) throws Exception {
        StringBuilder sb = runInServlet(server, path, method);

        check(sb, server, method);
    }

    public void check(StringBuilder sb, LibertyServer server, String method) throws Exception {
        if (!sb.toString().contains(SUCCESS)) {
            server.resetLogMarks();
            List<String> probableFailure = server.findStringsInLogs("WTRN0107W: Caught SQLException when opening SQL RecoveryLog");
            if (probableFailure != null && !probableFailure.isEmpty()) {
                fail(probableFailure.get(0));
            } else {
                fail(method + " did not return " + SUCCESS + ". Returned: " + sb.toString());
            }
        }
    }
}
