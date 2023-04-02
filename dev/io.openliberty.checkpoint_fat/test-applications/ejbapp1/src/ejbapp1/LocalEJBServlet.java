/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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

package ejbapp1;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.ejb.EJB;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;

@WebServlet("/")
public class LocalEJBServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    LocalInterface test;

    @Inject
    Event<EJBEvent> anEvent;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        anEvent.fire(new EJBEvent());
        assertTrue(test.observed());

        response.getOutputStream().println("Got RemoteEJBServlet");
    }
}
