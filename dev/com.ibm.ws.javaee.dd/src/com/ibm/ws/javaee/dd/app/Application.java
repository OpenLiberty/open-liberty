/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

/**
 *
 */
public interface Application
                extends DeploymentDescriptor, DescriptionGroup, JNDIEnvironmentRefs {
    static final String DD_NAME = "META-INF/application.xml";

    /**
     * @return version="..." attribute value
     */
    String getVersion();

    /**
     * @return &lt;application-name>, or null if unspecified
     */
    String getApplicationName();

    /**
     * @return true if &lt;initialize-in-order> is specified
     * @see #isInitializeInOrder
     */
    boolean isSetInitializeInOrder();

    /**
     * @return &lt;initialize-in-order> if specified
     * @see #isSetInitializeInOrder
     */
    boolean isInitializeInOrder();

    /**
     * @return &lt;module> as a read-only list
     */
    List<Module> getModules();

    /**
     * @return &lt;security-role> as a read-only list
     */
    List<SecurityRole> getSecurityRoles();

    /**
     * @return &lt;library-directory>, or null if unspecified
     */
    String getLibraryDirectory();

    /**
     * @return &lt;message-destination> as a read-only list
     */
    List<MessageDestination> getMessageDestinations();
}
