/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.ws.javaee.ddmodel.web.common.WebAppType;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class WebAppDDParser extends DDParserSpec {
    public static final String WEBAPP_DTD_PUBLIC_ID_22 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    public static final String WEBAPP_DTD_PUBLIC_ID_23 =
        "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    
    private static VersionData[] VERSION_DATA = {
        new VersionData("2.2", WEBAPP_DTD_PUBLIC_ID_22, null, WebApp.VERSION_2_2, JavaEEVersion.VERSION_1_2_INT),
        new VersionData("2.3", WEBAPP_DTD_PUBLIC_ID_23, null, WebApp.VERSION_2_3, JavaEEVersion.VERSION_1_3_INT),

        new VersionData("2.4", null, NAMESPACE_SUN_J2EE,   WebApp.VERSION_2_4, JavaEEVersion.VERSION_1_4_INT),
        new VersionData("2.5", null, NAMESPACE_SUN_JAVAEE, WebApp.VERSION_2_5, JavaEEVersion.VERSION_5_0_INT),
        new VersionData("3.0", null, NAMESPACE_SUN_JAVAEE, WebApp.VERSION_3_0, JavaEEVersion.VERSION_6_0_INT),
        new VersionData("3.1", null, NAMESPACE_JCP_JAVAEE, WebApp.VERSION_3_1, JavaEEVersion.VERSION_7_0_INT),
        new VersionData("4.0", null, NAMESPACE_JCP_JAVAEE, WebApp.VERSION_4_0, JavaEEVersion.VERSION_8_0_INT),

        new VersionData("5.0", null, NAMESPACE_JAKARTA, WebApp.VERSION_5_0, JavaEEVersion.VERSION_9_0_INT)
    };

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
