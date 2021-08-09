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
 * The ResourceAdapterModule model identifies a deployed resource adapter archive
 * (RAR).
 */
public interface ResourceAdapterModuleMBean extends J2EEModuleMBean {

    /**
     * A list of resource adapters contained in this resource adapter module. For
     * each resource adapter contained in the deployed RAR module, there must be one
     * ResourceAdapter OBJECT_NAME in the resourceAdapters list that identifies
     * it.
     */
    String[] getresourceAdapters();

}
