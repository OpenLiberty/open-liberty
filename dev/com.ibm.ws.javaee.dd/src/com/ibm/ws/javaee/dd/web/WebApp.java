/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.dd.web.common.WebCommon;

public interface WebApp extends ModuleDeploymentDescriptor, DeploymentDescriptor, WebCommon {
    String DD_NAME = "WEB-INF/web.xml";

    int VERSION_2_2 = 22;
    int VERSION_2_3 = 23;
    int VERSION_2_4 = 24;
    int VERSION_2_5 = 25;    
    int VERSION_3_0 = 30;
    int VERSION_3_1 = 31;
    int VERSION_4_0 = 40; // JavaEE
    int VERSION_5_0 = 50; // Jakarta

    int[] VERSIONS = { 
        VERSION_2_2, VERSION_2_3,
        VERSION_2_4,
        VERSION_2_5, VERSION_3_0,
        VERSION_3_1, VERSION_4_0,
        VERSION_5_0
    };

    String getVersion();

    boolean isSetMetadataComplete();
    boolean isMetadataComplete();

    @Override
    String getModuleName();

    AbsoluteOrdering getAbsoluteOrdering();
}
