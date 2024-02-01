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
import com.ibm.ws.javaee.ddmodel.web.common.WebFragmentType;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class WebFragmentDDParser extends DDParserSpec {
    public static VersionData[] VERSION_DATA = {
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
        return WebApp.VERSION_5_0; //TODO should this be updated to 6.0?
    }
    
    @Override
    protected VersionData[] getVersionData() {
        return VERSION_DATA;
    }

    public WebFragmentDDParser(Container ddRootContainer, Entry ddEntry, int maxSchemaVersion) throws ParseException {
        super( ddRootContainer, ddEntry,
               WebAppDDParser.adjustSchemaVersion(maxSchemaVersion),
               TRIM_SIMPLE_CONTENT,
               "web-fragment" );
    }

    @Override
    public WebFragmentType parse() throws ParseException {
        return (WebFragmentType) super.parse();
    }

    @Override
    protected ParsableElement createRootElement() {
        return new WebFragmentType( getDeploymentDescriptorPath() );        
    }
}
