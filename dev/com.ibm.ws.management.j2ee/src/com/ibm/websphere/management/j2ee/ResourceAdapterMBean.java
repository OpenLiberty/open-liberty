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
 * Identifies a deployed resource adapter.
 */
public interface ResourceAdapterMBean extends J2EEManagedObjectMBean {

    /**
     * The value of jcaResource must be a JCAResource OBJECT_NAME that
     * identifies the JCA connector resource implemented by this ResourceAdapter.
     */
    String getjcaResource();

}
