/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.misc.web;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.omg.CORBA.ORB;

import com.ibm.ws.ejbcontainer.injection.misc.ejb.ClientRemote;
import com.ibm.ws.ejbcontainer.injection.misc.ejb.EJBContextExtensionBean;
import com.ibm.ws.ejbcontainer.injection.misc.ejb.ImplicitTypeBean;
import com.ibm.ws.ejbcontainer.injection.misc.ejb.JavaAppBean;
import com.ibm.ws.ejbcontainer.injection.misc.ejb.SuperClassLevelBean;

import componenttest.app.FATServlet;

@EJBs({
        @EJB(name = "java:app/env/ejbref", beanInterface = ClientRemote.class),
        @EJB(name = "java:module/env/ejbref", beanInterface = ClientRemote.class),
        @EJB(name = "java:comp/env/ejbref", beanInterface = ClientRemote.class)
})
@Resources({
             @Resource(name = "orb/omg", type = org.omg.CORBA.ORB.class),
})

@WebServlet("/InjectionMiscServlet")
public class InjectionMiscServlet extends FATServlet {
    private static final long serialVersionUID = 8599560174705644483L;

    /**
     * This test ensures that class-level annotations on bean super-class are
     * processed.
     */
    @Test
    public void testSuperClassLevelAnnotation() throws Exception {
        ((SuperClassLevelBean) new InitialContext().lookup("java:app/InjectionMiscBean/SuperClassLevelBean")).test();
    }

    /**
     * This test ensures that a java:app env-entry with an env-entry-value
     * declared in one component namespace can be injected into another
     * component namespace that does not declare a value. Normally, an
     * env-entry is only injected if it has a value, but for non-java:comp, the
     * search for a value must be broader.
     */
    @Test
    public void testJavaAppEnvEntryInjection() throws Exception {
        //new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppResourceBean");
        ((JavaAppBean) new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppBean")).testEnvEntryInjection();
    }

    /**
     * This test ensures that the binding value for a java:app env-entry is
     * respected.
     */
    @Test
    public void testJavaAppEnvEntryBindingValue() throws Exception {
        ((JavaAppBean) new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppBean")).testEnvEntryBindingValue();
    }

    /**
     * This test ensures that the binding-name for a java:app env-entry is
     * respected.
     */
    @Test
    public void testJavaAppEnvEntryBindingName() throws Exception {
        ((JavaAppBean) new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppBean")).testEnvEntryBindingName();
    }

    /**
     * This test ensures that a java:app resource-env-ref for SessionContext can
     * be declared in one component namespace and injected into another.
     */
    @Test
    public void testJavaAppSessionContext() throws Exception {
        ((JavaAppBean) new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppBean")).testSessionContext();
    }

    /**
     * This test ensures that a java:app resource-env-ref for
     * TransactionSynchronizationRegistry can be declared in a non-java:comp
     * namespace.
     */
    @Test
    public void testJavaAppTransactionSynchronizationRegistry() throws Exception {
        ((JavaAppBean) new InitialContext().lookup("java:app/InjectionMiscBean/JavaAppBean")).testTransactionSynchronizationRegistry();
    }

    /**
     * This test ensures that when a type is not specified in an env-entry,
     * ejb-ref, ejb-local-ref, resource-ref, or resource-env-ref but an
     * injection-target is specified in XML that the corresponding field or
     * method is found properly.
     */
    @Test
    public void testImplicitType() throws Exception {
        ((ImplicitTypeBean) new InitialContext().lookup("java:app/InjectionMiscBean/ImplicitTypeBean!" + ImplicitTypeBean.class.getName())).test();
    }

    /**
     * This test ensures that a servlet can look up an EJB ref declared in
     * java:global, java:app, and java:module, and java:comp.
     */
    @Test
    public void testEJBRef() throws Exception {
        ((ClientRemote) new InitialContext().lookup("java:global/env/ejb/ClientBeanRef")).test();
        ((ClientRemote) new InitialContext().lookup("java:app/env/ejbref")).test();
        ((ClientRemote) new InitialContext().lookup("java:module/env/ejbref")).test();
        ((ClientRemote) new InitialContext().lookup("java:comp/env/ejbref")).test();
    }

    /**
     * This test ensures that an ORB can be injected using the supported ORB
     * interfaces types.
     */
    @Test
    public void testORB() throws Exception {
        org.omg.CORBA.Object remote = (org.omg.CORBA.Object) new InitialContext().lookup("java:comp/env/ejbref");
        ((ORB) new InitialContext().lookup("java:comp/env/orb/omg")).object_to_string(remote);
    }

    /**
     * This test ensures an EJBContextExtension and SessionContextExtension may be
     * injected into a bean class and the isTransactionGlobal API method works properly
     * with the REQUIRED transaction attribute.
     */
    @Test
    public void testEJBContextExtensionWithRequired() throws Exception {
        ((EJBContextExtensionBean) new InitialContext().lookup("java:app/InjectionMiscBean/EJBContextExtensionBean")).verifyEJBContextExtensionWithRequired();
    }

    /**
     * This test ensures an EJBContextExtension and SessionContextExtension may be
     * injected into a bean class and the isTransactionGlobal API method works properly
     * with the NOT_SUPPORTED transaction attribute.
     */
    @Test
    public void testEJBContextExtensionWithNotSupported() throws Exception {
        ((EJBContextExtensionBean) new InitialContext().lookup("java:app/InjectionMiscBean/EJBContextExtensionBean")).verifyEJBContextExtensionWithNotSupported();
    }

}
