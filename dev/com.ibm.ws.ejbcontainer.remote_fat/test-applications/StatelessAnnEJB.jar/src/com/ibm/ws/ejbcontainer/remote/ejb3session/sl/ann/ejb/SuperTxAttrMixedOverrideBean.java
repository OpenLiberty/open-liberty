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

import javax.ejb.TransactionAttribute;

/**
 * Bean implementation class for Enterprise Bean: SuperTxAttrMixedOverrideBean
 * This bean is used in override testing of Transaction Attributes at both the
 * class and method levels.
 **/
@TransactionAttribute(NEVER)
public class SuperTxAttrMixedOverrideBean {
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
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException if
     * not overridden by the BC. The caller must begin a global transaction
     * prior to calling this method.
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
    public String scObcClassImp(byte[] tid) {
        String override = "The SuperClass was used, but this message should never be seen because an error should be"
                          + "thrown if a method with the TX attr value = NEVER is called while associated with a global tran.";

        return override;
    }

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
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException if
     * not overridden by the BC. The caller must begin a global transaction
     * prior to calling this method.
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
    public String scObcClassExp(byte[] tid) {
        String override = "The SuperClass was used, but this message should never be seen because an error should be"
                          + "thrown if a method with the TX attr value = NEVER is called while associated with a global tran.";

        return override;

    }

    /**
     * The SuperClass has class level demarcation of TX attr = NEVER. The
     * SuperClass's method scObcMethExp TX attr should be implicitly set
     * (defaulted) to NEVER. The BaseClass (BC) is implicitly set to a TX attr
     * of REQUIRED at the class level. The BaseClass's method scObcClassExp TX
     * attr is explicitly set to REQUIRES_NEW.
     *
     * The BaseClass should explicitly override (via its method level
     * demarcation of REQUIRES_NEW) the SuperClass's class level TX Attr
     * demarcation of NEVER for this method.
     *
     * Since this is the SuperClass implementation with a TX attribute of NEVER
     * and the method is called while the thread is currently associated with a
     * global transaction the container will throw a javax.ejb.EJBException if
     * not overridden by the BC. The caller must begin a global transaction
     * prior to calling this method.
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
    public String scObcMethExp(byte[] tid) {
        String override = "The SuperClass was used, but this message should never be seen because an error should be"
                          + "thrown if a method with the TX attr value = NEVER is called while associated with a global tran.";

        return override;

    }
}