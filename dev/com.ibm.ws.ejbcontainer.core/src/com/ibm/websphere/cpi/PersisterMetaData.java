/*******************************************************************************
 * Copyright (c) 1998, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cpi;

/**
 * Contains basic persister configuration data which is expected to
 * be useful for all types of persisters. Data specific to particular
 * backends must be provided via extensions. eg. JDBCPersisterMetaData
 * 
 * @see com.ibm.websphere.cpi.JDBCPersisterMetaData
 * 
 */
public interface PersisterMetaData {

    /**
     * @return (EnterpriseBean) MOF object of the associated bean
     */
    public Object getEnterpriseBean();

    /**
     * @return the ClassLoader that can load persister classes.
     */
    public ClassLoader getClassLoader();

    /**
     * @returns a PersisterConfigData that contains
     *          properties of the Perister
     */
    public PersisterConfigData getPersisterConfigData();
}
