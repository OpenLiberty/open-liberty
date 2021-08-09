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

import static javax.ejb.TransactionAttributeType.MANDATORY;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: SuperDuperTxAttrOverrideBean
 * This bean is used in override testing of Transaction Attributes at the method
 * level.
 **/
public class SuperDuperTxAttrOverrideBean {
    /**
     * Used to verify that when a method has an implicit (defaulted) transaction
     * attribute value of REQUIRED in the SuperDuperClass (SDC), then an
     * explicitly set value of NEVER in the SuperClass(SC), and finally an
     * explicitly set value of REQUIRES_NEW on the base class(BC) method, the
     * base class value is used.
     *
     * Since this is the SuperDuperClass implementation we will return a string
     * stating the fact that the base class did not override the
     * SuperDuperClass's implementation of this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperDuperClass.
     *
     * @return String containing failure message.
     *
     */
    public String defaultSDCoverrideSCBC(byte[] tid) {
        String override = "Override failed because it used the SuperDuperClass as opposed to the Base Class.";
        return override;
    }

    /**
     * Used to verify that when a method has an explicit transaction attribute
     * value of MANDATORY in the SuperDuperClass (SDC), then an explicitly set
     * value of NEVER in the SuperClass(SC), and finally an explicitly set value
     * of REQUIRES_NEW on the base class(BC) method, the base class value is
     * used.
     *
     * Since this is the SuperDuperClass implementation we will return a string
     * stating the fact that the base class did not override the
     * SuperDuperClass's implementation of this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperDuperClass.
     *
     * @return String containing failure message.
     *
     */
    @TransactionAttribute(MANDATORY)
    public String explicitSDCoverrideSCBC(byte[] tid) {
        String override = "Override failed because it used the SuperDuperClass as opposed to the Base Class.";
        return override;
    }

    /**
     * Used to verify that when a method has an explicitly set value of
     * REQUIRES_NEW in the SuperDuperClass(SDC) and an implicitly set value of
     * REQUIRED (default) on the Super class(SC) method, the Super class value
     * is used NOT the Super Duper class level value.
     *
     * Since this is the SuperDuperClass implementation we will return a string
     * stating the fact that the SuperClass did not override the
     * SuperDuperClass's implementation of this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the
     *            SuperClass, but no used in this SuperDuperClass.
     *
     * @return String containing failure message.
     *
     */
    @TransactionAttribute(REQUIRES_NEW)
    public String explicitSDCimpOverrideSC(byte[] tid) {
        String override = "Override failed because it used the SuperDuperClass as opposed to the SuperClass.";
        return override;
    }

    /**
     * Used to verify that when a method has an explicitly set value of
     * REQUIRES_NEW in the SuperDuperClass(SDC) and an implicitly set value of
     * REQUIRED (default) on the base class(BC) method, the base class value is
     * used.
     *
     * Since this is the SuperDuperClass implementation we will return a string
     * stating the fact that the base class did not override the
     * SuperDuperClass's implementation of this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperDuperClass.
     *
     * @return String containing failure message.
     *
     */
    @TransactionAttribute(REQUIRES_NEW)
    public String explicitSDCimpOverrideBC(byte[] tid) {
        String override = "Override failed because it used the SuperDuperClass as opposed to the SuperClass.";
        return override;
    }

    /**
     * Used to verify that when a method with an implicit/defaulted REQUIRED
     * transaction attribute is called while the calling thread is not currently
     * associated with a transaction context it causes the container to begin a
     * global transaction.
     *
     * @return String override = "SDC" if the method is dispatched in a global
     *         transaction. String override = "Failure: The value of the
     *         transaction attribute for the SuperDuperClass method,
     *         defaultSDC(), was not implicitly set to REQUIRED" if method is
     *         dispatched in a local transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public String defaultSDC() {
        String override = "Failure: The value of the transaction attribute for the SuperDuperClass method, defaultSDC(), was not implicitly set to REQUIRED";

        if (FATTransactionHelper.isTransactionGlobal()) {
            override = "SDC";
            return override;
        }

        return override;
    }

    /**
     * Used to verify when a method with an explicitly set REQUIRES_NEW
     * transaction attribute is called while calling thread is currently
     * associated with a global transaction it causes the container to dispatch
     * the method in the a new global transaction context (e.g container does
     * begin a new global transaction). The caller must begin a global
     * transaction prior to calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method.
     *
     * @return String override = "SDC" if method is dispatched in a global
     *         tranaction with a global transaction ID that does not match the
     *         tid parameter. Otherwise String override = "Failure: The value of
     *         the transaction attribute for the SuperDuperClass method,
     *         explicitSDC(),should have been set to REQUIRES_NEW." is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @TransactionAttribute(REQUIRES_NEW)
    public String explicitSDC(byte[] tid) {
        String override = "Failure: The value of the transaction attribute for the SuperDuperClass method, explicitSDC(),should have been set to REQUIRES_NEW.";
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return override = "Failure: myTid == null.  This should not be the case.";
        }

        if (FATTransactionHelper.isSameTransactionId(tid) == false) {
            override = "SDC";
        }

        return override;
    }
}