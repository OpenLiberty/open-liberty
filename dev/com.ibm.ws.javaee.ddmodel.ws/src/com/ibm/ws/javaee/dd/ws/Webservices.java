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
package com.ibm.ws.javaee.dd.ws;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;

public interface Webservices extends DeploymentDescriptor, DescriptionGroup {
    String WEB_DD_NAME = "WEB-INF/webservices.xml";
    String EJB_DD_NAME = "META-INF/webservices.xml";

    int VERSION_1_1 = 11;
    int VERSION_1_2 = 12;
    int VERSION_1_3 = 13;
    int VERSION_1_4 = 14;
    int VERSION_2_0 = 20;

    String VERSION_1_1_STR = "1.1";    
    String VERSION_1_2_STR = "1.2";
    String VERSION_1_3_STR = "1.3";
    String VERSION_1_4_STR = "1.4";
    String VERSION_2_0_STR = "2.0";

    String getVersion();

    List<WebserviceDescription> getWebServiceDescriptions();
}
