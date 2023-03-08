/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.keyring.saf;

/**
 * Management interface for the MBean "WebSphere:service=com.ibm.websphere.security.keyring.saf.KeyringNotificationMBean"
 * The Liberty profile makes this MBean available so that it is used to trigger the refresh of SAF
 * based keyrings used in the Liberty configuration.
 * 
 * @ibm-api
 */
public interface KeyringNotificationMBean {

    /**
     * This is the name to be used to register and to look up the MBean.
     * It should match the <code>jmx.objectname</code> property in the
     * bnd.bnd file for the component that provides this interface.
     */
    String INSTANCE_NAME = "WebSphere:service=com.ibm.websphere.security.keyring.saf.KeyringNotificationMBean";

    /**
     * Method to call the update of SAF keyrings
     *
     * @param id - The config id of the keystore from server.xml to be refresh if
     *            null all keystore config ID for keyrings found in server.xml will be refreshed.
     */
    boolean refreshRequested(String id);

}
