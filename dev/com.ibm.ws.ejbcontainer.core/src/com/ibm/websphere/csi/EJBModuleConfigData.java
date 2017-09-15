/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
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
 * <code>EJBModuleConfigData</code> instances contain all the
 * ConfigData required
 * by a container to correctly install and manage an EJB module. <p>
 */

public interface EJBModuleConfigData
                extends java.io.Serializable
{
    /**
     * Returns the EJBJar object for the module
     */
    public Object getModule();

    /**
     * getModuleBinding returns EJBJarBinding
     */
    public Object getModuleBinding();

    /**
     * getModuleExtension returns the EJBJarExtension object associated with
     * this module
     */
    public Object getModuleExtension();

}
