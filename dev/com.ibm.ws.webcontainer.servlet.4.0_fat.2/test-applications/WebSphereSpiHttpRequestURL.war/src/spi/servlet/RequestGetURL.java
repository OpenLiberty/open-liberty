/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package spi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.websphere.servlet.request.extended.IRequestExtended;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

@SuppressWarnings("serial")
@WebServlet("/testSPIGetURL")
public class RequestGetURL extends HttpServlet {
    private static final String CLASS_NAME = RequestGetURL.class.getName();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        _log(">>>>> service() ENTRY ");

        //demonstrate how to get to different request hierarchy objects.
        IExtendedRequest extRequest = (IExtendedRequest) request;
        IRequest iRequest = extRequest.getIRequest();
        IRequestExtended iRequestExt = (IRequestExtended) iRequest;
        IRequest40 iRequest40 = (IRequest40) iRequestExt;
        
        HttpRequest SpiHttpReq = (HttpRequest) iRequest40.getHttpRequest();
        String url = SpiHttpReq.getURL();

        _log("IExtendedRequest [" + extRequest + "]");
        _log("IRequest [" + iRequest + "]");
        _log("IRequestExtended [" + iRequestExt + "]");
        _log("IRequest40 [" + iRequest40 + "]");
        _log("SPI HttpRequest [" + SpiHttpReq );

        _log("IRequestExtended getRequestURI [" + iRequest.getRequestURI() + "]");
        _log("SPI HttpRequest getURL [" + url + "]");

        PrintWriter writer = response.getWriter();
        writer.println("SPI HTTPRequest getURL return [" + url + "]");

        _log("<<<<< service() EXIT");
    }
    
    private void _log(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }

}
