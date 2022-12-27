/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import componenttest.topology.impl.LibertyServer;

public class PrepInitServer {

    static private final Class<?> thisClass = PrepInitServer.class;

    /*
     * This method passed in LibertyServer object to SSLTestCommon
     */

    public static void prepareInit(LibertyServer server) throws Exception {
        SSLTestCommon.server = server;

    }

}
