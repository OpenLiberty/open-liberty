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
package com.ibm.ws.javaee.dd.app;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.SecurityRole;

public interface Application extends DeploymentDescriptor, DescriptionGroup, JNDIEnvironmentRefs {
    static final String DD_NAME = "META-INF/application.xml";

    int VERSION_1_2 = 12;
    int VERSION_1_3 = 13;
    int VERSION_1_4 = 14;
    int VERSION_5 = 50;
    int VERSION_6 = 60;
    int VERSION_7 = 70;
    int VERSION_8 = 80;
    int VERSION_9 = 90;

    String getVersion();

    String getApplicationName();

    boolean isSetInitializeInOrder();
    boolean isInitializeInOrder();

    List<Module> getModules();

    List<SecurityRole> getSecurityRoles();

    String getLibraryDirectory();

    List<MessageDestination> getMessageDestinations();
}
