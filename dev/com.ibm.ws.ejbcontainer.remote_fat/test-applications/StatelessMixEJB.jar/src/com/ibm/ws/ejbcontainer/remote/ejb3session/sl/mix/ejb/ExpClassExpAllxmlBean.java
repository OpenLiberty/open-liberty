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

import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionManagementType.CONTAINER;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: ExpClassExpAllxmlBean
 **/
@Local(ExpClassExpAllxmlLocal.class)
@Remote(ExpClassExpAllxmlRemote.class)
@TransactionManagement(CONTAINER)
@TransactionAttribute(NEVER)
public class ExpClassExpAllxmlBean {
    /**
     * 1)Not taking the XML into account the bean and its methods will
     * explicitly have Tx Attr of NEVER as set at the class level. 2)XML uses
     * the * to set all methods to have the trans-attribute of RequiresNew 3)The
     * XML should take precedence
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
     *         means class level demarcation of NEVER failed as well.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public boolean expClassExpAllXML(byte[] tid) {
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return false;
        }

        return (FATTransactionHelper.isSameTransactionId(tid) == false);
    }

    /**
     * 1)Not taking the XML into account this method will explicitly be set to
     * have Tx Attr of REQUIRED. 2)XML uses the * to set all methods to have the
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
     *         is used (tid's match) meaning the XML override failed.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @TransactionAttribute(REQUIRED)
    public boolean expClassExpMethodExpAllXML(byte[] tid) {
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return false;
        }

        return (FATTransactionHelper.isSameTransactionId(tid) == false);
    }
}