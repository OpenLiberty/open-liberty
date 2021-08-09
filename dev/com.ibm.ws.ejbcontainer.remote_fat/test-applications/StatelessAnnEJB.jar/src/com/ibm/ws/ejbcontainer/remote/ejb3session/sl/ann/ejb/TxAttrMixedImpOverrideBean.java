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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionManagementType.CONTAINER;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionManagement;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: TxAttrMixedIxpOverrideBean
 * This bean extends SuperTxAttrMixedOverrideBean.
 **/
@Stateless(name = "TxAttrMixedImpOverride")
@Remote(TxAttrMixedImpOverrideRemote.class)
@TransactionManagement(CONTAINER)
public class TxAttrMixedImpOverrideBean extends SuperTxAttrMixedOverrideBean {
    /**
     * The SuperClass has class level demarcation of TX attr = NEVER. The
     * SuperClass's method scObcClassImp TX attr should be implicitly set
     * (defaulted) to NEVER. The BaseClass (BC) is implicitly (defaults to) set
     * to a TX attr of REQUIRED at the class level. The BaseClass's method
     * scObcClassImp TX attr should be implicitly set (defaulted) to REQUIRED.
     *
     * The BaseClass should implicitly override the SuperClass's class level TX
     * Attr demarcation of NEVER for this method.
     *
     * To verify this, when a method with an implicitly/defaulted REQUIRED
     * transaction attribute is called while the calling thread is currently
     * associated with a global transaction it causes the container to dispatch
     * the method in the caller's global transaction context (e.g container does
     * NOT begin a new transaction). The caller must begin a global transaction
     * prior to calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return String override = "BC" if method is dispatched in the same
     *         transaction context with the same transaction ID as passed by tid
     *         parameter. Otherwise String override ="Failure: The value of the
     *         transaction attribute for the Base Class method, scObcClassImp(),
     *         was not implicitly set to REQUIRED" is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @Override
    public String scObcClassImp(byte[] tid) {
        String override = "Failure: The value of the transaction attribute for the Base Class method, scObcClassImp(), was not implicitly set to REQUIRED";
        if (FATTransactionHelper.isSameTransactionId(tid)) {
            override = "BC";
        }
        return override;
    }

    /**
     * The SuperClass has class level demarcation of TX attr = NEVER. The
     * SuperClass's method scObcMethExp TX attr should be implicitly set
     * (defaulted) to NEVER. The BaseClass (BC) is implicitly set to a TX attr
     * of REQUIRED at the class level. The BaseClass's method scObcMethExp TX
     * attr is explicitly set to REQUIRES_NEW.
     *
     * The BaseClass should explicitly override (via its method level
     * demarcation of REQUIRES_NEW) the SuperClass's class level TX Attr
     * demarcation of NEVER for this method.
     *
     * To verify this, when a method with an explicitly set REQUIRES_NEW
     * transaction attribute is called while the calling thread is currently
     * associated with a global transaction it causes the container to dispatch
     * the method in the a new global transaction context (e.g container does
     * begin a new global transaction). The caller must begin a global
     * transaction prior to calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return String override = "BC" if method is dispatched in a global
     *         tranaction with a global transaction ID that does not match the
     *         tid parameter. Otherwise String override = "Failure: The value of
     *         the transaction attribute for the Base Class method,
     *         scObcMethExp(),should have been set to REQUIRES_NEW." is
     *         returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public String scObcMethExp(byte[] tid) {
        String override = "Failure: The value of the transaction attribute for the Base Class method, scObcMethExp(),should have been set to REQUIRES_NEW.";
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return override = "Failure: myTid == null.  This should not be the case.";
        }

        if (FATTransactionHelper.isSameTransactionId(tid) == false) {
            override = "BC";
        }

        return override;
    }
}