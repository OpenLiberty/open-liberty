/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.fat.singleton.quiesce.web;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;
import io.openliberty.ejbcontainer.fat.singleton.quiesce.ejb.SingletonQuiesce;

@WebServlet("/SingletonQuiesceServlet")
@SuppressWarnings("serial")
public class SingletonQuiesceServlet extends FATServlet {

    // EJBs from the EJB module (SingletonQuiesceEjb.jar)
    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceDefault")
    SingletonQuiesce ejbSingletonQuiesceDefault;

    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceDD")
    SingletonQuiesce ejbSingletonQuiesceDD;

    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceBnd")
    SingletonQuiesce ejbSingletonQuiesceBnd;

    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceBndOverride")
    SingletonQuiesce ejbSingletonQuiesceBndOverride;

    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceServer")
    SingletonQuiesce ejbSingletonQuiesceServer;

    @EJB(beanName = "SingletonQuiesceEjb.jar#SingletonQuiesceServerOverride")
    SingletonQuiesce ejbSingletonQuiesceServerOverride;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceDefault")
    SingletonQuiesce ejbStartupSingletonQuiesceDefault;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceDD")
    SingletonQuiesce ejbStartupSingletonQuiesceDD;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceBnd")
    SingletonQuiesce ejbStartupSingletonQuiesceBnd;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceBndOverride")
    SingletonQuiesce ejbStartupSingletonQuiesceBndOverride;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceServer")
    SingletonQuiesce ejbStartupSingletonQuiesceServer;

    @EJB(beanName = "SingletonQuiesceEjb.jar#StartupSingletonQuiesceServerOverride")
    SingletonQuiesce ejbStartupSingletonQuiesceServerOverride;

    // EJBs from the Web module (SingletonQuiesceWeb.war)
    @EJB(beanName = "SingletonQuiesceDefault")
    SingletonQuiesce webSingletonQuiesceDefault;

    @EJB(beanName = "SingletonQuiesceDD")
    SingletonQuiesce webSingletonQuiesceDD;

    @EJB(beanName = "SingletonQuiesceBnd")
    SingletonQuiesce webSingletonQuiesceBnd;

    @EJB(beanName = "SingletonQuiesceServer")
    SingletonQuiesce webSingletonQuiesceServer;

    @EJB(beanName = "SingletonQuiesceServerOverride")
    SingletonQuiesce webSingletonQuiesceServerOverride;

    @EJB(beanName = "StartupSingletonQuiesceDefault")
    SingletonQuiesce webStartupSingletonQuiesceDefault;

    @EJB(beanName = "StartupSingletonQuiesceDD")
    SingletonQuiesce webStartupSingletonQuiesceDD;

    @EJB(beanName = "StartupSingletonQuiesceBnd")
    SingletonQuiesce webStartupSingletonQuiesceBnd;

    @EJB(beanName = "StartupSingletonQuiesceServer")
    SingletonQuiesce webStartupSingletonQuiesceServer;

    @EJB(beanName = "StartupSingletonQuiesceServerOverride")
    SingletonQuiesce webStartupSingletonQuiesceServerOverride;

    public void testBeans() throws Exception {
        // Call getName on all beans from EJB module
        ejbSingletonQuiesceDefault.getName();
        ejbSingletonQuiesceDD.getName();
        ejbSingletonQuiesceBnd.getName();
        ejbSingletonQuiesceBndOverride.getName();
        ejbSingletonQuiesceServer.getName();
        ejbSingletonQuiesceServerOverride.getName();
        ejbStartupSingletonQuiesceDefault.getName();
        ejbStartupSingletonQuiesceDD.getName();
        ejbStartupSingletonQuiesceBnd.getName();
        ejbStartupSingletonQuiesceBndOverride.getName();
        ejbStartupSingletonQuiesceServer.getName();
        ejbStartupSingletonQuiesceServerOverride.getName();

        // Call getName on all beans from Web module
        webSingletonQuiesceDefault.getName();
        webSingletonQuiesceDD.getName();
        webSingletonQuiesceBnd.getName();
        webSingletonQuiesceServer.getName();
        webSingletonQuiesceServerOverride.getName();
        webStartupSingletonQuiesceDefault.getName();
        webStartupSingletonQuiesceDD.getName();
        webStartupSingletonQuiesceBnd.getName();
        webStartupSingletonQuiesceServer.getName();
        webStartupSingletonQuiesceServerOverride.getName();
    }
}
