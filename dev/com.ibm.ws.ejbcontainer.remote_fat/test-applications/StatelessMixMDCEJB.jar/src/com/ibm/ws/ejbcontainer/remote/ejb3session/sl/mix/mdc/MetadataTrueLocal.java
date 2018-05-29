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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.mdc;

import javax.ejb.EJBException;

public interface MetadataTrueLocal {
    /**
     * Used to verify when a method with a REQUIRES_NEW transaction attribute is
     * called while calling thread is currently associated with a global
     * transaction causes the container to dispatch the method in the a new
     * global transaction context (e.g container does begin a new global
     * transaction). The caller must begin a global transaction prior to calling
     * this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if method is dispatched in a global transaction with
     *         a global transaction ID the does not match the tid parameter.
     *         Otherwise boolean false is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean txRequiresNew(byte[] tid) throws EJBException;
}