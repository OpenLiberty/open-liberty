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
package test.ejbcontainer.restart.singleton.web;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;

import componenttest.app.FATServlet;
import test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal;
import test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote;

/**
 * Test that non-persistent timers respect application lifecycle.
 */
@WebServlet("RestartStartupSingletonServlet")
@SuppressWarnings("serial")
public class RestartStartupSingletonServlet extends FATServlet {

    private static final Logger logger = Logger.getLogger(RestartStartupSingletonServlet.class.getName());

    /**
     * Verifies that all of the startup singletons started successfully
     */
    public void verify() throws Exception {
        logger.info("Verifing that all Startup Singleton beans started successfully");

        InitialContext ctx = new InitialContext();

        // ------------------------------------------------------------------------------------------------------
        // RestartStartupSingletonWarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        Object found = ctx.lookup("java:global/RestartStartupSingletonWeb/RestartStartupSingletonWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        RestartStartupSingletonRemote rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonWarBean", rbean.getName());

        found = ctx.lookup("ejb/RestartStartupSingletonWarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonWarBean", rbean.getName());

        RestartStartupSingletonLocal lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonWeb/RestartStartupSingletonWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartStartupSingletonWarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartStartupSingletonWarBean");
        Assert.assertEquals("RestartStartupSingletonWarBean", lbean.getName());

        // ------------------------------------------------------------------------------------------------------
        // RestartSimpleBindingSingletonWarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        found = ctx.lookup("java:global/RestartStartupSingletonWeb/RestartSimpleBindingSingletonWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartSimpleBindingSingletonWarBean", rbean.getName());

        found = ctx.lookup("RestartSimpleBindingSingletonWarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartSimpleBindingSingletonWarBean", rbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonWeb/RestartSimpleBindingSingletonWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartSimpleBindingSingletonWarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartSimpleBindingSingletonWarBean");
        Assert.assertEquals("RestartSimpleBindingSingletonWarBean", lbean.getName());

        // ------------------------------------------------------------------------------------------------------
        // RestartStartupSingletonJarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        found = ctx.lookup("java:global/RestartStartupSingletonEjb/RestartStartupSingletonJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonJarBean", rbean.getName());

        found = ctx.lookup("ejb/RestartStartupSingletonJarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonJarBean", rbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonEjb/RestartStartupSingletonJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartStartupSingletonJarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartStartupSingletonJarBean");
        Assert.assertEquals("RestartStartupSingletonJarBean", lbean.getName());

        // ------------------------------------------------------------------------------------------------------
        // RestartStartupSingletonEarWarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        found = ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarWeb/RestartStartupSingletonEarWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonEarWarBean", rbean.getName());

        found = ctx.lookup("ejb/RestartStartupSingletonEarWarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonEarWarBean", rbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarWeb/RestartStartupSingletonEarWarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartStartupSingletonEarWarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartStartupSingletonEarWarBean");
        Assert.assertEquals("RestartStartupSingletonEarWarBean", lbean.getName());

        // ------------------------------------------------------------------------------------------------------
        // RestartStartupSingletonEarJarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        found = ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarEjb/RestartStartupSingletonEarJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonEarJarBean", rbean.getName());

        found = ctx.lookup("ejb/RestartStartupSingletonEarJarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartStartupSingletonEarJarBean", rbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarEjb/RestartStartupSingletonEarJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartStartupSingletonEarJarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartStartupSingletonEarJarBean");
        Assert.assertEquals("RestartStartupSingletonEarJarBean", lbean.getName());

        // ------------------------------------------------------------------------------------------------------
        // RestartSimpleBindingSingletonEarJarBean - remote in global, remote in root, local in global & local in ejblocal
        // ------------------------------------------------------------------------------------------------------
        found = ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarEjb/RestartSimpleBindingSingletonEarJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonRemote");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartSimpleBindingSingletonEarJarBean", rbean.getName());

        found = ctx.lookup("RestartSimpleBindingSingletonEarJarBean");
        rbean = (RestartStartupSingletonRemote) PortableRemoteObject.narrow(found, RestartStartupSingletonRemote.class);
        Assert.assertEquals("RestartSimpleBindingSingletonEarJarBean", rbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("java:global/RestartStartupSingletonApp/RestartStartupSingletonEarEjb/RestartSimpleBindingSingletonEarJarBean!test.ejbcontainer.restart.singleton.shared.RestartStartupSingletonLocal");
        Assert.assertEquals("RestartSimpleBindingSingletonEarJarBean", lbean.getName());

        lbean = (RestartStartupSingletonLocal) ctx.lookup("ejblocal:RestartSimpleBindingSingletonEarJarBean");
        Assert.assertEquals("RestartSimpleBindingSingletonEarJarBean", lbean.getName());
    }
}
