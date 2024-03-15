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
package com.ibm.ws.gvt.rest.fat;

import java.io.IOException;
import java.net.HttpURLConnection;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Some HTTP convenience methods.
 */
public class GvtUtils {

    public static String performPostGvt(LibertyServer server, String endpoint,
                                        int expectedResponseStatus, String expectedResponseContentType, String user,
                                        String password, String contentType, String content) throws Exception {

        return HttpUtils.postRequest(server, endpoint, expectedResponseStatus, expectedResponseContentType, user, password, contentType, content);

    }

    /**
     * @param server
     * @return
     * @throws IOException
     */
    public static HttpURLConnection getHttpConnectionForUTF(LibertyServer server) throws IOException {

        return HttpUtils.getHttpConnectionForUTF(server);
    }

}
