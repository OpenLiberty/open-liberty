/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.management.j2ee;

/**
 * Identifies a J2EE application EAR that has been deployed.
 */
public interface J2EEApplicationMBean extends J2EEDeployedObjectMBean {

    /**
     * A list of J2EEModules that comprise this application. For each J2EE module
     * that is utilized by this application, there must be one J2EEModule
     * OBJECT_NAME in the modules list that identifies it.
     */
    String[] getmodules();

}
