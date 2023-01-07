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
package requestconnection.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test servletrequest getRequestID and servletconnection API
 * 
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletrequest#getRequestId()
 * 
 * https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/servletconnection
 *
 * request URL: /TestServletRequestID
 */
@WebServlet("/TestServletRequestID")
public class TestServletRequestID extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestServletRequestID.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public TestServletRequestID() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.info("TestServletRequestID");

        ServletOutputStream sos = response.getOutputStream();
        
        sos.println("Test ServletRequest and ServletConnection");
        testRequestConnection(request, response);
      
    }

    //Utilize the response header so that client can easily parse it.
    private void testRequestConnection(HttpServletRequest request, HttpServletResponse response)  {
        LOG.info("testRequestID");

        StringBuilder sBuilder = new StringBuilder();
        ServletConnection sConn = request.getServletConnection();

        response.setHeader("req.getRequestId", request.getRequestId());
        response.setHeader("req.getProtocolRequestId", request.getProtocolRequestId());
        response.setHeader("req.getServletConnection", sConn.toString());
        
        
        response.setHeader("conn.getConnectionId", sConn.getConnectionId());
        response.setHeader("conn.getProtocol", sConn.getProtocol());
        response.setHeader("conn.getProtocolConnectionId", sConn.getProtocolConnectionId());
        response.setHeader("conn.isSecure", String.valueOf(sConn.isSecure()));
        

        sBuilder.append("Testing 3 ServletRequest APIs. Set these values in the response headers \n");
        sBuilder.append(addDivider());
        sBuilder.append("   request.getRequestID=[" + response.getHeader("req.getRequestId") + "]\n");
        sBuilder.append("   request.getProtocolRequestId=[" + response.getHeader("req.getProtocolRequestId")  + "]\n");
        sBuilder.append("   request.getServletConnection=[" + response.getHeader("req.getServletConnection")  + "]\n");
        sBuilder.append(addDivider());
        sBuilder.append(addDivider());

        sBuilder.append("Testing 4 ServletConnection APIs \n");
        sBuilder.append(addDivider());
        sBuilder.append("   ServletConnection.getConnectionId=[" + response.getHeader("conn.getConnectionId")+ "]\n");
        sBuilder.append("   ServletConnection.getProtocol=[" + response.getHeader("conn.getProtocol")+ "] \n");
        sBuilder.append("   ServletConnection.getProtocolConnectionId=[" + response.getHeader("conn.getProtocolConnectionId")+ "]\n");
        sBuilder.append("   ServletConnection.isSecure=[" + response.getHeader("conn.isSecure") + "]\n");
        sBuilder.append(addDivider());
        
        LOG.info(sBuilder.toString());

    }

    public static String addDivider() {
        return ("=============================\n");
    }
}
