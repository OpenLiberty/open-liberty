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
 * The J2EEModule model is the base model for all of the J2EE Module types.
 * Managed objects that implement the J2EEModule model represent EAR, JAR,
 * WAR and RAR files that have been deployed.
 */
public interface J2EEModuleMBean extends J2EEDeployedObjectMBean {

    /**
     * Identifies the Java virtual machines on which this module is running. For each
     * JVM on which this module has running threads there must be one JVM
     * OBJECT_NAME in the javaVMs list that identifies it.
     * 
     * Each OBJECT_NAME in the J2EEModule javaVMs list must match one of
     * the Java VM names in the javaVMs attribute of the J2EEServer on which this
     * module is deployed.
     */
    String[] getjavaVMs();
}
