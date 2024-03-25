/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package readwritebytebuffer.servlets;

import java.io.IOException;
import java.nio.ByteBuffer;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test the ServletInputStream read(ByteBuffer) the request POST data
 * then ServletOutputStream write(ByteBuffer) those data back to client
 */
@WebServlet(urlPatterns = {"/TestReadWriteByteBuffer/*"}, name = "ReadWriteByteBuffer")
public class ReadWriteByteBuffer extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = ReadWriteByteBuffer.class.getName();

    //Common
    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    //Just for this test
    ByteBuffer buffer = null;
    String message = null;

    public ReadWriteByteBuffer() {
        super();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGENTER("doPOST via doGET");
        doGet(req,resp);
        LOGEXIT("doPOST via doGET");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGENTER("doGET");

        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        switch (request.getHeader("runTest")) {
            case "testReadWriteByteBuffer" : testReadWriteByteBuffer(); break;
            case "testReadNullByteBuffer" : testRedWriteNullByteBuffer(true); break;
            case "testWriteNullByteBuffer" : testRedWriteNullByteBuffer(false); break;
        }

        if (!responseSB.isEmpty()) {
            LOG(responseSB.toString());
            sos.println(responseSB.toString());
        }

        LOGEXIT("doGET");
    }


    /*
     * Test ServletInputStream.read(ByteBuffer) to read POST data.
     * Then ServletOutputStream.write(ByteBuffer) to write data back to client for verification
     */
    private void testReadWriteByteBuffer() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        int readIn;
        try {
            buffer = ByteBuffer.allocate(1024);

            LOG("Read data using ServletInputStream.read(ByteBuffer)");
            readIn = request.getInputStream().read(buffer);
            LOG("read(ByteBuffer): read [" + readIn +"] bytes. DONE");

            LOG("Write [" + buffer.remaining() + "] bytes to client using ServletOutputStream.write(ByteBuffer)");
            sos.write(buffer);
            LOG("write(ByteBuffer) DONE");
        }
        catch (Exception e) {
            LOG("Exception ["+ e.getMessage() +"]");
        }

        LOGEXIT(method);
    }

    /*
     * Test ServletInputStream.read(ByteBuffer) and ServletOutputStream.write(ByteBuffer) with NULL ByteBuffer to cause NPE
     * test will catch, exam the message and report PASS/FAIL back to client for assert
     *
     * @param boolean read      - true - for read a null ByteBuffer
     *                          - false - for write a null ByteBuffer
     */
    private void testRedWriteNullByteBuffer(boolean read) throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        try {
            buffer = null;

            if (read) {
                LOG("ServletInputStream.read(ByteBuffer) with NULL ByteBuffer");
                request.getInputStream().read(buffer);
                LOG("read(ByteBuffer) DONE");
            }
            else {
                LOG("ServletOutputStream.write(ByteBuffer) with NULL ByteBuffer");
                sos.write(buffer);
                LOG("write(ByteBuffer) DONE");
            }
        }
        catch (Exception e) {
            LOG("Exception ["+ (message = e.getMessage()) +"]");

            if (message.contains("CWWWC0010E")) {
                responseSB.append(method + " found the message with CWWWC0010E. PASS");
            }
            else {
                responseSB.append(method + " expected error message contains CWWWC0010E but found [" + message + "]. FAIL");
            }
        }

        LOGEXIT(method);
    }

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
