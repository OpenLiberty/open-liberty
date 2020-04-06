/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.servlet31.readListener;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/ReadListenerFilterServlet", asyncSupported = true)
public class ReadListenerFilterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    //private static final Logger LOG = Logger.getLogger(ReadListenerFilterServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

    }
}
