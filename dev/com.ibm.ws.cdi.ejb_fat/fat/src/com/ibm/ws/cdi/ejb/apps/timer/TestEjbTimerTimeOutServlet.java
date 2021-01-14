/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.timer;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi.ejb.apps.timer.view.SessionBeanLocal;

/**
 *
 */
@WebServlet("/timerTimeOut")
public class TestEjbTimerTimeOutServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    @EJB
    SessionBeanLocal bean;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println(bean.getValue());
        bean.setUpTimer();
    }
}
