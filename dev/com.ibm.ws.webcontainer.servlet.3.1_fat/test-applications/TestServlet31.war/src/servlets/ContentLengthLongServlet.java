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
package servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet(urlPatterns = "/ContentLengthLongServlet/*")
public class ContentLengthLongServlet extends HttpServlet {

    private static final long serialVersionUID = 101L;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String dataLen = req.getPathInfo();
        System.out.println("pathInfo = " + dataLen);

        if (dataLen.startsWith("/RESP")) {
            dataLen = dataLen.substring(5);
            System.out.println("Response : datLen = " + dataLen);
            long dataLenLong = new Long(dataLen).longValue();

            ServletOutputStream os = resp.getOutputStream();
            resp.setContentLengthLong(dataLenLong);

            byte[] b = new byte[32768];
            for (int i = 0; i < 32768; i++) {
                b[i] = (byte) 0x62;
            }

            int writeNum = 1;
            long remaining = dataLenLong;

            try {
                while (remaining > 0) {
                    if (remaining <= 32768) {
                        os.write(b, 0, (int) remaining);
                        remaining = 0;
                    } else if (writeNum == 1) {
                        os.write(b, 0, 32768);
                        writeNum = 2;
                        remaining -= 32768;
                    } else if (writeNum == 2) {
                        os.write(b, 0, 16384);
                        writeNum = 3;
                        remaining -= 16384;
                    } else {
                        os.write(b, 0, 8192);
                        writeNum = 2;
                        remaining -= 8192;
                    }
                }
            } catch (IOException ioe) {
                resp.sendError(500);
            }
        } else {
            //strip of the leading "/"
            dataLen = dataLen.substring(1);
            System.out.println("Request : datLen = " + dataLen);

            long dataLenLong = new Long(dataLen).longValue();
            String output = "Test for expected content length " + dataLen + " : ";
            System.out.println(output);

            long contentLengthLong = req.getContentLengthLong();
            System.out.println("contentLengthLong = " + Long.toString(contentLengthLong));

            if (dataLenLong == contentLengthLong) {
                output += "Test getLong : PASS : expected long length " + Long.toString(dataLenLong) + " and content length long was " + Long.toString(contentLengthLong);
            } else {
                output += "Test getLong : FAIL : expected long length " + Long.toString(dataLenLong) + " and content length long was " + Long.toString(contentLengthLong);
            }

            int contentLengthInt = req.getContentLength();
            System.out.println("contentLengthInt = " + contentLengthInt);

            if (dataLenLong > Integer.MAX_VALUE) {
                if (contentLengthInt == -1) {
                    output += " Test getInt : PASS : expected int length -1 because content length was greater than " + Integer.MAX_VALUE + " : " + dataLen;
                } else {
                    output += " Test getInt : FAIL : expected int length -1 because content length was greater than " + Integer.MAX_VALUE + " : " + dataLen + " , but got "
                              + contentLengthInt;
                }
            } else {
                if ((int) dataLenLong == contentLengthInt) {
                    output += " Test getInt : PASS : expected int length " + Long.toString(dataLenLong) + " and content length int was " + contentLengthInt;
                } else {
                    output += " Test getInt : FAIL : expected int length " + Long.toString(dataLenLong) + " and content length int was " + contentLengthInt;
                }
            }

            ServletInputStream is = req.getInputStream();
            byte[] b1 = new byte[32768];
            byte[] b2 = new byte[16384];
            byte[] b3 = new byte[8192];

            int readLen = is.read(b1);
            long total = 0;
            int bufferNum = 1;
            while (readLen != -1) {
                total += readLen;
                if (bufferNum == 1) {
                    readLen = is.read(b1);
                    bufferNum = 2;
                } else if (bufferNum == 2) {
                    readLen = is.read(b2);
                    bufferNum = 2;
                } else {
                    readLen = is.read(b3);
                    bufferNum = 1;
                }
            }

            if (total == dataLenLong) {
                output += " Test getData : PASS : expected " + Long.toString(dataLenLong) + "bytes of data and received " + Long.toString(total) + " bytes.";
            } else {
                output += " Test getData : FAIL : expected " + Long.toString(dataLenLong) + "bytes of data and received " + Long.toString(total) + " bytes.";
            }

            long responseLengthLong = new Long(output.length());
            resp.setContentLengthLong(responseLengthLong);
            System.out.println("responseLength = " + Long.toString(responseLengthLong));
            System.out.println("response output = " + output);

            PrintWriter pw = resp.getWriter();

            pw.print(output);
            pw.flush();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
        // TODO Auto-generated method stub
        super.service(arg0, arg1);
    }

}
