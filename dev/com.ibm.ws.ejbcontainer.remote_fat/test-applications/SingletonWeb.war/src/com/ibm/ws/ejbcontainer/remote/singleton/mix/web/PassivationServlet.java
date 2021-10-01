/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.remote.singleton.mix.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.MixHelper;
import com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.StatefulEJBRefLocal;
import com.ibm.ws.ejbcontainer.remote.singleton.mix.shared.StatefulEJBRefRemote;

import componenttest.app.FATServlet;

/**
 * Test the basic properties of a singleton session bean.
 */
@WebServlet("/PassivationMixServlet")
@SuppressWarnings("serial")
public class PassivationServlet extends FATServlet {

    private StatefulEJBRefLocal lookupLocal() throws NamingException {
        return MixHelper.lookupDefaultLocal(StatefulEJBRefLocal.class, "StatefulEJBRefBean");
    }

    private StatefulEJBRefRemote lookupRemote() throws NamingException {
        return MixHelper.lookupDefaultRemote(StatefulEJBRefRemote.class, "StatefulEJBRefBean");
    }

    /**
     * Passivate and activate an SFSB with a reference to a singleton bean's business interface
     */
    @Test
    public void testMixPassivateSingleton() throws Exception {
        // Create an instance of the bean by looking up the business interface and ensure the bean contains the default state.
        StatefulEJBRefLocal localBean = lookupLocal();
        assertNotNull("SFLSB 'lookup' successful", localBean);
        assertTrue("Local Bean + Remote Ref Activate/Passivate successful", localBean.testRemoteSingleStart());
        assertTrue("Remote Ref is same after A/P", localBean.testRemoteSingleEnd());
        localBean.finish();

        // Create an instance of the bean by looking up the business interface and ensure the bean contains the default state.
        localBean = lookupLocal();
        assertNotNull("SFLSB 'lookup' successful", localBean);
        assertTrue("Local Bean + Local Ref Activate/Passivate successful", localBean.testLocalSingleStart());
        assertTrue("Local Ref is same after A/P", localBean.testLocalSingleEnd());
        localBean.finish();

        // Create an instance of the bean by looking up the business interface and ensure the bean contains the default state.
        StatefulEJBRefRemote bean = lookupRemote();
        assertNotNull("SFRSB 'lookup' successful", bean);
        assertTrue("Remote Bean + Local Ref Activate/Passivate successful", bean.testLocalSingleStart());
        assertTrue("Local Ref is same after A/P", bean.testLocalSingleEnd());
        bean.finish();

        // Create an instance of the bean by looking up the business interface and ensure the bean contains the default state.
        bean = lookupRemote();
        assertNotNull("SFRSB 'lookup' successful", bean);
        assertTrue("Remote Bean + Remote Ref Activate/Passivate successful", bean.testRemoteSingleStart());
        assertTrue("Remote Ref is same after A/P", bean.testRemoteSingleEnd());
        bean.finish();
    }
}
