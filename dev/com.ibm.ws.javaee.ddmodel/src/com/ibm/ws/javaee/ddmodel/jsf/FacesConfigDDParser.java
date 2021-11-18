/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.jsf;

import com.ibm.ws.javaee.dd.jsf.FacesConfig;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

final class FacesConfigDDParser extends DDParserSpec {
    public static final String FACES_CONFIG_DTD_PUBLIC_ID_10 =
        "-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.0//EN";
    public static final String FACES_CONFIG_DTD_PUBLIC_ID_11 =
        "-//Sun Microsystems, Inc.//DTD JavaServer Faces Config 1.1//EN";

    private static final VersionData[] VERSION_DATA = {
        new VersionData("1.0", FACES_CONFIG_DTD_PUBLIC_ID_10, null, FacesConfig.VERSION_1_0, JavaEEVersion.VERSION_1_2_INT),    
        new VersionData("1.1", FACES_CONFIG_DTD_PUBLIC_ID_11, null, FacesConfig.VERSION_1_1, JavaEEVersion.VERSION_1_3_INT),

        new VersionData("1.2", null, NAMESPACE_SUN_JAVAEE, FacesConfig.VERSION_1_2, JavaEEVersion.VERSION_5_0_INT),                    
        new VersionData("2.0", null, NAMESPACE_SUN_JAVAEE, FacesConfig.VERSION_2_0, JavaEEVersion.VERSION_6_0_INT),            
        new VersionData("2.1", null, NAMESPACE_SUN_JAVAEE, FacesConfig.VERSION_2_1, JavaEEVersion.VERSION_6_0_INT),
        new VersionData("2.2", null, NAMESPACE_JCP_JAVAEE, FacesConfig.VERSION_2_2, JavaEEVersion.VERSION_7_0_INT),
        new VersionData("2.3", null, NAMESPACE_JCP_JAVAEE, FacesConfig.VERSION_2_3, JavaEEVersion.VERSION_8_0_INT),                            
        
        new VersionData("3.0", null, NAMESPACE_JAKARTA, FacesConfig.VERSION_3_0, JavaEEVersion.VERSION_9_0_INT)
    };
    
    @Override
    protected VersionData[] getVersionData() {
        return VERSION_DATA;
    } 

    // 2.1 is returned instead of 2.0.
    //
    // The previous implementation always allowed 2.1, even when
    // the max version was set to 2.0.
    //
    // The previous implementation added version requirements
    // starting with 2.2 and higher.
    protected static int adjustSchemaVersion(int maxSchemaVersion) {
        return ( (maxSchemaVersion < FacesConfig.VERSION_2_1) ? FacesConfig.VERSION_2_1 : maxSchemaVersion );
    }

    public FacesConfigDDParser(Container ddRootContainer, Entry ddEntry) throws ParseException {
        this(ddRootContainer, ddEntry, FacesConfig.VERSION_2_0);
    }

    public FacesConfigDDParser(Container ddRootContainer, Entry ddEntry, int maxSchemaVersion) throws ParseException {
        super( ddRootContainer, ddEntry,
               adjustSchemaVersion(maxSchemaVersion),
               "faces-config" );
    }

    public int getFacesBundleLoadedVersion() {
        return maxVersion;
    }

    @Override
    public FacesConfigType parse() throws ParseException {
        return (FacesConfigType) super.parse();
    }

    @Override
    protected FacesConfigType createRootElement() {
        return new FacesConfigType( getDeploymentDescriptorPath() );
    }
}
