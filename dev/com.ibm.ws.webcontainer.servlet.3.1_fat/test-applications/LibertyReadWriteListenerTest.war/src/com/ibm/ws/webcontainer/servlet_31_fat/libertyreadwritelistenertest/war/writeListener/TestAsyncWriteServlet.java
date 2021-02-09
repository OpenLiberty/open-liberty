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
package com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.writeListener;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/TestAsyncWriteServlet", asyncSupported = true)
public class TestAsyncWriteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TestAsyncWriteServlet.class.getName());
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<String>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        LOG.info("Entering TestAsyncWriteServlet: or TAWS");
        // start async
        AsyncContext ac = req.startAsync(req, res);
        // set up async listener
        ac.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info("TestAsyncWriteServlet: Complete");

            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info("TestAsyncWriteServlet:  " + event.getThrowable().toString());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info("TestAsyncWriteServlet: " + "my asyncListener.onStartAsync");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info("TestAsyncWriteServlet: " + "my asyncListener.onTimeout");
            }
        }, req, res);

        ServletOutputStream out = res.getOutputStream();
        String testToCall = req.getHeader("TestToCall").toString();
        LOG.info("TestToCall :  " + testToCall);

        if (testToCall.equals("TestWrite_DontCheckisReady_fromWL")) {

            String postDataSize = req.getHeader("ContentSizeSent");
            StringBuilder sb = new StringBuilder();
            LOG.info(testToCall + " TAWS:  Request has postDataSize : " + postDataSize);

            sb = this.createBufferTosend(postDataSize, sb);
            try {
                LOG.info(testToCall + " TAWS: Length of data to add in the queue is now " + sb.toString().length());
                q.put(sb.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);

            // This will call to write data success
            // then it will try to print more data without checking isReady, this will throw exception and onError will be called

        } else if (testToCall.equals("TestWriteFromServlet_AftersetWL")) {
            String postDataSize = req.getHeader("ContentSizeSent");
            StringBuilder sb = new StringBuilder();
            LOG.info(testToCall + " TAWS:  Request has postDataSize : " + postDataSize);
            sb = this.createBufferTosend(postDataSize, sb);
            try {
                LOG.info(testToCall + " TAWS: Length of data to add in the queue is now " + sb.toString().length());
                q.put(sb.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);
            // the print from the Listener shud work fine

//            if (out.isReady()) {
//                out.print(false);
//            }
//            else {
//                LOG.info(testToCall + " TAWS: isReady always false from the thread which sets the WL");
//            }

            // The following shud log Error in logs
            //Expected output :  SRVE0918E: The attempted blocking write is not allowed because the non-blocking I/O has already been started.
            LOG.info(testToCall + " TAWS: isReady not checked Printing output again from this thread , it shud not be allowed");
            out.print(false);
        } else if (testToCall.equals("TestWL_Println_Large")
                   || testToCall.equals("TestWL_Write_Large")
                   || testToCall.equals("TestWL_Write_Medium")
                   || testToCall.equals("Test_ISE_setSecondWriteListener")) {

            String postDataSize = req.getHeader("ContentSizeSent");
            StringBuilder sb = new StringBuilder();
            LOG.info(testToCall + " TAWS:  Request has postDataSize : " + postDataSize);

            sb = this.createBufferTosend(postDataSize, sb);

            LOG.info(testToCall + " TAWS: Length of data to add in the queue is now " + sb.toString().length());
            try {
                q.put(sb.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String type = req.getHeader("Type");
            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall, postDataSize, type);
            out.setWriteListener(writeListener);

            if (testToCall.equals("Test_ISE_setSecondWriteListener")) {
                LOG.info(testToCall + " TAWS: set second WriteListener now");
                try {
                    WriteListener writeListener1 = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
                    out.setWriteListener(writeListener1);
                } catch (final Exception e) {
                    // This must throw exception
                    //java.lang.IllegalStateException SRVE9009E";
                    e.printStackTrace();

                }

            }
        } else if (testToCall.equals("TestWL_Write_MediumChunks")
                   || testToCall.equals("TestWL_Println_MediumChunks")
                   || testToCall.equals("TestWL_Write_LargeChunks")
                   || testToCall.equals("TestWL_Println_LargeChunks")) {
            String postDataSize = req.getHeader("ContentSizeSent");
            try {

                ac.setTimeout(240000);

                //generate queue data
                StringBuilder sb = new StringBuilder();
                LOG.info(testToCall + " TAWS:  Request has postDataSize : " + postDataSize);
                if (postDataSize != null) {
                    byte[] b = new byte[10000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) 0x61; // building a byte buffer which will be used to build a large data set for the queue, that will be passed on to the writeListener
                    }
                    int postDataSizeInt = Integer.parseInt(postDataSize);
                    //LOG.info(testToCall+ " TAWS:  postDataSizeInt is " + postDataSizeInt);
                    int total = 0;
                    while (total < postDataSizeInt) {
                        if ((postDataSizeInt - total) < b.length) {
                            LOG.info(testToCall + " TAWS: postDataSizeInt -total is " + (postDataSizeInt - total) + " b.length is " + b.length);
                            sb.append(new String(b, 0, postDataSizeInt - total));

                            q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener

                            sb.delete(0, postDataSizeInt - total); //clear temporary buffer, used to append to the queue
                            total = postDataSizeInt;
                        } else {
                            sb.append(new String(b));

                            q.put(sb.toString()); // put small chunk, one of many, into queue to be sent to the writeListener
                            LOG.info(testToCall + " TAWS:  queue created " + q.size());

                            sb.delete(0, b.length); //clear temporary buffer, used to append to the queue
                            total += b.length;

                        }
                    }

                }
                LOG.info(testToCall + " TAWS: queue size, the number of elements, is now " + q.size());
                LOG.info(testToCall + " TAWS: queue length is " + q.toString().length());
            } catch (Exception e) {
                LOG.info(testToCall + " TAWS: exception in exception is " + e.toString() + " printing stack trace ... ");
                e.printStackTrace();
            }
            // send it to AsyncWriteListener
            String type = req.getHeader("Type");
            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall, postDataSize, type);
            out.setWriteListener(writeListener);

        } else if (testToCall.equals("Test_NPE_setNullWriteListener")) {
            try {
                LOG.info(testToCall + " TAWS: set null in WriteListener now");
                out.setWriteListener(null);
            } catch (final Exception e) {
                //// This will work since WL not set
                out.print(e.toString());
                out.flush();
                ac.complete();
            }
        }

        else if (testToCall.equals("Test_Return_onWritePossible")) {
            if (!q.isEmpty()) {
                q.poll();
            }

            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);
            LOG.info(testToCall + " TAWS: back from WriteListener now");
            // This shud not be allowed
            out.flush();
            ac.complete();
        }

        else if (testToCall.equals("TestWL_IOE_AfterWrite")) {
            if (!q.isEmpty()) {
                q.poll();
            }
            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);
        }

        else if (testToCall.equals("TestWL_onError")) {
            if (!q.isEmpty()) {
                q.poll();
            }
            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);

        }

        else if (testToCall.equals("TestWL_ContextTransferProperly")) {
            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, "TestWL_ContextTransferProperly");
            out.setWriteListener(writeListener);

        } else if (testToCall.equals("printBoolean") || testToCall.equals("printChar") || testToCall.equals("printInt") || testToCall.equals("printDouble")
                   || testToCall.equals("printFloat") || testToCall.equals("printLong")) {

            try {
                if (testToCall.equals("printBoolean"))
                    q.put("true");
                else if (testToCall.equals("printChar"))
                    q.put("a");
                else if (testToCall.equals("printDouble"))
                    q.put("12.34");
                else if (testToCall.equals("printFloat"))
                    q.put("3.14");
                else if (testToCall.equals("printInt"))
                    q.put("9");
                else if (testToCall.equals("printLong"))
                    q.put("10999079904000");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WriteListener writeListener = new TestAsyncWriteListener(out, q, ac, req, res, testToCall);
            out.setWriteListener(writeListener);

        }

        else {
            LOG.info("TestAsyncWriteServlet: Uknown Test");
        }

    }

    /**
     * @param postDataSize
     * @param sb
     * @return
     */
    private StringBuilder createBufferTosend(String postDataSize, StringBuilder sb) {
        if (postDataSize != null) {
            byte[] b = new byte[10000];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) 0x61;
            }
            int postDataSizeInt = Integer.parseInt(postDataSize);
            int total = 0;
            while (total < postDataSizeInt) {
                if ((postDataSizeInt - total) < b.length) {
                    sb.append(new String(b, 0, postDataSizeInt - total));
                    total = postDataSizeInt;
                } else {
                    sb.append(new String(b));
                    total += b.length;
                }
            }
        }
        sb.trimToSize();
        return sb;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
