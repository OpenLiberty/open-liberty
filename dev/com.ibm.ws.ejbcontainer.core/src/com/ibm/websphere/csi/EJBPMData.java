/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
 * The <code>EJBPMData</code> interface is a marker interface
 * identifies the information needed by PM for each EJB type during
 * module start. <p>
 * 
 * <code>EJBPMData</code> instances contain all the ConfigData required
 * by PM to correctly install and manage an EJB. There is a
 * single <code>EJBPMData</code> instance per home. <p>
 * 
 */

public interface EJBPMData extends java.io.Serializable {
    /**
     * isLazyInitialization flag indicates to PM when
     * "quick start" mode is in force.
     */
    public boolean islazyInitialization();

    /**
     * getDeploymentData returns the EnterpriseBean object
     * for the particular EJB type.
     */
    public Object getDeploymentData();

    /**
     * getDeploymentExtn returns EnterpriseBeanExtension which has
     * Websphere related extentions
     */
    public Object getDeploymentExtn();

    /**
     * getClassLoader returns the ClassLoader for the bean.
     */
    public ClassLoader getClassLoader();

    /**
     * getCMPConnectionFactoryLookupName returns the String which can be
     * used to lookup the CMP 2.0 Connection Factory (Essentially a constant
     * defined in EJBConfigDataImpl).
     */
    public String getCMPConnectionFactoryLookupName();

    public String getConnectionFactoryName();

}
