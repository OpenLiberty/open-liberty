/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import javax.ejb.EJBException;

public interface TxAttrMixedExpOverrideRemote {
    /**
     * The SuperClass has class level demarcation of TX attr = NEVER. The
     * SuperClass's method scObcClassExp TX attr should be implicitly set
     * (defaulted) to NEVER. The BaseClass (BC) is explicitly set to a TX attr
     * of REQUIRES_NEW at the class level. The BaseClass's method scObcClassExp
     * TX attr should be implicitly set (defaulted) to REQUIRES_NEW.
     *
     * The BaseClass should explicitly override (via its class level demarcation
     * of REQUIRES_NEW) the SuperClass's class level TX Attr demarcation of
     * NEVER for this method.
     *
     * To verify this, when a method with a REQUIRES_NEW transaction attribute
     * is called while the calling thread is currently associated with a global
     * transaction it causes the container to dispatch the method in the a new
     * global transaction context (e.g container does begin a new global
     * transaction). The caller must begin a global transaction prior to calling
     * this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return String override = "BC" if method is dispatched in a global
     *         tranaction with a global transaction ID that does not match the
     *         tid parameter. Otherwise String override = "Failure: The value of
     *         the transaction attribute for the Base Class method,
     *         scObcClassExp(), should have been set to REQUIRES_NEW." is
     *         returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public String scObcClassExp(byte[] tid) throws EJBException;

    /**
     * The BaseClass (BC) is explicitly set to a TX attr of REQUIRES_NEW at the
     * class level. The BaseClass's method expClassOverriddenByExpMethod TX attr
     * is explicitly set to NEVER.
     *
     * The explicit method level demarcation of NEVER should override the
     * explicit class level TX Attr demarcation of REQUIRES_NEW.
     *
     * Since this method has a TX attribute explicitly set to NEVER and this
     * method will be called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException.
     *
     * The caller must begin a global transaction prior to calling this method.
     *
     * @return We should never enter this method due to the nature of the NEVER
     *         TX attribute, HOWEVER we will return a string if the method is
     *         used but it is not picking up the NEVER TX Attr demarcation.
     *
     * @throws javax.ejb.EJBException
     */
    public String expClassOverriddenByExpMethod() throws EJBException;
}