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
package web;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestAsyncReadListener implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse res = null;
    private HttpServletRequest request = null;
    private AsyncContext ac = null;
    private String TestCall = "";
    long dataSize = 0;
    private String inData;

    private static final Logger LOG = Logger.getLogger(TestAsyncReadListener.class.getName());

    TestAsyncReadListener(ServletInputStream in, HttpServletResponse r,
                          AsyncContext c, HttpServletRequest req, String test) {
        input = in;
        res = r;
        ac = c;
        request = req;
        TestCall = test;
        inData = new String("");
    }

    @Override
    public void onDataAvailable() throws IOException {
        LOG.info("TestAsyncReadListener onDataAvailable method successfully called  for " + TestCall);

        int len = 0;
        byte b[] = new byte[1024];

        if (TestCall.toLowerCase().contains("ondataavailableexception")) {
            throw new IOException(TestCall + " : IOException thrown");
        }

        while (input.isReady() && len != -1) {
            LOG.info("TestAsyncReadListener onDataAvailable method is ready() is true. do a read");
            if ((len = input.read(b)) != -1) {
                LOG.info("TestAsyncReadListener onDataAvailable data length read = " + len);
                dataSize += len;
                byte[] dataRead = new byte[len];
                System.arraycopy(b, 0, dataRead, 0, len);
                inData += new String(dataRead);
                LOG.info("TestAsyncReadListener onDataAvailable data length read = " + len + ", data = " + new String(dataRead));
            } else {
                LOG.info("TestAsyncReadListener onDataAvailable read returned : " + len);
            }
        }

    }

    @Override
    public void onAllDataRead() throws IOException {
        LOG.info("TestAsyncReadListener onAllDataRead method successfully called  for " + TestCall);

        if (TestCall.toLowerCase().contains("onalldatareadexception")) {
            throw new IOException(TestCall + " : IOException thrown");
        }

        ServletOutputStream out = res.getOutputStream();
        out.print(inData);
        out.flush();
        ac.complete();

    }

    @Override
    public void onError(final Throwable t) {
        LOG.info("TestAsyncReadListener OnError method called  for " + TestCall);

        try {
            ServletOutputStream out = res.getOutputStream();
            out.print("onError called : " + t.getMessage());
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            LOG.info("TestAsyncReadListener onError: " + t.getMessage());
            ac.complete();
        }
    }

}
