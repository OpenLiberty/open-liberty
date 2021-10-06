/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.misc.ejb;

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import org.junit.Assert;

import com.ibm.websphere.ejbcontainer.EJBContextExtension;
import com.ibm.websphere.ejbcontainer.SessionContextExtension;

/**
 * Basic session bean verifying injection and use of the EJBContextExtension API.
 */
@Stateless
public class EJBContextExtensionBean {

    @Resource
    SessionContext sessionContext;

    @Resource
    EJBContextExtension ejbContextExt;

    @Resource
    SessionContextExtension sessionContextExt;

    @TransactionAttribute(REQUIRED)
    public void verifyEJBContextExtensionWithRequired() {
        Assert.assertNotNull("Injected SessionContext was null", sessionContext);
        Assert.assertEquals("Injected SessionContext does not equal EJBContextExtension", sessionContext, ejbContextExt);
        Assert.assertEquals("Injected SessionContext does not equal SessionContextExtension", sessionContext, sessionContextExt);
        Assert.assertTrue("EJBContextExt.isTranactionGlobal() did not return true for REQUIRED method", ejbContextExt.isTransactionGlobal());
    }

    @TransactionAttribute(NOT_SUPPORTED)
    public void verifyEJBContextExtensionWithNotSupported() {
        Assert.assertNotNull("Injected SessionContext was null", sessionContext);
        Assert.assertEquals("Injected SessionContext does not equal EJBContextExtension", sessionContext, ejbContextExt);
        Assert.assertEquals("Injected SessionContext does not equal SessionContextExtension", sessionContext, sessionContextExt);
        Assert.assertFalse("EJBContextExt.isTranactionGlobal() did not return false for NOT_SUPPORTED method", ejbContextExt.isTransactionGlobal());
    }

}
