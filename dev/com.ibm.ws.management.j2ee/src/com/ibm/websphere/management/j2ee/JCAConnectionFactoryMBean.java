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
 * Identifies a connection factory. For each connection factory available to a server,
 * there must be one managed object that implements the JCAConnectionFactory
 * model.
 */
public interface JCAConnectionFactoryMBean extends J2EEManagedObjectMBean {

    /**
     * The value of managedConnectionFactory must be a
     * JCAManagedConnectionFactory OBJECT_NAME that identifies the managed
     * connection factory associated with the corresponding connection factory.
     */
    String getmanagedConnectionFactory();

}
