/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.tx.statlesmixsec.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

@Stateless(name = "TranAttrBean")
@Local(AsmDescTxAttrLocal.class)
public class AsmDescTxAttrBean {
    @TransactionAttribute(REQUIRES_NEW)
    public String xmlOverridesAnnTxAttr() {
        String result = "FAIL: This method call should have resulted "
                        + "in a javax.ejb.EJBTransactionRequiredException "
                        + "exception being thrown.";

        return result;
    }

    public String xmlOnlyTxAttr() {
        String result = "FAIL: This method call should have resulted "
                        + "in a javax.ejb.EJBTransactionRequiredException "
                        + "exception being thrown.";

        return result;
    }
}