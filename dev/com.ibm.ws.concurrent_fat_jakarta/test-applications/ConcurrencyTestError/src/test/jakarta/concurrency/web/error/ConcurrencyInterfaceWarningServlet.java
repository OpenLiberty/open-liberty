/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.web.error;

import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import test.jakarta.concurrency.ejb.error.AsynchInterfaceLocal;

//This servlet should never be installed as it uses a mis-configured EJB.
@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrencyInterfaceWarningServlet extends FATServlet {

    @EJB
    AsynchInterfaceLocal ejb;

    public void testMethod() {
        return;
    }

    public void getThreadName() {
        try {
            ejb.getThreadName();
        } catch (Exception e) {
            fail("Should have been able to execute method since Jakarta Ansychronous anotation was on the interface.");
        }
    }

    public void getThreadNameNonAsyc() {
        try {
            ejb.getThreadNameNonAsyc();
        } catch (Exception e) {
            fail("Should have been able to execute method since Jakarta Ansychronous anotation was on the interface.");
        }
    }

    public void getState() {
        try {
            Future<String> future = ejb.getState("Rochester");
            fail("Should not have been able to get a future object from a bean method.");
        } catch (Exception e) {
            //expected TODO ensure consistent behavior
        }
    }

    public void getStateFromService() {
        try {
            Future<String> future = ejb.getStateFromService("Rochester");
            fail("Should not have been able to get a future object from a bean method.");
        } catch (Exception e) {
            //expected TODO ensure consistent behavior
        }
    }

}
