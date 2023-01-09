/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.cdi40.internal.fat.startupEvents.ear.war;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.ejb.StartupSingletonEJB;
import io.openliberty.cdi40.internal.fat.startupEvents.ear.lib.EarLibApplicationScopedBean;
import io.openliberty.cdi40.internal.fat.startupEvents.sharedLib.SharedLibApplicationScopedBean;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/events")
public class StartupEventsServlet extends FATServlet {

    @Inject
    private WarApplicationScopedBean warApplicationScopedBean;

    @Inject
    private EarLibApplicationScopedBean earLibApplicationScopedBean;

    @Inject
    private SharedLibApplicationScopedBean sharedLibApplicationScopedBean;

    @EJB
    private StartupSingletonEJB startupSingletonEJB;

    @Test
    public void testWarStartupEvents() {
        assertNotNull(warApplicationScopedBean);
        warApplicationScopedBean.test();
    }

    @Test
    public void testEarLibStartupEvents() {
        assertNotNull(earLibApplicationScopedBean);
        earLibApplicationScopedBean.test();
    }

    @Test
    public void testSharedLibStartupEvents() {
        assertNotNull(sharedLibApplicationScopedBean);
        sharedLibApplicationScopedBean.test();
    }

    @Test
    public void testStartupEJBStartupEvents() {
        assertNotNull(startupSingletonEJB);
        startupSingletonEJB.test();
    }
}
