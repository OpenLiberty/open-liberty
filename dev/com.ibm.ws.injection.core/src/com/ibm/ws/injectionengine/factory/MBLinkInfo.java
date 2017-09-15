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
package com.ibm.ws.injectionengine.factory;

import java.io.Serializable;

/**
 * ManagedBean link information for ManagedBean reference binding resolution.
 * Used to support auto-link XML or the @Resource annotation. <p>
 *
 * This class is used to hold the information needed to resolve a JNDI lookup
 * in the java:comp name space that a component may do to get a reference
 * to a ManagedBean instance. <p>
 *
 * This class is NOT used when a binding override has been provided.
 * When a binding has been provided, the built in naming indirect
 * lookup support is used. <p>
 */
public class MBLinkInfo implements Serializable
{
    private static final long serialVersionUID = 5242854726536988229L;

    /**
     * Name of the resource-ref.
     */
    final String ivRefName;

    /**
     * Application name of the referencing bean, NOT the referenced bean.
     **/
    final String ivApplication;

    /**
     * Module name of the referencing bean, NOT the referenced bean.
     **/
    final String ivModule;

    /**
     * Component name of the referencing bean, NOT the referenced bean.
     */
    final String ivComponent;

    /**
     * The referenced managed bean type.
     */
    final String ivBeanType;

    /**
     * Construct an instance for the specified reference to a managed bean.
     *
     * @param refName Name of the resource-ref.
     * @param application Application name of the referencing bean.
     * @param module Module name of the referencing bean.
     * @param component Component name of the referencing bean
     * @param beanType The referenced managed bean type.
     */
    public MBLinkInfo(String refName,
                      String application,
                      String module,
                      String component,
                      String beanType)
    {
        ivRefName = refName;
        ivApplication = application;
        ivModule = module;
        ivComponent = component;
        ivBeanType = beanType;
    }

    @Override
    public String toString()
    {
        return super.toString() + '[' + ivRefName +
               ", " + ivApplication + "#" + ivModule + "#" + ivComponent +
               ", " + ivBeanType + ']';
    }
}
