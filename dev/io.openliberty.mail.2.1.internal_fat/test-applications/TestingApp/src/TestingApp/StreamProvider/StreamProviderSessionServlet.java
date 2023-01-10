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
package TestingApp.StreamProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        session.setDebug(true);

        StreamProvider streamProvider = null;

        String param = request.getParameter("testName");

        //Same outputStream will be used for all tests
        String encoderString = "Hello StreamProvider Encoders!";
        OutputStream outputStream = new ByteArrayOutputStream(encoderString.getBytes().length);
        outputStream.write(encoderString.getBytes());
        outputStream.flush();

        String decoderString = "Hello StreamProvider Decoders!";
        InputStream inputStream = new ByteArrayInputStream(decoderString.getBytes());

        OutputStream encoderOutputStream = null;
        InputStream decoderInputStream = null;

        try {
            switch (param) {
                case "testBase64":
                    streamProvider = session.getStreamProvider();;
                    encoderOutputStream = streamProvider.outputBase64(outputStream);
                    decoderInputStream = streamProvider.inputBase64(inputStream);
                    out.print("Base64 encoderOutputStream:" + encoderOutputStream.toString() + ", Base64 decoderInputStream:" + decoderInputStream.toString());
                    break;
                case "testBinary":
                    streamProvider = session.getStreamProvider();;
                    encoderOutputStream = streamProvider.outputBinary(outputStream);
                    decoderInputStream = streamProvider.inputBinary(inputStream);
                    out.print("Binary encoderOutputStream:" + encoderOutputStream.toString() + ", Binary decoderInputStream:" + decoderInputStream.toString());
                    break;
                case "testQ":
                    streamProvider = session.getStreamProvider();;
                    encoderOutputStream = streamProvider.outputQ(outputStream, false);
                    decoderInputStream = streamProvider.inputQ(inputStream);
                    out.print("Q encoderOutputStream:" + encoderOutputStream.toString() + ", Q decoderInputStream:" + decoderInputStream.toString());
                    break;
                case "testQP":
                    streamProvider = session.getStreamProvider();;
                    encoderOutputStream = streamProvider.outputQP(outputStream);
                    decoderInputStream = streamProvider.inputQP(inputStream);
                    out.print("QP encoderOutputStream:" + encoderOutputStream.toString() + ", QP decoderInputStream:" + decoderInputStream.toString());
                    break;
                case "testUU":
                    streamProvider = session.getStreamProvider();;
                    encoderOutputStream = streamProvider.outputUU(outputStream, null);
                    decoderInputStream = streamProvider.inputUU(inputStream);
                    out.print("UU encoderOutputStream:" + encoderOutputStream.toString() + ", UU decoderInputStream:" + decoderInputStream.toString());
                    break;
            }
        } catch (NoSuchMethodError e) {
            // Do nothing, not printing anything on PrintWriter causes assertion fail for not getting stream provider
        } finally {
            //Clean up
            outputStream.close();
            inputStream.close();
            if (null != encoderOutputStream)
                encoderOutputStream.close();
            if (null != decoderInputStream)
                decoderInputStream.close();
        }

    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
