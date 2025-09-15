/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package servlets;

import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.wsspi.uow.UOWAction;
import com.ibm.wsspi.uow.UOWManager;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/UOWScopeCallbackServlet")
public class UOWScopeCallbackServlet extends FATServlet {

    @Resource
    UOWManager uowm;

    public void testBadBehavior() throws Exception {

        UOWScopeCallback cb = null;

        try {
            cb = TxTestUtils.registerUOWScopeCallback();

            final long localUOWId = uowm.getLocalUOWId();

            uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
                @Override
                public void run() throws Exception {

                    if (localUOWId == uowm.getLocalUOWId()) {
                        throw new Exception("UOWAction not run under new UOW");
                    }
                }
            });

            assertTrue("END UOWScopeCallbacks should not have been called", TxTestUtils.onlyBeginCallbacksCalled(cb));
        } finally {
            TxTestUtils.unregisterUOWScopeCallback(cb);
        }
    }

    public void testGoodBehavior() throws Exception {

        UOWScopeCallback cb = null;

        try {
            cb = TxTestUtils.registerUOWScopeCallback();

            final long localUOWId = uowm.getLocalUOWId();

            uowm.runUnderUOW(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION, false, new UOWAction() {
                @Override
                public void run() throws Exception {

                    if (localUOWId == uowm.getLocalUOWId()) {
                        throw new Exception("UOWAction not run under new UOW");
                    }
                }
            });

            assertTrue("Not all UOWScopeCallbacks were called", TxTestUtils.allCallbacksCalled(cb));
        } finally {
            TxTestUtils.unregisterUOWScopeCallback(cb);
        }
    }
}
