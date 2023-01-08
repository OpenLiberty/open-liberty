/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.web;

import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.singletonlifecycledeadlock.ejb.SingletonPostConstructBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/SingletonPostConstructServlet")
public class SingletonPostConstructServlet extends FATServlet {

    @EJB
    SingletonPostConstructBean bean;

    @Test
    @ExpectedFFDC({ "javax.ejb.ConcurrentAccessTimeoutException", "com.ibm.websphere.csi.CSITransactionRolledbackException", "javax.ejb.EJBTransactionRolledbackException" })
    public void testPostConstruct() throws Exception {
        executor = Executors.newSingleThreadExecutor();
        System.out.println("Servlet testPostConstruct");
        //Do the method call in another thread otherwise this test will hang forever if we deadlock
        Future<?> result = callBean();
        try {
            result.get(2, TimeUnit.MINUTES);
        } catch (TimeoutException toe) {
            fail("Singleton PostConstruct did not finish (fail) in a timely matter, probably hit deadlock");
        }
    }

    private ExecutorService executor;

    private Future<?> callBean() {
        return executor.submit(() -> {
            bean.otherBusinessMethod();
        });
    }

}
