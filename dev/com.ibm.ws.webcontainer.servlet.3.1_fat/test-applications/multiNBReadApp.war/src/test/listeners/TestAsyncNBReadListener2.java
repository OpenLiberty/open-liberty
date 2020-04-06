/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.listeners;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Second Listener for MultiRead
 */
public class TestAsyncNBReadListener2 implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse response = null;
    private HttpServletRequest request = null;
    private AsyncContext ac = null;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private String responseString;
    String testToPerfom = null;
    long dataSizeofLen = 0;
    StringBuffer inBufferDataFromPrevious = new StringBuffer();
    boolean pass = false;
    StringBuffer inBuffer = null;

    String classname = "TestAsyncNBReadListener2";

    private static final Logger LOG = Logger.getLogger(TestAsyncNBReadListener2.class.getName());

    public TestAsyncNBReadListener2(ServletInputStream in, HttpServletRequest req, HttpServletResponse r,
                                    AsyncContext c, String test, StringBuffer inbuff) {
        input = in;
        response = r;
        ac = c;
        request = req;
        testToPerfom = test;
        dataSizeofLen = 0;
        inBufferDataFromPrevious = inbuff;
        inBuffer = new StringBuffer();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ReadListener#onAllDataRead()
     */
    @Override
    public void onAllDataRead() throws IOException {

        System.out.println("onAllDataRead : TestAsyncNBReadListener2");

        ServletOutputStream out = response.getOutputStream();
        while (queue.peek() != null) {
            String data = queue.poll();
            LOG.info("onAllDataRead queueContains = " + data);
        }

        out.flush();

        if (queue.peek() == null) {
            ac.complete();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ReadListener#onDataAvailable()
     */
    @Override
    public void onDataAvailable() throws IOException {

        System.out.println("onDataAvailable : TestAsyncNBReadListener2 Start");

        if (testToPerfom.equalsIgnoreCase("NBReadStreamNBReadStreamServlet")) {

            // Read 5 bytes
            //int Len = 5;

            int Len = request.getContentLength();

            try {
                byte[] inBytes = new byte[(int) (Len - dataSizeofLen)];

                if (input.isReady()) {
                    Len = input.read(inBytes);
                    if (Len > 0) {
                        inBuffer.append(new String(inBytes, 0, Len));
                        dataSizeofLen += Len;
                    }
                }
            } catch (IOException exc) {
                responseString += "Exception reading post data " + exc + " :";
                System.out.println(responseString);
            }

            if (dataSizeofLen == Len) {

                dataSizeofLen = 0;

                if (inBuffer.toString().contains(inBufferDataFromPrevious)) {

//                }
//                if (inBufferDataFromPrevious.toString()..contentEquals(inBuffer)) {
                    pass = true;

                    System.out.println("onDataAvailable : data in buffer is same read from TestAsyncNBReadListener and TestAsyncNBReadListener2");

                    ServletOutputStream out = response.getOutputStream();
                    if (pass)
                        out.println("PASS : " + responseString);
                    else
                        out.println("FAIL : " + responseString);

                    LOG.info("onDataAvailable doWork finish for " + testToPerfom);
                } else {
                    System.out.println("onDataAvailable : data in buffer is not same from TestAsyncNBReadListener and TestAsyncNBReadListener2");

                }
                //ac.complete(); // cannot call onAllDataRead since reading only 5 bytes

            }

        }

        System.out.println("onDataAvailable : TestAsyncNBReadListener2 Finish");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ReadListener#onError(java.lang.Throwable)
     */
    @Override
    public void onError(Throwable arg0) {

        System.out.println("onError : TestAsyncNBReadListener2");

        try {
            ServletOutputStream out = response.getOutputStream();
            out.print("OnError method successfully called !!! " + arg0.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ac.complete();

    }

}
