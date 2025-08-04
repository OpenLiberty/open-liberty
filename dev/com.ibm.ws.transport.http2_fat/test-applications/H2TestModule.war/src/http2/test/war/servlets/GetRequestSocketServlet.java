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
package http2.test.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

@WebServlet("/GetRequestSocketServlet")
public class GetRequestSocketServlet extends HttpServlet {
    static final long serialVersionUID = 9999L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        IExtendedRequest extRequest = (IExtendedRequest) req;
        IRequest iRequest = extRequest.getIRequest();
        IRequestExtended iRequestExt = (IRequestExtended) iRequest;
        Socket socket = iRequestExt.getRequestSocket();

        PrintWriter writer = resp.getWriter();
        writer.println("RequestSocket called from " +
                       "socket LocalPort: " + socket.getLocalPort());

        writer.flush();
        writer.close();
    }
}
