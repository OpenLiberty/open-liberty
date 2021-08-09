/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb;

import javax.ejb.EJBException;

public interface ExpMethodExpAllxmlLocal {
    /**
     * 1)Not taking the XML into account this method will explicitly have Tx
     * Attr of NEVER. 2)XML uses the * to set all methods to have the
     * trans-attribute of RequiresNew 3)The XML should take precedence
     *
     * To verify this, the caller must begin a global transaction prior to
     * calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return boolean true if a new global tran is created (tid's do not match)
     *         meaning the XML override worked. boolean false if the same tran
     *         is used (tid's match) meaning the XML override failed - this also
     *         means method level demarcation of NEVER failed as well.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean expMethodExpAllXML(byte[] tid) throws EJBException;
}