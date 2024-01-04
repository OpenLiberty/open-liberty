/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.app;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefs;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.SecurityRole;

public interface Application extends DeploymentDescriptor, DescriptionGroup, JNDIEnvironmentRefs {
    String DD_SHORT_NAME = "application.xml";
    String DD_NAME = "META-INF/application.xml";

    int VERSION_1_2 = 12;
    int VERSION_1_3 = 13;
    int VERSION_1_4 = 14;
    int VERSION_5 = 50;
    int VERSION_6 = 60;
    int VERSION_7 = 70;
    int VERSION_8 = 80;
    int VERSION_9 = 90;
    int VERSION_10 = 100;
    int VERSION_11 = 110;

    int[] VERSIONS = {
                       VERSION_1_2, VERSION_1_3, // dtd
                       VERSION_1_4, // sun j2ee
                       VERSION_5, VERSION_6, // sun javaee
                       VERSION_7, VERSION_8, // jcp java
                       VERSION_9, VERSION_10, VERSION_11 // Jakarta
    };

    String getVersion();

    String getApplicationName();

    boolean isSetInitializeInOrder();

    boolean isInitializeInOrder();

    List<Module> getModules();

    List<SecurityRole> getSecurityRoles();

    String getLibraryDirectory();

    List<MessageDestination> getMessageDestinations();
}
