/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.websphere.csi.CSIServant;
import com.ibm.websphere.csi.TransactionalObject;

/**
 * An <code>EJSRemoteWrapper</code> wraps a remote EJB and interposes
 * calls to the container before and after every method call on the
 * EJB. <p>
 *
 * The <code>EJSRemoteWrapper</code> is designed to extend the minimum amount
 * of state in EJSWrapperBase with only the additional state common to all
 * remote EJBs (2.1 Component and 3.0 Business). Note, it does not directly
 * maintain a reference to the EJB. It relies on the container to supply it
 * with the appropriate EJB. <p>
 *
 * NOTE: The deployed remote interface to a bean extends an EJSRemoteWrapper
 * instance. For that reason, EJSRemoteWrapper MUST NOT implement any
 * methods, including those in the EJBObject interface, since the
 * EJB 3.0 business interfaces do not implement EJBObject. Otherwise,
 * there would be a potential conflict between the methods on
 * the bean's remote interface and those on EJSRemoteWrapper.
 */
public abstract class EJSRemoteWrapper extends EJSWrapperBase
                implements CSIServant,
                TransactionalObject
{
    /**
     * Cluster Identity to be used for registerServant/export of this
     * Remote Wrapper. <p>
     *
     * May be be null for Stateful bean instances or when running
     * in a single server environment. <p>
     **/
    public Object ivCluster;

    /**
     * Member that can be used to cache the tie object associated with this
     * wrapper. This field is not used by distributed traditional WAS.
     */
    // LIDB2775-23.8
    public javax.rmi.CORBA.Tie intie;

    /**
     * Member that can be used to cache the stub/reference object associated
     * with this wrapper. This field is not used by distributed traditional WAS.
     */
    // LIDB2775-23.8
    public Object instub;

    /**
     * Return true iff this wrapper may participate in the WLM protocol.
     * NOTE: this code is now dead and should never be called as of
     * release 6.0. However, we need to leave here for backwards
     * compatibility.
     */
    @Override
    public boolean wlmable()
    {
        throw new ContainerEJBException("wlmable call not expected to occur.");
    } // wlmable

} // EJSRemoteWrapper
