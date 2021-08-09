/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.tx.rununderuow.web;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.ejbcontainer.tx.rununderuow.ejb.RunUnderUOWBMTBean;
import com.ibm.ws.ejbcontainer.tx.rununderuow.ejb.RunUnderUOWCMTBean;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Test that UOWManager.runUnderUOW works properly with EJBs.
 */
@WebServlet("/RunUnderUOWServlet")
public class RunUnderUOWServlet extends FATServlet {
    private static final long serialVersionUID = 6647199629501192360L;
    private static final String APP = "RunUnderUOWTestApp";
    private static final String MOD = "RunUnderUOWBean";

    static RunUnderUOWBMTBean lookupBMTBean(String bean) throws NamingException {
        return (RunUnderUOWBMTBean) FATHelper.lookupDefaultBindingEJBJavaGlobal(RunUnderUOWBMTBean.class.getName(), APP, MOD, bean);
    }

    static RunUnderUOWCMTBean lookupCMTBean(String bean) throws NamingException {
        return (RunUnderUOWCMTBean) FATHelper.lookupDefaultBindingEJBJavaGlobal(RunUnderUOWCMTBean.class.getName(), APP, MOD, bean);
    }

    /**
     * Ensure a BMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * begin/commit a global transaction, and resume the local transaction;
     * completing without error.
     */
    @Test
    public void testBMTStatelessLocal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSL");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a BMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testBMTStatelessGlobal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSL");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    /**
     * Ensure a BMT Stateful method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * begin/commit a global transaction, and resume the local transaction;
     * completing without error.
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.BeanNotReentrantException")
    public void testBMTStatefulLocal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSF");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.remove(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a BMT Stateful method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testBMTStatefulGlobal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSF");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.remove(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    /**
     * Ensure a BMT Singleton method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * begin/commit a global transaction, and resume the local transaction;
     * completing without error.
     */
    @Test
    public void testBMTSingletonLocal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSG");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a BMT Singleton method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testBMTSingletonGlobal() throws Exception {
        RunUnderUOWBMTBean bean = lookupBMTBean("BMTSG");
        bean.beanManaged(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTStatelessLocal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSL");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTStatelessGlobal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSL");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateful method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTStatefulLocal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSF");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.remove(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateful method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTStatefulGlobal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSF");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.remove(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a local transaction,
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTSingletonLocal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSG");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_LOCAL_TRANSACTION);
    }

    /**
     * Ensure a CMT Stateless method may use UOWManager.runUnderUOW to
     * suspend the local transaction, run under a global transaction
     * and resume the local transaction; completing without error.
     */
    @Test
    public void testCMTSingletonGlobal() throws Exception {
        RunUnderUOWCMTBean bean = lookupCMTBean("CMTSG");
        bean.notSupported(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.supports(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.required(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.requiresNew(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
        bean.never(UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION);
    }

}
