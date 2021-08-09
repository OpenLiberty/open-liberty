/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi.ejb.apps.remoteEJB;

import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/")
public class RemoteEJBServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    RemoteInterface test;

    @Inject
    Event<EJBEvent> anEvent;

    @Test
    public void testRemoteEJBsWorkWithCDI() throws Exception {
        anEvent.fire(new EJBEvent());
        assertTrue(test.observed());
    }

}
