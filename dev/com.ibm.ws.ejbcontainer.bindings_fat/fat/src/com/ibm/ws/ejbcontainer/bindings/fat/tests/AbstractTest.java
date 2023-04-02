/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
public abstract class AbstractTest extends FATServletClient {

    public static void stopServer(LibertyServer server, String... expectedFailuresRegExps) throws Exception {
        if (server != null && server.isStarted()) {
            List<String> ignoreList = new ArrayList<String>();

            for (String failures : expectedFailuresRegExps) {
                ignoreList.add(failures);
            }

            // Allow quiesce warnings on windows runs as we have had infrastructure problems
            // that caused us to take longer than 30 seconds to quiesce
            Machine m = server.getMachine();
            if (m.getOperatingSystem() == OperatingSystem.WINDOWS) {
                /*
                 * W CWWKE1102W: The quiesce operation did not complete. The server will now stop.
                 * W CWWKE1107W: N threads did not complete during the quiesce period.
                 */
                ignoreList.add("CWWKE1102W");
                ignoreList.add("CWWKE1107W");
            }

            String[] stringArr = new String[ignoreList.size()];
            server.stopServer(ignoreList.toArray(stringArr));
        }
    }

}
