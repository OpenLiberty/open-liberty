/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.fat.ejbinwarbnd;

import java.sql.Connection;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.fat.ejbinwarbnd.ejb.EJBInWARBndLocal;
import com.ibm.ws.ejbcontainer.fat.ejbinwarbnd.ejb.EJBInWARStatelessBean;

import componenttest.app.FATServlet;

@WebServlet("/EJBInWARBndServlet")
@SuppressWarnings("serial")
public class EJBInWARBndServlet extends FATServlet {

    @EJB(name = "ejb/servletdef/stateless", beanName = "EJBInWARStatelessBean")
    EJBInWARBndLocal stateless;

    /**
     * Ensure that the env-entry ejb binding is read
     *
     * @throws Exception
     */
    @Test
    public void testEJBInWarEnvEntry() throws Exception {
        stateless.verifyEnvEntryBinding();
    }

    @Resource(name = "jdbc/ejbResRefEjbIsoDS")
    private DataSource _bindEjbEjbTransactionReadUncommitDS;

    /**
     * Ensure that an ejb resource reference is read correctly
     */
    @Test
    public void testEJBInWarResourceRefBindings() throws Exception {
        EJBInWARStatelessBean.verifyDataSource(_bindEjbEjbTransactionReadUncommitDS, Connection.TRANSACTION_READ_UNCOMMITTED);
        stateless.verifyResourceBinding();
    }

    /**
     * Ensure that resource references and isolation levels are merged
     * properly. When working with EJB-in-War, the resource ref can be
     * defined in either the EJB or War and should be merged with an
     * isolation level defined in either the EJB or War binding.
     *
     * This test covers three permutations:
     * - res ref defined in ejb binding and the isolation level defined
     * in the web binding
     * - res ref defined in web binding and the isolation level defined
     * in the ejb binding
     * - res ref and isolation level duplicated and defined in both the
     * web and ejb binding
     *
     * @throws Exception
     */
    @Test
    public void testEJBInWarResourceIsolationLevelBindings() throws Exception {
        stateless.verifyResourceIsolationBindingMerge();
    }

    /**
     * Ensure that an ejb data-source binding is read correctly
     */
    @Test
    public void testEJBInWarDataSource() throws Exception {
        stateless.verifyDataSourceBinding();
    }
}
