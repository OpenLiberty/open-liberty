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
package com.ibm.ws.cdi20.fat.apps.events.war;

import static org.junit.Assert.assertNotNull;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.cdi20.fat.apps.events.ejb.SingletonStartupBean;

import componenttest.app.FATServlet;

@WebServlet("/events")
public class StartupEventsServlet extends FATServlet {

    @Inject
    private WarApplicationScopedBean warApplicationScopedBean;

    @EJB
    private SingletonStartupBean ejb;

    @Test
    public void testWarStartupEvents() {
        assertNotNull(warApplicationScopedBean);
        warApplicationScopedBean.test();
    }

}
