/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApps.utils;

import javax.servlet.http.HttpServletRequest;

public class BackChannelLogout_utils {

    public String getClientNameFromAppName(HttpServletRequest req) throws Exception {

        String app = getAppName(req);
        String client = app.replace("_postLogout", "");

        return client;
    }

    public String getAppName(HttpServletRequest req) throws Exception {

        String uri = req.getRequestURI();
        System.out.println("app - uri: " + uri);
        String[] splits = uri.split("/");

        String app = splits[splits.length - 1];

        return app;
    }

}
