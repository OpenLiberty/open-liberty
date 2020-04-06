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
package com.ibm.ws.fat.wc.servlet31.readListener;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class TestAsyncReadListener implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse res = null;
    private HttpServletRequest request = null;
    private AsyncContext ac = null;
    private String TestCall = "";
    long dataSize = 0;
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    private boolean testComplete = false;
    CountDownLatch testCompleteLatch = null;

    private static final Logger LOG = Logger.getLogger(TestAsyncReadListener.class.getName());

    TestAsyncReadListener(ServletInputStream in, HttpServletResponse r,
                          AsyncContext c, HttpServletRequest req, String s) {
        input = in;
        res = r;
        ac = c;
        request = req;
        TestCall = s;
    }

    @Override
    public void onDataAvailable() throws IOException {
        LOG.info("TestAsyncReadListener onDataAvailable method successfully called  for " + TestCall);

        int len = -1;
        byte b[] = new byte[1024];
        ServletOutputStream out = res.getOutputStream();

        if (!input.isReady()) {
            LOG.info(TestCall + " onDataAvailable, isReady=false");
        }

        if (TestCall.equalsIgnoreCase("test_OnError_AsyncRL")) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
                out.print(data);
                LOG.info("TestAsyncReadListener onDataAvailable, read data --> " + data);
            }
        }

        else if (TestCall.equalsIgnoreCase("test_ReadVariousInputDataSizes_AsyncRL")) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                dataSize += len;

            }

            res.addHeader("PostDataRead", Long.toString(dataSize));
            LOG.info("TestAsyncReadListener onDataAvailable, read datasize --> " + Long.toString(dataSize));
        }

        else if (TestCall.equalsIgnoreCase("test_Exception_onReadingData_isReadyFalse") && !this.testComplete) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                //We just want to loop through the data and not much else. The point is not the data,
                //but what happens when isReady is false
                LOG.info("test_Exception_onReadingData_isReadyFalse onDataAvailable, isReady true num bytes read : " + len);
                // String data = new String(b, 0, len);
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
            }

            LOG.info("test_Exception_onReadingData_isReadyFalse onDataAvailable, read all the data available, isReady should be false");
            try {
                LOG.info("test_Exception_onReadingData_isReadyFalse onDataAvailable, running read, expected to fail");
                //An exception is expected here
                //We are going to catch the exception and pass it back to the client
                len = input.read(b);
            } catch (IllegalStateException ise) {
                LOG.info("test_Exception_onReadingData_isReadyFalse onDataAvailable, caught exception : " + ise);
                testComplete = true;
                res.setStatus(500);
                out.print(ise.toString());
            }
        }

        else if (TestCall.equalsIgnoreCase("test_IsReadyAfterIsReadyFalse") && !this.testComplete) {
            LOG.info("test_IsReadyAfterIsReadyFalse onDataAvailable");
            while (input.isReady() && (len = input.read(b)) != -1) {
                //We just want to loop through the data and not much else. The point is not the data,
                //but what happens after isReady is false
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
                //out.print(data);
                LOG.info("test_IsReadyAfterIsReadyFalse onDataAvailable, length read : " + len);
            }
            try {
                //Prior to the code change in 173921 we were receiving this exception:
                //com.ibm.wsspi.http.channel.exception.IllegalHttpBodyException: Illegal chunk length digit: 0
                //Now we should properly pass the isReady() call and receive the exception IllegalStateException
                if (!input.isReady()) {
                    LOG.info("testisReadyAfterisReadyFalse onDataAvailable, isReady=false");
                    //If we get to this point we are expecting it to fail with an IllegalStateException which states isReady() is false
                    len = input.read(b);
                }
            } catch (IllegalStateException ise) {
                //Here we want to package up and send out the exception, no matter the contents
                LOG.info("test_IsReadyAfterIsReadyFalse onDataAvailable, caught exception : " + ise);
                testComplete = true;
                out.print(ise.toString());
            }
        }

        else if (TestCall.equalsIgnoreCase("test_HandleException_ThrownByOnDataAvailable")) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
                out.print(data);
            }

            out.flush();

            LOG.info("test_HandleException_ThrownByOnDataAvailable onDataAvailable, throws an Exception");
            throw new IOException("Exception thrown from onDataAvailable");

        }

        else if (TestCall.equalsIgnoreCase("test_ReadData_BeforeRL")) {
            //We shouldn't actually read anything here since we read it all before we got to this point.

            while (queue.peek() != null) {
                String s = queue.poll();
                out.print(s);
                LOG.info("TestAsyncReadListener onDataAvailable, read queue --> " + s);
            }
            out.print("All Data Read from client before ReadListener invoked!");

        } else if (TestCall.equalsIgnoreCase("test_HandleException_ThrownByOnAllDataRead")) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
                out.print(data);
                LOG.info("TestAsyncReadListener onDataAvailable, read data --> " + data);
            }
        }

        else if (TestCall.equalsIgnoreCase("test_Exception_setRL_onNonAsyncServlet")) {
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                // queue.add(data); // commenting out the queue(used to collate input data, and pass to another method or another listener), for now, may be uncommented for future use
                out.print(data);
                LOG.info("TestAsyncReadListener onDataAvailable, read data --> " + data);
            }
        }

        else if (TestCall.equalsIgnoreCase("test_ReadData_onDataAvailableReturn")) {
            return;
        }

        else if (TestCall.equalsIgnoreCase("test_Exception_onSecondReadListener")) {
            LOG.info("test_Exception_onSecondReadListener onDataAvailable, Nothing we need to do");
        }

        else if (TestCall.equalsIgnoreCase("test_OnReadParameter_WhenRLset")) {
            // The testcase will only getParameter from the queryList since Inputstream has already been taken, post getParameter will be ignored
            LOG.info("test_OnReadParameter_WhenRLset onDataAvailable call getParameter");
            String param = request.getParameter("parameter");

            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                LOG.info("TestAsyncReadListener test_OnReadParameter_WhenRLset onDataAvailable, read data --> " + data);

                out.println("parameter=" + param);
                LOG.info("test_OnReadParameter_WhenRLset onDataAvailable parameter=" + param);
            }
            out.flush();
        }

        else if (TestCall.equalsIgnoreCase("test_ContextTransferProperly_WhenRLset")) {
            LOG.info("test_ContextTransferProperly_WhenRLset onDataAvailable, trying a context lookup");

            try {
                //This is expected to fail. We will write back the exception back to the client
                Context ctx = new InitialContext();
                @SuppressWarnings("unused")
                DataSource ds = (DataSource) ctx.lookup("java:comp/UserTransaction");
                LOG.info("test_ContextTransferProperly_WhenRLset onDataAvailable, Successfully completed the lookup");
            } catch (Exception e) {
                LOG.info("test_ContextTransferProperly_WhenRLset onDataAvailable, Expected exception occurred while doing the initialContext lookup : " + e);

                out.println(e.toString());
                out.flush();
            } finally {
                while (input.isReady() && (len = input.read(b)) != -1) {
                    // read data to force onAllDataRead()
                }
            }
        }

    }

    @Override
    public void onAllDataRead() throws IOException {
        LOG.info("TestAsyncReadListener onAllDataRead method successfully called  for " + TestCall);
        ServletOutputStream out = res.getOutputStream();

        if (TestCall.equalsIgnoreCase("test_OnError_AsyncRL")) {
            out.flush();
            ac.complete();

        } else if (TestCall.equalsIgnoreCase("test_ReadVariousInputDataSizes_AsyncRL")) {
            out.flush();
            ac.complete();

        } else if (TestCall.equalsIgnoreCase("test_Exception_onReadingData_isReadyFalse")) {
            out.flush();
            ac.complete();

        } else if (TestCall.equalsIgnoreCase("test_ReadData_BeforeRL")) {
            out.flush();
            ac.complete();

        } else if (TestCall.equalsIgnoreCase("test_HandleException_ThrownByOnAllDataRead")) {
            LOG.info("test_HandleException_ThrownByOnAllDataRead onAllDataRead, throws an Exception");
            //Throw an Exception. We expect the exception to be sent over to onError
            throw new IOException("Exception thrown from onAllDataRead");

        } else if (TestCall.equalsIgnoreCase("test_Exception_setRL_onNonAsyncServlet")) {
            out.flush();
        } else if (TestCall.equalsIgnoreCase("test_OnReadParameter_WhenRLset")) {
            // this moved from onAllDataAvailable
            ac.complete();
        } else if (TestCall.equalsIgnoreCase("test_Exception_onSecondReadListener")) {
            try {
                LOG.info("test_Exception_onSecondReadListener onAllDataRead, Need to call onClose, but will await until the exception is written out");
                testCompleteLatch.await();
            } catch (InterruptedException ie) {
                LOG.info("test_Exception_onSecondReadListener onAllDataRead, InterruptedException caught : " + ie);
            }
            LOG.info("test_Exception_onSecondReadListener onAllDataRead, test complete, calling ac.complete()");
            ac.complete();
        } else if (TestCall.equalsIgnoreCase("test_ContextTransferProperly_WhenRLset")) {
            ac.complete();
            LOG.info("test_ContextTransferProperly_WhenRLset onAllDataRead, calling ac.complete()");
        }

    }

    @Override
    public void onError(final Throwable t) {
        LOG.info("TestAsyncReadListener OnError method successfully called  for " + TestCall);

        try {
            ServletOutputStream out = res.getOutputStream();

            if (TestCall.equalsIgnoreCase("test_HandleException_ThrownByOnAllDataRead")
                || TestCall.equalsIgnoreCase("test_HandleException_ThrownByOnDataAvailable")) {
                out.println(", " + t.getMessage());
            }
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LOG.info("TestAsyncReadListener onError: " + t.getMessage());
            ac.complete();
        }
    }

    public void setQueue(LinkedBlockingQueue<String> newQueue) {
        LOG.info("TestAsyncReadListener setQueue: " + newQueue.toString());
        queue = newQueue;
    }

    public void armTestCompleteLatch() {
        testCompleteLatch = new CountDownLatch(1);
    }

    public void hitTestCompleteLatch() {
        testCompleteLatch.countDown();
    }
}
