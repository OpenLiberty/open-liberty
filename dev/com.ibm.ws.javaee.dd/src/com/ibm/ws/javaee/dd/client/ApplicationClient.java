/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.client;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.ModuleDeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;

/**
 * The application-client element is the root element of an
 * application client deployment descriptor. The application
 * client deployment descriptor describes the EJB components
 * and external resources referenced by the application
 * client.
 */
public interface ApplicationClient extends ModuleDeploymentDescriptor, DeploymentDescriptor, JNDIEnvironmentRefsGroup {

    static final String DD_NAME = "META-INF/application-client.xml";

    int VERSION_1_2 = 12;
    int VERSION_1_3 = 13;
    int VERSION_1_4 = 14;
    int VERSION_5 = 50;
    int VERSION_6 = 60;
    int VERSION_7 = 70;
    int VERSION_8 = 80;
    int VERSION_9 = 90;

    public int[] VERSIONS = {
            VERSION_1_2, VERSION_1_3, // DTD versions
            VERSION_1_4, // sun.j2ee
            VERSION_5, VERSION_6, // sun.javaee
            VERSION_7, VERSION_8, // jcp.j2ee
            VERSION_9, // jakarta
    };

    int getVersionID();

    @Override
    List<EJBRef> getEJBLocalRefs();

    @Override
    List<PersistenceContextRef> getPersistenceContextRefs();

    String getCallbackHandler();

    List<MessageDestination> getMessageDestinations();

    String getVersion();

    boolean isSetMetadataComplete();    
    boolean isMetadataComplete();
}
