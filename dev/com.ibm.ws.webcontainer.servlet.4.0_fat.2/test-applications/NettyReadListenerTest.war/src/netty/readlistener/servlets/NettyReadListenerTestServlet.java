/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package netty.readlistener.servlets;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import netty.readlistener.listeners.NettyReadListener;

@WebServlet(value = "/NettyReadListenerTestServlet", asyncSupported = true)
public class NettyReadListenerTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        AsyncContext ac = request.startAsync();
        ServletOutputStream output = response.getOutputStream();
        ServletInputStream input = request.getInputStream();
        NettyReadListener readListener = new NettyReadListener(input, output, ac);
        input.setReadListener(readListener);
    }
}
