/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * J2EEName instances are used to encapsulate the Application-Module-Component name for
 * uniquely identifying EJBs in an application. An object implementing this interface must
 * also correctly implement the hashCode and equals methods.
 * 
 * @ibm-private-in-use
 */
public interface J2EEName extends java.io.Serializable {

    /**
     * Returns a J2EEName in the format app#mod#comp for a component,
     * app#mod for a module, or app for an application.
     */
    @Override
    public String toString();

    /**
     * Returns the application name.
     * 
     * @return application name
     */
    public String getApplication();

    /**
     * Returns the module name, or null for J2EENames representing applications.
     * 
     * @return module name, or null
     */
    public String getModule();

    /**
     * Returns the component name, or null for J2EENames representing
     * applications or modules.
     * 
     * @return component name, or null
     */
    public String getComponent();

    /**
     * Returns a serialized name that can be passed in to {@link J2EENameFactory#create(byte[]) to create the name.
     */
    public byte[] getBytes();

}
