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
package io.openliberty.ejbcontainer.fat.singleton.quiesce.war;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@WebServlet("/SingletonQuiesceServlet")
@SuppressWarnings("serial")
public class SingletonQuiesceServlet extends FATServlet {

    @EJB(beanName = "SingletonQuiesceDefault")
    SingletonQuiesce singletonQuiesceDefault;

    @EJB(beanName = "SingletonQuiesceDD")
    SingletonQuiesce singletonQuiesceDD;

    @EJB(beanName = "SingletonQuiesceBnd")
    SingletonQuiesce singletonQuiesceBnd;

    @EJB(beanName = "SingletonQuiesceServer")
    SingletonQuiesce singletonQuiesceServer;

    @EJB(beanName = "SingletonQuiesceServerOverride")
    SingletonQuiesce singletonQuiesceServerOverride;

    @EJB(beanName = "StartupSingletonQuiesceDefault")
    SingletonQuiesce startupSingletonQuiesceDefault;

    @EJB(beanName = "StartupSingletonQuiesceDD")
    SingletonQuiesce startupSingletonQuiesceDD;

    @EJB(beanName = "StartupSingletonQuiesceBnd")
    SingletonQuiesce startupSingletonQuiesceBnd;

    @EJB(beanName = "StartupSingletonQuiesceServer")
    SingletonQuiesce startupSingletonQuiesceServer;

    @EJB(beanName = "StartupSingletonQuiesceServerOverride")
    SingletonQuiesce startupSingletonQuiesceServerOverride;

    public void testBeans() throws Exception {
        // Call getName on all beans to ensure they are initialized
        singletonQuiesceDefault.getName();
        singletonQuiesceDD.getName();
        singletonQuiesceBnd.getName();
        singletonQuiesceServer.getName();
        singletonQuiesceServerOverride.getName();
        startupSingletonQuiesceDefault.getName();
        startupSingletonQuiesceDD.getName();
        startupSingletonQuiesceBnd.getName();
        startupSingletonQuiesceServer.getName();
        startupSingletonQuiesceServerOverride.getName();
    }
}
