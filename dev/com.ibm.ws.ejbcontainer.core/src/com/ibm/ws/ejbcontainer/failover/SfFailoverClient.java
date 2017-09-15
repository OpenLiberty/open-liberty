/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.failover;

import com.ibm.ejs.container.BeanId;

public interface SfFailoverClient
{
    /**
     * Used by failover client to indicate SFSB is now activated.
     * 
     * @param beanId for the SFSB. *
     * 
     * @param lastAccessTime is the last access time for this SFSB.
     */
    void activated(BeanId beanId, long lastAccessTime);

    /**
     * Failover client uses this method to update failover entry when
     * a SFSB is passivated.
     * 
     * @param BeanId for the SFSB.
     * 
     * @param data is the byte[] object that contains the serialized
     *            data when a SFSB is passivated.
     * 
     * @param lastAccessTime is the last access time for this SFSB.
     */
    void passivated(BeanId beanId, byte[] data, long lastAccessTime);

    /**
     * Failover client uses this method to create a failover entry when
     * a SFSB is created. The failover client is required to call this
     * method prior to the created SFSB is passivated.
     * 
     * @param beanId for the SFSB.
     * @param is the timeout value configured for SFSB in milliseconds.
     */
    void createEntry(BeanId beanId, long timeout);

    /**
     * Failover client uses to remove a failover entry for a SFSB when the
     * SFSB is destroyed or timed out.
     * 
     * @param BeanId for the SFSB.
     */
    void removeEntry(BeanId beanId);

    /**
     * Failover client uses to get WLM Identity object to use in IOR.
     * 
     * @param BeanId for the SFSB.
     */
    Object getWLMIdentity(BeanId beanId);

    /**
     * Used by failover client to indicate SFSB is enlisted in a
     * a sticky UOW that is active or to indicate the sticky UOW
     * has completed.
     * 
     * @param beanId for the SFSB.
     * @param isActive must be true if SFSB is enlisted in an active
     *            sticky UOW and false if the sticky UOW completed.
     */
    void stickyUOW(BeanId beanId, boolean isActive);
}
