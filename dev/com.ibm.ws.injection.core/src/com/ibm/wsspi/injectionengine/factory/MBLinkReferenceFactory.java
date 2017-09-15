/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine.factory;

import javax.naming.Reference;

/**
 * Instances of this interface are used to create Reference objects with
 * lookup information for MBLink References, which the caller then binds
 * to a JNDI name space. When the object is looked up, the associated
 * factory uses the MBLink information and component or environment
 * specific information to resolve the MBLink. <p>
 **/
public interface MBLinkReferenceFactory
{
    /**
     * This method creates an MBLink reference based on the application,
     * module, and component names of the referencing context and the
     * bean type of the referenced managed bean. <p>
     *
     * @param refName name of the resource-ref.
     * @param application name of the application containing the ref.
     * @param module name of the module containing the ref.
     * @param component name of the component containing the ref.
     * @param beanType type of the referenced managed bean.
     *
     * @return the created EJBLink Reference.
     **/
    public Reference createMBLinkReference(String refName,
                                           String application,
                                           String module,
                                           String component,
                                           String beanType);
}
