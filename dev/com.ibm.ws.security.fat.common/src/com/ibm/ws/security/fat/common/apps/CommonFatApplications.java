/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.apps;

import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

public class CommonFatApplications {

    public static WebArchive getTestMarkerApp() throws Exception {
        return ShrinkHelper.buildDefaultAppFromPath("testmarker", getPathToTestApps(), "com.ibm.ws.security.fat.common.apps.testmarker.*");
    }

    private static String getPathToTestApps() {
        // When executing FATs, the "user.dir" property is <OL root>/dev/<FAT project>/build/libs/autoFVT/
        // Hence, to get back to this project, we have to navigate a few levels up.
        return System.getProperty("user.dir") + "/../../../../com.ibm.ws.security.fat.common/";
    }

}