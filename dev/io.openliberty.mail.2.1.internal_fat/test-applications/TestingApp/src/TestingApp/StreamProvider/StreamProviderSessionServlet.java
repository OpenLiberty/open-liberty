/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package TestingApp.SMTP;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import jakarta.annotation.Resource;
import jakarta.mail.Session;
import jakarta.mail.util.StreamProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/StreamProviderSessionServlet")
public class StreamProviderSessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "TestingApp/SMTPMailSessionServlet/testSMTPMailSession")
    Session session;

    private final String encodeString = "Hello StreamProvider Encoders!";
    private final InputStream inputEndcodeStream = new ByteArrayInputStream(encodeString.getBytes());

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        session.setDebug(true);

        StreamProvider streamProvider = null;;

        try {
            streamProvider = session.getStreamProvider();
            out.print("getStreamProvider method successfully invoked");
        } catch (NoSuchMethodError e) {
            // Do nothing, not printing anything on PrintWriter causes assertion fail
        }

//        String param = request.getHeader("testName");
//
//        switch (param) {
//            case "testNewStreamProvider":
//                try {
//                    streamProvider = session.getStreamProvider();
//                    out.print("getStreamProvider method successfully invoked");
//                } catch (NoSuchMethodError e) {
//                    // Do nothing, not printing anything on PrintWriter causes assertion fail
//                }
//                break;
//            case "testBase64Encoder":
//
//                break;
//            case "testBinaryEncoder":
//
//                break;
//            case "testQEncoder":
//
//                break;
//            case "testQPEncoder":
//
//                break;
//            case "testUUEncoder":
//
//                break;
//        }

//        InputStream EncodedInputStream = streamProvider.inputBase64(inputEndcodeStream);
//        out.print("Is base64: " + Base64.isBase64(encodeString.getBytes()));

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
