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
 * Identifies a Java VM being utilized by a server. For each Java VM which is running
 * threads associated with the J2EE server, its containers or its resources, there must
 * be one managed object that implements the JVM model. A JVM managed object
 * must be removed when the Java VM it manages is no longer running.
 */
public interface JVMMBean extends J2EEManagedObjectMBean {

    /**
     * Identifies the Java Runtime Environment version of this Java VM. The value
     * of javaVersion must be identical to the value of the system property java.version.
     */
    String getjavaVersion();

    /**
     * Identifies the Java Runtime Environment vendor of this Java VM. The value
     * of javaVendor must be identical to the value of the system property
     * java.vendor.
     */
    String getjavaVendor();

    /**
     * Identifies the node (machine) this JVM is running on. The value of the node
     * attribute must be the fully quailified hostname of the node the JVM is running on.
     */
    String getnode();

}
