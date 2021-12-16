/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

    static final String WEB_DD_NAME = "WEB-INF/webservices.xml";
    static final String EJB_DD_NAME = "META-INF/webservices.xml";

    public String getVersion();

    public List<WebserviceDescription> getWebServiceDescriptions();
}
