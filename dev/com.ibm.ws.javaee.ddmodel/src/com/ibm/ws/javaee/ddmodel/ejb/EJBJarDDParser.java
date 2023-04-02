/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.ejb;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.ddmodel.DDParserSpec;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;

public class EJBJarDDParser extends DDParserSpec {
    public static final String EJBJAR_DTD_PUBLIC_ID_11 =
        "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
    public static final String EJBJAR_DTD_PUBLIC_ID_20 =
        "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";

    public static VersionData[] VERSION_DATA = {
        new VersionData("1.1", EJBJAR_DTD_PUBLIC_ID_11, null, EJBJar.VERSION_1_1, VERSION_1_2_INT),
        new VersionData("2.0", EJBJAR_DTD_PUBLIC_ID_20, null, EJBJar.VERSION_2_0, VERSION_1_3_INT),

        new VersionData("2.1", null, NAMESPACE_SUN_J2EE,   EJBJar.VERSION_2_1, VERSION_1_4_INT),
        new VersionData("3.0", null, NAMESPACE_SUN_JAVAEE, EJBJar.VERSION_3_0, VERSION_5_0_INT),
        new VersionData("3.1", null, NAMESPACE_SUN_JAVAEE, EJBJar.VERSION_3_1, VERSION_6_0_INT),
        new VersionData("3.2", null, NAMESPACE_JCP_JAVAEE, EJBJar.VERSION_3_2, VERSION_7_0_INT),

        new VersionData("4.0", null, NAMESPACE_JAKARTA, EJBJar.VERSION_4_0, VERSION_9_0_INT),
    };

    public static int getMaxTolerated() {
        return EJBJar.VERSION_4_0;
    }
    
    public static int getMaxImplemented() {
        return EJBJar.VERSION_4_0;
    }
    
    @Override
    protected VersionData[] getVersionData() {
        return VERSION_DATA;
    }

    protected static int adjustSchemaVersion(int maxSchemaVersion) {
        return ( (maxSchemaVersion < EJBJar.VERSION_3_1) ? EJBJar.VERSION_3_1 : maxSchemaVersion );
    }

    public EJBJarDDParser(Container ddRootContainer, Entry ddEntry, int maxSchemaVersion) throws ParseException {
        super( ddRootContainer, ddEntry,
               adjustSchemaVersion(maxSchemaVersion),
               "ejb-jar" );
    }

    @Override
    public EJBJarType parse() throws ParseException {
        return (EJBJarType) super.parse();
    }

    @Override    
    protected EJBJarType createRootElement() {
        return new EJBJarType( getDeploymentDescriptorPath() );
    }
}
