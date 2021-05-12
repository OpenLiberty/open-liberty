/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.libertyreadwritelistenertest.war.readListener;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/TestAsyncReadServlet", asyncSupported = true)
public class TestAsyncReadServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TestAsyncReadServlet.class.getName());
    private String testToCall;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info("TestAsyncReadServlet onComplete() called");
                LOG.info("/************ [TestRequestProperty]: " + testToCall + " Finish ************/");
                LOG.info("/*************************************************************************************/");

            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info("TestAsyncReadServlet onError() " + event.getThrowable().toString());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info("TestAsyncReadServlet onStartAsync() " + "my asyncListener.onStartAsync");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info("TestAsyncReadServlet onTimeout() " + "my asyncListener.onTimeout");
            }
        }, req, res);

        // set up ReadListener to read data for processing
        ServletInputStream input = req.getInputStream();

        testToCall = req.getHeader("TestToCall");
        LOG.info("/*************************************************************************************/");
        LOG.info("/************ [TestRequestProperty]: " + testToCall + " Start ************/");

        if (req.getHeader("TestToCall").toString().equals("test_OnError_AsyncRL")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_OnError_AsyncRL");
            input.setReadListener(readListener);
        } else if (req.getHeader("TestToCall").toString().equals("test_Exception_onSecondReadListener")) {
            ServletOutputStream err = res.getOutputStream();

            LOG.info("TestAsyncReadServlet, setting the first ReadListener");
            TestAsyncReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_Exception_onSecondReadListener");
            LOG.info("TestAsyncReadServlet, arming latch to signal the test is complete when we write the expected exception");
            readListener.armTestCompleteLatch();
            input.setReadListener(readListener);

            try {
                LOG.info("TestAsyncReadServlet, setting the second ReadListener");
                ReadListener readListener1 = new TestAsyncReadListener(input, res, ac, req, "test_Exception_onSecondReadListener");
                input.setReadListener(readListener1);
            } catch (final Exception e) {
                err.print(e.toString());
                LOG.info("TestAsyncReadServlet, Expected error encountered : " + e.toString());
                LOG.info("TestAsyncReadServlet, Notifying the ReadListener the test is complete");
                readListener.hitTestCompleteLatch();
            }
        }

        else if (req.getHeader("TestToCall").toString().equals("test_Exception_onNullReadListener")) {
            ServletOutputStream err = res.getOutputStream();
            try {
                LOG.info("TestAsyncReadServlet, setting a null ReadListener");
                input.setReadListener(null);
            } catch (final Exception e) {
                err.print(e.toString());

                //Call ac.complete here since we are expecting the exception and we want to set a null readListener
                ac.complete();
            }
        }

        else if (req.getHeader("TestToCall").toString().equals("test_ReadVariousInputDataSizes_AsyncRL")) {
            if (req.getHeader("TestInputData").toString().equals("1024000"))
                ac.setTimeout(75000); //set longer timeout in case of network latency, issue 17013

            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_ReadVariousInputDataSizes_AsyncRL");
            input.setReadListener(readListener);

        }

        else if (req.getHeader("TestToCall").toString().equals("test_Exception_onReadingData_isReadyFalse")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_Exception_onReadingData_isReadyFalse");
            input.setReadListener(readListener);
        }

        else if (req.getHeader("TestToCall").toString().equals("test_HandleException_ThrownByOnDataAvailable")) {
            ServletOutputStream output = res.getOutputStream();
            try {

                ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_HandleException_ThrownByOnDataAvailable");
                input.setReadListener(readListener);
            } catch (Exception e) {
                output.print(e.toString());
            }

        }

        else if (req.getHeader("TestToCall").toString().equals("test_ReadData_BeforeRL")) {
            final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            ServletOutputStream err = res.getOutputStream();
            try {

                StringBuilder sb = new StringBuilder();
                byte b[] = new byte[1024];
                int len = -1;
                while ((len = input.read(b)) != -1) {
                    String data = new String(b, 0, len);
                    sb.append(data);
                    queue.add(sb.toString());
                }
                ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_ReadData_BeforeRL");
                LOG.info("TestAsyncReadServlet, setting a Queue");
                ((TestAsyncReadListener) readListener).setQueue(queue);
                input.setReadListener(readListener);
                //input.close(); , if we close here that makes listener null and cannot call onAllDataRead
            } catch (Exception e) {
                e.printStackTrace();
                err.print(e.toString());

            }
        } else if (req.getHeader("TestToCall").toString().equals("test_HandleException_ThrownByOnAllDataRead")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_HandleException_ThrownByOnAllDataRead");
            input.setReadListener(readListener);
        }

        else if (req.getHeader("TestToCall").toString().equals("test_Exception_setRL_onNonAsyncServlet")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, null, req, "test_Exception_setRL_onNonAsyncServlet");
            input.setReadListener(readListener);
        }

        else if (req.getHeader("TestToCall").toString().equals("test_ReadData_onDataAvailableReturn")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_ReadData_onDataAvailableReturn");
            input.setReadListener(readListener);

        } else if (req.getHeader("TestToCall").toString().equals("test_OnReadParameter_WhenRLset")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_OnReadParameter_WhenRLset");
            input.setReadListener(readListener);
        } else if (req.getHeader("TestToCall").toString().equals("test_ContextTransferProperly_WhenRLset")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_ContextTransferProperly_WhenRLset");
            input.setReadListener(readListener);
        } else if (req.getHeader("TestToCall").toString().equals("test_IsReadyAfterIsReadyFalse")) {
            ReadListener readListener = new TestAsyncReadListener(input, res, ac, req, "test_IsReadyAfterIsReadyFalse");
            input.setReadListener(readListener);
        }

    }
}
