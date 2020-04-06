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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class TestAsyncFilterReadListener implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse res = null;
    private AsyncContext ac = null;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    private static final Logger LOG = Logger.getLogger(TestAsyncFilterReadListener.class.getName());

    TestAsyncFilterReadListener(ServletInputStream in, HttpServletResponse r,
                                AsyncContext c) {
        input = in;
        res = r;
        ac = c;

    }

    @Override
    public void onDataAvailable() throws IOException {
        StringBuilder sb = new StringBuilder();
        int len = -1;
        byte b[] = new byte[1024];

        if (!input.isReady()) {
            LOG.info("onDataAvailable, isReady=false");
        }

        ServletOutputStream out = res.getOutputStream();

        while (!input.isFinished() && input.isReady() && (len = input.read(b)) != -1) {
            String data = new String(b, 0, len);
            sb.append(data);
            out.print(data);
            queue.add(sb.toString());
        }

    }

    @Override
    public void onAllDataRead() throws IOException {

        ServletOutputStream out = res.getOutputStream();
        while (queue.peek() != null) {
            String data = queue.poll();
            LOG.info("onAllDataRead queueContains = " + data);
        }

        out.flush();

        if (queue.peek() == null) {
            ac.complete();
        }
    }

    @Override
    public void onError(final Throwable t) {
        try {
            ServletOutputStream out = res.getOutputStream();
            out.print("OnError method successfully called !!!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ac.complete();
    }
}
