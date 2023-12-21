/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.web;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.ws.javaee.ddmodel.web.common.WebAppType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class WebAppDDParser extends DDParserSpec {
    public static final String WEBAPP_DTD_PUBLIC_ID_22 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    public static final String WEBAPP_DTD_PUBLIC_ID_23 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    
    public static VersionData[] VERSION_DATA = {
        new VersionData("2.2", WEBAPP_DTD_PUBLIC_ID_22, null, WebApp.VERSION_2_2, VERSION_1_2_INT),
        new VersionData("2.3", WEBAPP_DTD_PUBLIC_ID_23, null, WebApp.VERSION_2_3, VERSION_1_3_INT),

        new VersionData("2.4", null, NAMESPACE_SUN_J2EE,   WebApp.VERSION_2_4, VERSION_1_4_INT),
        new VersionData("2.5", null, NAMESPACE_SUN_JAVAEE, WebApp.VERSION_2_5, VERSION_5_0_INT),
        new VersionData("3.0", null, NAMESPACE_SUN_JAVAEE, WebApp.VERSION_3_0, VERSION_6_0_INT),
        new VersionData("3.1", null, NAMESPACE_JCP_JAVAEE, WebApp.VERSION_3_1, VERSION_7_0_INT),
        new VersionData("4.0", null, NAMESPACE_JCP_JAVAEE, WebApp.VERSION_4_0, VERSION_8_0_INT),

        new VersionData("5.0", null, NAMESPACE_JAKARTA, WebApp.VERSION_5_0, VERSION_9_0_INT),
        new VersionData("6.0", null, NAMESPACE_JAKARTA, WebApp.VERSION_6_0, VERSION_10_0_INT),
        new VersionData("6.1", null, NAMESPACE_JAKARTA, WebApp.VERSION_6_1, VERSION_11_0_INT)
    };

    public static int getMaxTolerated() {
        return WebApp.VERSION_6_1;
    }
    
    public static int getMaxImplemented() {
        return WebApp.VERSION_6_0;
    }
    
    @Override    
    protected VersionData[] getVersionData() {
        return VERSION_DATA;
    }

    protected static int adjustSchemaVersion(int maxSchemaVersion) {
        return ( (maxSchemaVersion < WebApp.VERSION_3_0) ? WebApp.VERSION_3_0 : maxSchemaVersion );
    }

    public WebAppDDParser(Container ddRootContainer, Entry ddEntry, int maxSchemaVersion) throws ParseException {
        super( ddRootContainer, ddEntry,
               adjustSchemaVersion(maxSchemaVersion),
               TRIM_SIMPLE_CONTENT,
               "web-app" );
    }

    @Override    
    public WebAppType parse() throws ParseException {
        return (WebAppType) super.parse();
    }

    @Override    
    protected ParsableElement createRootElement() {
        return new WebAppType( getDeploymentDescriptorPath() );        
    }
}
