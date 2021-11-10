/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb2x.ejbinwar.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.ejb.EJBHome;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.ejbinwar.intf.Stateful2xRemote;
import com.ibm.ejb2x.ejbinwar.intf.Stateful2xRemoteHome;
import com.ibm.ejb2x.ejbinwar.intf.Stateless2xRemoteHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/EJB2xTestServlet")
public class EJB2xTestServlet extends FATServlet {
    private static <T extends EJBHome> T lookupBean(Class<T> klass) throws NamingException {
        return klass.cast(FATHelper.lookupRemoteHomeBinding("ejb/" + klass.getName().replace('.', '/'), klass));
    }

    /**
     * This test ensures that a stateless session bean with a local home can be
     * defined in a 2.1 EJB deployment descriptor in a WAR.
     *
     * <p>A method is called on a remote driver bean. Then, the local home is
     * looked up from a custom binding location defined in ibm-ejb-jar-bnd.xmi,
     * an instance is obtained, and a method is called.
     *
     * <p>The test expects that the method call succeeds in returning a variant
     * of the input parameter.
     */
    @Test
    public void testStatelessLocal() throws Exception {
        lookupBean(Stateless2xRemoteHome.class).create().testStatelessLocal();
    }

    /**
     * This test ensures that a stateless session bean with a remote home can be
     * defined in a 2.1 EJB deployment descriptor in a WAR.
     *
     * <p>The remote home is looked up from a custom binding location defined in
     * ibm-ejb-jar-bnd.xmi, an instance is obtained, and a method is called.
     *
     * <p>The test expects that the method call succeeds in returning a variant
     * of the input parameter.
     */
    @Test
    public void testStatelessRemote() throws Exception {
        Stateless2xRemoteHome home = lookupBean(Stateless2xRemoteHome.class);
        assertEquals("Call to remote home of stateless bean was not successful", "paramparam", home.create().test("param"));
    }

    /**
     * This test ensures that a stateful session bean with a local home can be
     * defined in a 2.1 EJB deployment descriptor in a WAR.
     *
     * <p>A method is called on a remote driver bean. Then, the local home is
     * looked up from a custom binding location defined in ibm-ejb-jar-bnd.xmi,
     * an instance is obtained, and a method is called.
     *
     * <p>The test expects that the method call succeeds in returning a variant
     * of the input parameter.
     */
    @Test
    public void testStatefulLocal() throws Exception {
        lookupBean(Stateless2xRemoteHome.class).create().testStatefulLocal();
    }

    /**
     * This test ensures that a stateful session bean with a remote home can be
     * defined in a 2.1 EJB deployment descriptor in a WAR.
     *
     * <p>The remote home is looked up from a custom binding location defined in
     * ibm-ejb-jar-bnd.xmi, an instance is created with an input parameter, and
     * a method is called, which causes reactivation of the bean because of the
     * activationAt specified in ibm-ejb-jar-ext.xmi
     *
     * <p>The test expects that the final method call succeeds in returning a
     * list of events, which includes variants of the creation and method
     * parameters.
     */
    @Test
    public void testStatefulRemote() throws Exception {
        Stateful2xRemoteHome home = lookupBean(Stateful2xRemoteHome.class);
        Stateful2xRemote bean = home.create("create");
        assertEquals("Call to remote home of stateful bean was not successful", Arrays.asList("createcreate", "passivate", "activate", "paramparam", "passivate"),
                     bean.test("param"));
        bean.remove();
    }
}
