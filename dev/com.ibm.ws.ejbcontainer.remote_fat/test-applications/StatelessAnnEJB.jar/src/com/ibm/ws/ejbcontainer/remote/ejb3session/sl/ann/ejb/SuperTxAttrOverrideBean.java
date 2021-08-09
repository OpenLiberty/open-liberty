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

import static javax.ejb.TransactionAttributeType.NEVER;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import javax.ejb.TransactionAttribute;

import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;

/**
 * Bean implementation class for Enterprise Bean: SuperTxAttrOverrideBean This
 * bean will extend SuperDuperTxAttrOverrideBean This bean is used in override
 * testing of Transaction Attributes at the method level.
 **/
public class SuperTxAttrOverrideBean extends SuperDuperTxAttrOverrideBean {
    /**
     * Used to verify that when a method has an implicit (defaulted) transaction
     * attribute value of REQUIRED in the SuperDuperClass (SDC), then an
     * explicitly set value of NEVER in the SuperClass(SC), and finally an
     * explicitly set value of REQUIRES_NEW on the base class(BC) method, the
     * base class value is used.
     *
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException if
     * not overridden by BC. The caller must begin a global transaction prior to
     * calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperClass.
     * @return We should never enter this method due to the nature of the NEVER
     *         TX attribute. The return value of string is only because it is
     *         needed in the base class.
     *
     * @throws javax.ejb.EJBException
     */
    @Override
    @TransactionAttribute(NEVER)
    public String defaultSDCoverrideSCBC(byte[] tid) {
        // Note: this method should never be entered and thus the lines below
        // have no purpose other
        // than the method needs to return a String at the base class level
        String override = "Override failed because it used the SuperClass as opposed to the Base Class.";
        return override;
    }

    /**
     * Used to verify that when a method has an explicit transaction attribute
     * value of MANDATORY in the SuperDuperClass (SDC), then an explicitly set
     * value of NEVER in the SuperClass(SC), and finally an explicitly set value
     * of REQUIRES_NEW on the base class(BC) method, the base class value is
     * used.
     *
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException if
     * not overridden by BC. The caller must begin a global transaction prior to
     * calling this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperClass.
     * @return We should never enter this method due to the nature of the NEVER
     *         TX attribute. The return value of string is only because it is
     *         needed in the base class.
     *
     * @throws javax.ejb.EJBException
     */
    @Override
    @TransactionAttribute(NEVER)
    public String explicitSDCoverrideSCBC(byte[] tid) {
        // Note: this method should never be entered and thus the lines below
        // have no purpose other
        // than the method needs to return a String at the base class level
        String override = "Override failed because it used the SuperClass as opposed to the Base Class.";
        return override;
    }

    /**
     * Used to verify that when a method has an explicitly set value of
     * REQUIRES_NEW in the SuperDuperClass(SDC) and an implicitly set value of
     * REQUIRED (default) on the Super class(SC) method, the Super class value
     * is used NOT the Super Duper class level value.
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
     * @return String override = "SC" if the method is dispatched in the same
     *         transaction. Otherwise String override = "Failure: The value of
     *         the transaction attribute for the SuperClass method,
     *         explicitSDCimpOverrideSC(), was not implicitly set to REQUIRED"
     *         is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @Override
    public String explicitSDCimpOverrideSC(byte[] tid) {
        String override = "Failure: The value of the transaction attribute for the SuperClass method, explicitSDCimpOverrideSC(), was not implicitly set to REQUIRED";

        if (FATTransactionHelper.isSameTransactionId(tid)) {
            override = "SC";
        }

        return override;
    }

    /**
     * Used to verify that when a method has an implicit (defaulted) transaction
     * attribute value of REQUIRED in the SuperClass (SC) and an explicitly set
     * value of REQUIRES_NEW on the base class(BC) method, the base class value
     * is used.
     *
     * Since this is the SuperClass implementation we will return a string
     * stating the fact that the base class did not override the SuperClass's
     * implementation of this method.
     *
     * @param tid
     *            is the global transaction ID for the transaction that was
     *            started prior to calling this method. This is used in the base
     *            class, but not used in the SuperClass.
     *
     * @return String contianing failure message.
     *
     */
    public String defaultSCoverrideBC(byte[] tid) {
        String override = "Override failed because it used the SuperClass as opposed to the Base Class.";
        return override;
    }

    /**
     * Used to verify that when a method with an implicit/defaulted REQUIRED
     * transaction attribute is called while the calling thread is not currently
     * associated with a transaction context it causes the container to begin a
     * global transaction.
     *
     * @return String override = "SC" if the method is dispatched in a global
     *         transaction. String override = "Failure: The value of the
     *         transaction attribute for the SuperClass method, defaultSC(), was
     *         not implicitly set to REQUIRED" if method is dispatched in a
     *         local transaction.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    public String defaultSC() {
        String override = "Failure: The value of the transaction attribute for the SuperClass method, defaultSC(),was not implicitly set to REQUIRED";

        if (FATTransactionHelper.isTransactionGlobal()) {
            override = "SC";
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
     * @return String override = "SC" if method is dispatched in a global
     *         transaction with a global transaction ID that does not match the
     *         tid parameter. Otherwise String override = "Failure: The value of
     *         the transaction attribute for the SuperClass method,
     *         explicitSC(),should have been set to REQUIRES_NEW." is returned.
     *
     * @throws java.lang.IllegalStateException
     *             is thrown if method is dispatched while not in any
     *             transaction context.
     */
    @TransactionAttribute(REQUIRES_NEW)
    public String explicitSC(byte[] tid) {
        String override = "Failure: The value of the transaction attribute for the SuperClass method, explicitSC(),should have been set to REQUIRES_NEW.";
        byte[] myTid = FATTransactionHelper.getTransactionId();
        if (myTid == null) {
            return override = "Failure: myTid == null.  This should not be the case.";
        }

        if (FATTransactionHelper.isSameTransactionId(tid) == false) {
            override = "SC";
        }

        return override;
    }
}