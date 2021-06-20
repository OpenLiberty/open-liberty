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
package com.ibm.ws.javaee.ddmodel.client;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class ApplicationClientDDParser extends DDParserSpec {
    private static final String APPCLIENT_DTD_PUBLIC_ID_12 = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN";
    private static final String APPCLIENT_DTD_PUBLIC_ID_13 = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";

    private static final VersionData[] VERSION_DATA = {
        new VersionData("1.2", APPCLIENT_DTD_PUBLIC_ID_12, null, ApplicationClient.VERSION_1_2, JavaEEVersion.VERSION_1_2_INT),    
        new VersionData("1.3", APPCLIENT_DTD_PUBLIC_ID_13, null, ApplicationClient.VERSION_1_3, JavaEEVersion.VERSION_1_3_INT),

        new VersionData("1.4", null, NAMESPACE_SUN_J2EE,   ApplicationClient.VERSION_1_4, JavaEEVersion.VERSION_1_4_INT),            
        new VersionData("5",   null, NAMESPACE_SUN_JAVAEE, ApplicationClient.VERSION_5,   JavaEEVersion.VERSION_5_0_INT),                    
        new VersionData("6",   null, NAMESPACE_SUN_JAVAEE, ApplicationClient.VERSION_6,   JavaEEVersion.VERSION_6_0_INT),                            
        new VersionData("7",   null, NAMESPACE_JCP_JAVAEE, ApplicationClient.VERSION_7,   JavaEEVersion.VERSION_7_0_INT),                            
        new VersionData("8",   null, NAMESPACE_JCP_JAVAEE, ApplicationClient.VERSION_8,   JavaEEVersion.VERSION_8_0_INT),                            

        new VersionData("9", null, NAMESPACE_JAKARTA, ApplicationClient.VERSION_9, JavaEEVersion.VERSION_9_0_INT)
    };

    @Override
    protected VersionData[] getVersionData() {
        return VERSION_DATA;
    }
            
    protected static int adjustSchemaVersion(int maxSchemaVersion) {
        return ( (maxSchemaVersion < Application.VERSION_6) ? Application.VERSION_6 : maxSchemaVersion );
    }
            
    public ApplicationClientDDParser(Container ddRootContainer, Entry ddEntry, int maxSchemaVersion)
        throws ParseException {

        super( ddRootContainer, ddEntry,
               adjustSchemaVersion(maxSchemaVersion),
               "application-client");
    }

    @Override
    public ApplicationClientType parse() throws ParseException {
        return (ApplicationClientType) super.parse();
    }

    @Override
    protected ParsableElement createRootElement() {
        return new ApplicationClientType( getDeploymentDescriptorPath() );
    }
}
