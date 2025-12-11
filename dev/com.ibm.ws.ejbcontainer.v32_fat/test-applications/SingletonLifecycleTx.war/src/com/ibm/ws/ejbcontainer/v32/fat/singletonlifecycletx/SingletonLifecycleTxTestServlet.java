/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.v32.fat.singletonlifecycletx;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.wsspi.uow.UOWManager;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/*")
@SuppressWarnings("serial")
public class SingletonLifecycleTxTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(SingletonLifecycleTxTestServlet.class.getName());

    private static String uowTypeToString(int uowType) {
        switch (uowType) {
            case UOWManager.UOW_TYPE_GLOBAL_TRANSACTION:
                return "global";
            case UOWManager.UOW_TYPE_LOCAL_TRANSACTION:
                return "local";
            case UOWManager.UOW_TYPE_ACTIVITYSESSION:
                return "activity";
        }
        return "unknown";
    }

    @Resource
    UserTransaction userTran;

    @Resource(lookup = "java:comp/websphere/UOWManager")
    UOWManager uowManager;

    private static void assertUOWType(SingletonLifecycleTxUOWState actual, int uowTypeExpected) {
        if (actual.type != uowTypeExpected) {
            throw new IllegalStateException("expected uowType=" + uowTypeExpected + " (" + uowTypeToString(uowTypeExpected) + "), " +
                                            "got " + actual.type + " (" + uowTypeToString(actual.type) + ")");
        }
    }

    private void testGlobalTx(String beanName) throws Exception {
        userTran.begin();

        SingletonLifecycleTxUOWState callerState = new SingletonLifecycleTxUOWState(uowManager);

        SingletonLifecycleTxXMLBean bean = (SingletonLifecycleTxXMLBean) new InitialContext().lookup("java:module/" + beanName);
        SingletonLifecycleTxUOWState state = bean.getPostConstructUOWState();

        assertUOWType(state, UOWManager.UOW_TYPE_GLOBAL_TRANSACTION);

        if (state.localId == callerState.localId) {
            throw new IllegalStateException("unexpected uowId=0x" + Long.toHexString(state.localId) + " already used by caller");
        }

        userTran.rollback();
    }

    private void testLTC(String beanName) throws Exception {
        SingletonLifecycleTxXMLBean bean = (SingletonLifecycleTxXMLBean) new InitialContext().lookup("java:module/" + beanName);
        SingletonLifecycleTxUOWState state = bean.getPostConstructUOWState();
        assertUOWType(state, UOWManager.UOW_TYPE_LOCAL_TRANSACTION);
    }

    private void testError(String beanName) throws Exception {
        try {
            new InitialContext().lookup("java:module/" + beanName);
            throw new IllegalStateException("expected lookup failure");
        } catch (NameNotFoundException e) {
            throw e;
        } catch (NamingException e) {
            logger.info("caught expected " + e);
        }
    }

    @Test
    public void testRequiredXMLStyle1() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle1Bean");
    }

    @Test
    public void testRequiredXMLStyle2() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle2Bean");
    }

    @Test
    public void testRequiredXMLStyle3() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle3Bean");
    }

    @Test
    public void testRequiredXMLStyle3Unspec() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle3UnspecBean");
    }

    @Test
    public void testRequiredXMLStyle4() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle4Bean");
    }

    @Test
    public void testRequiredXMLStyle4Unspec() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiredXMLStyle4UnspecBean");
    }

    @Test
    public void testRequiresNewXMLStyle1() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle1Bean");
    }

    @Test
    public void testRequiresNewXMLStyle2() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle2Bean");
    }

    @Test
    public void testRequiresNewXMLStyle3() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle3Bean");
    }

    @Test
    public void testRequiresNewXMLStyle3Unspec() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle3UnspecBean");
    }

    @Test
    public void testRequiresNewXMLStyle4() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle4Bean");
    }

    @Test
    public void testRequiresNewXMLStyle4Unspec() throws Exception {
        testGlobalTx("SingletonLifecycleTxRequiresNewXMLStyle4UnspecBean");
    }

    @Test
    public void testNotSupportedXMLStyle1() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle1Bean");
    }

    @Test
    public void testNotSupportedXMLStyle2() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle2Bean");
    }

    @Test
    public void testNotSupportedXMLStyle3() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle3Bean");
    }

    @Test
    public void testNotSupportedXMLStyle3Unspec() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle3UnspecBean");
    }

    @Test
    public void testNotSupportedXMLStyle4() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle4Bean");
    }

    @Test
    public void testNotSupportedXMLStyle4Unspec() throws Exception {
        testLTC("SingletonLifecycleTxNotSupportedXMLStyle4UnspecBean");
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testMandatoryXMLError() throws Exception {
        testError("SingletonLifecycleTxMandatoryXMLErrorBean");
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testNeverXMLError() throws Exception {
        testError("SingletonLifecycleTxNeverXMLErrorBean");
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testSupportsXMLError() throws Exception {
        testError("SingletonLifecycleTxSupportsXMLErrorBean");
    }
}
