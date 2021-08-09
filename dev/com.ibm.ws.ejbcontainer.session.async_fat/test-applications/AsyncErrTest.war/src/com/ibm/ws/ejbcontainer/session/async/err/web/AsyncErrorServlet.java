/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.session.async.err.web;

import static org.junit.Assert.assertNull;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.session.async.err.shared.AsyncError1;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> asyncErrorTest
 *
 * <dt><b>Test Author:</b> Randy Erickson
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB 3.1 configuration exception test.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd><br>
 * Sub-tests
 * <ul>
 * <li>testNever - Invalid tx_attribute - NEVER
 * <li>testMDB - Invalid bean type - Message Driven Bean
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@WebServlet("/AsyncErrorServlet")
public class AsyncErrorServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private final static String APPLICATION = "AsyncErr1BeanApp";
    private final static String MODULE = "AsyncErr1Bean";

    private AsyncError1 lookupAsyncError1() throws NamingException {
        return (AsyncError1) FATHelper.lookupDefaultBindingEJBJavaGlobal(AsyncError1.class.getName(), APPLICATION, MODULE, "AsyncError1Bean");
    }

    /**
     * Invalid tx_attribute - NEVER
     */
    public void testNever() throws Exception {
        AsyncError1 bean = null;

        try {
            bean = lookupAsyncError1();
        } catch (NamingException iex) {
            // CannotInstantiateObjectException encountered starting application.
            // This is the exception associated with CNTR0187E.
        }

        assertNull("Application should have failed to start, but we successfully looked up the AsyncError1Bean bean", bean);
    }
}
