/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.cache;

/**
 * MBean to removeAllEntries in the Auth Cache.
 */
public interface DeleteAuthCache {

    /**
     * This is the name to be used to register and to look up the MBean.
     * It should match the <code>jmx.objectname</code> property in the
     * bnd.bnd file for the component that provides this interface.
     */
    String INSTANCE_NAME = "WebSphere:service=com.ibm.ws.security.authentication.cache.DeleteAuthCache";

    public void removeAllEntries();

}
