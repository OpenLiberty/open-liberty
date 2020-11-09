/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.web;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.UserTranStatelessBean;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ResourceServlet")
public class ResourceServlet extends FATServlet {
    /**
     * Tests that a UserTransaction can be injected into a CDI bean that is
     * constructed within the context of a container-managed EJB and that the
     * UserTransaction can be used meaningfully.
     */
    @Test
    public void testUserTransaction() throws Exception {
        UserTranStatelessBean bean = (UserTranStatelessBean) FATHelper.lookupDefaultBindingEJBLocalInterface(
                                                                                                             UserTranStatelessBean.class.getName(), "EJB31JCDITestApp",
                                                                                                             "EJB31JCDIBean.jar", "UserTranStatelessBean");

        // The first access to the UserTranBean should be within the context of
        // this EJB.  Ensure that the UserTransaction injected into it can be used
        // from a servlet.
        UserTransaction userTran = bean.getUserTransaction();
        userTran.begin();
        userTran.rollback();
    }
}
