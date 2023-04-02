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
package com.ibm.ws.transaction.fat.util;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.topology.impl.LibertyServer;

public class TxShrinkHelper extends ShrinkHelper {

    public static WebArchive buildDefaultApp(LibertyServer server, String appName, String appPath, String... packages) throws Exception {
        WebArchive app = buildDefaultAppFromPath(appName, appPath, packages);
        exportAppToServer(server, app, new DeployOptions[0]);
        return app;
   }
}