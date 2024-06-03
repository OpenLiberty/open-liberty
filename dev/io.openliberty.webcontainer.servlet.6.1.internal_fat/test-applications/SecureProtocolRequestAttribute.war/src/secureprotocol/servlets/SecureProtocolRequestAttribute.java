/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package secureprotocol.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test request attribute "jakarta.servlet.request.secure_protocol" is set when HTTPS using TLSv1.3 and TLSv1.2
 */
@WebServlet(urlPatterns = {"/TestSecureProtocolRequestAttribute/*"}, name = "SecureProtocolRequestAttribute")
public class SecureProtocolRequestAttribute extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = SecureProtocolRequestAttribute.class.getName();

    //Common
    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    long access_lastAccessedTime = 0;

    public SecureProtocolRequestAttribute() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGENTER("doGET");

        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        testRequestAttributeSecureProtocol();

        if (!responseSB.isEmpty()) {
            sos.println(responseSB.toString());
        }

        LOGEXIT("doGET");
    }

    private void testRequestAttributeSecureProtocol() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        String securePro = (String) request.getAttribute("jakarta.servlet.request.secure_protocol");

        LOG("Request attribute secure protocol [" + securePro + "]");

        responseSB.append("Request attribute secure protocol [" + securePro + "]");
        LOG("Sending response [" + responseSB.toString() + "]");
        LOGEXIT(method);
    }

    //#########################################################
    private void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }

    private void LOGENTER(String method) {
        LOG(">>>>>>>>>>>>>>>> TESTING [" + method + "] ENTER >>>>>>>>>>>>>>>>");
    }

    private void LOGEXIT(String method) {
        LOG("<<<<<<<<<<<<<<<<<< TESTING [" + method + "] EXIT <<<<<<<<<<<<<<<<<<");
    }
}
