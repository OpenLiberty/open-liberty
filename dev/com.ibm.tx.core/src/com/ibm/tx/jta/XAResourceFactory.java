/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta;

import java.io.Serializable;

import javax.transaction.xa.XAResource;

/**
 * XAResourceFactory interface needs to be implemented by a XAResource 
 * provider if they want their XAResource objects to participate in JTA/or JTS
 * transactions managed by JTA TM. It is used by JTA TM to reproduce 
 * XAResource objects during XA recovery.
 *
 * <p> JTA TM should be able to use the following method to produce a 
 *     XAResource object:
 *
 *     <b>
 *     XAResourceFactory factory = (
 *         XAResourceFactory) Class.forName(XAResourceFactoryClass).newInstance();
 *	   XAResource xaresource = factory.getXAResource(XAResourceInfo);
 *     </b>
 *     where XAResourceFactoryClass is the class name of implementation 
 *     class of XAResourceFactory. XAResourceInfo is the necessary information
 *     object wrapper, which is also implemented by XAResource providers, for 
 *     XAResourceFactory to produce a XAResource object.
 *
 */
public interface XAResourceFactory
{
    /** 
     * Given XAResourceInfo, the XAResourceFactory produce a XAResource object.
     *
     * The only exception raised by getXAResource should be XAResourceNotAvailableException.  This
     * exception will be handled by the TM to indicate that the resource manager is not available
     * and recovery may not complete.  Any other exception raised by getXAResource will be 
     * caught by the TM and the server terminated as recovery cannot be guaranteed.  
     *
     * getXAResource may also be invoked during normal server running if a RM returns XAER_RMFAIL
     * on a completion method.  The TM will attempt to retry the completion method after obtaining
     * a new XAResource.  This retry processing will continue until either the retry limits are
     * exceeded, the resource manager allows completion to occur or a permananent error is reported.
     */
    public XAResource getXAResource(Serializable xaresinfo) throws XAResourceNotAvailableException;

    /**
     * Destroy the XAResource object. Internally, the XAResource provider
     * should cleanup resources used by XAResource object. For example, JTA
     * should close XAConnection.
     */
    public void destroyXAResource(XAResource xaRes) throws DestroyXAResourceException;	
}
