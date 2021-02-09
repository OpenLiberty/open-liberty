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
package com.ibm.ws.webcontainer.servlet_31_fat.libertywritelistenerfiltertest.war.writeListener;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class WriteListenerFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(WriteListenerFilter.class.getName());
    private final LinkedBlockingQueue<String> q = new LinkedBlockingQueue<String>();

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain fc) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        //Get the IP address of client machine.
        String ipAddress = request.getRemoteAddr();

        //Log the IP address and current timestamp.
        LOG.info("WriteListenerFilter In filter IP " + ipAddress + ", Time " + new Date().toString());

        // set up ReadListener to read data for processing
        // start async
        AsyncContext ac = req.startAsync();
        // set up async listener
        ac.addListener(new AsyncListener() {

            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                LOG.info("WriteListenerFilter Filter Complete");

            }

            @Override
            public void onError(AsyncEvent event) {
                LOG.info("WriteListenerFilter" + event.getThrowable().toString());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                LOG.info(" WriteListenerFilter my asyncListener.onStartAsync");
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                LOG.info("WriteListenerFilter Filter my asyncListener.onTimeout");
            }
        }, req, res);

        @SuppressWarnings("unused")
        ServletInputStream input = req.getInputStream();
        ServletOutputStream out = res.getOutputStream();

        String postDataSize = request.getHeader("ContentSizeSent");
        String testToCall = request.getHeader("TestToCall").toString();
        LOG.info("TestToCall :  " + testToCall);

        if (testToCall.equals("TestWriteFromFilter_AftersetWL")) {
            StringBuilder sb = new StringBuilder();
            LOG.info(testToCall + " WLF:  Request has postDataSize : " + postDataSize);
            sb = this.createBufferTosend(postDataSize, sb);
            try {
                LOG.info(testToCall + " WLF: Length of data to add in the queue is now " + sb.toString().length());
                q.put(sb.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            WriteListener writeListener = new TestAsyncFilterWriteListener(out, q, ac);
            out.setWriteListener(writeListener);
            // the print from the Listener shud work fine

//            if (out.isReady()) {
//                out.print(false);
//            }
//            else {
//                LOG.info(testToCall + " WLF: isReady always false from the thread which sets the WL");
//            }

            // The following shud log Error in logs
            //Expected output :  SRVE0918E: The attempted blocking write is not allowed because the non-blocking I/O has already been started.
            LOG.info(testToCall + " WLF: isReady not checked Printing output again from this thread , it shud not be allowed");
            out.print(false);
        } else if (testToCall.equals("TestWL_Write_Less_InFilter")) {

            StringBuilder sb = new StringBuilder();
            LOG.info(testToCall + " WLF:  Request has postDataSize : " + postDataSize);
            sb = this.createBufferTosend(postDataSize, sb);
            try {
                LOG.info(testToCall + " WLF: Length of data to add in the queue is now " + sb.toString().length());
                q.put(sb.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            WriteListener writeListener = new TestAsyncFilterWriteListener(out, q, ac);
            out.setWriteListener(writeListener);

        }
        // call chain
        fc.doFilter(req, res);

    }

    @Override
    public void init(FilterConfig fc) throws ServletException {
        //Get init parameter
        String testParam = fc.getInitParameter("test-param");

        //Print the init parameter
        LOG.info("WriteListenerFilter In Filter,Test Param: " + testParam);

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

}
