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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 *
 */
public class TestAsyncFilterWriteListener implements WriteListener {

    private static final Logger LOG = Logger.getLogger(TestAsyncFilterWriteListener.class.getName());

    private ServletOutputStream output = null;
    private AsyncContext ac = null;

    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();

    /**
     * @param q
     * @param out
     * @param ac2
     * @param postDataSize
     */
    public TestAsyncFilterWriteListener(ServletOutputStream s, LinkedBlockingQueue<String> q, AsyncContext c) {
        output = s;
        queue = q;
        ac = c;
    }

    @Override
    public void onWritePossible() throws IOException {

        LOG.info("TestAsyncFilterWriteListener: queue length is " + queue.toString().length());

        while ((queue.peek() != null) && (output.isReady())) {
            try {

                LOG.info("TestAsyncFilterWriteListener: write output bytes");
                output.write(queue.take().getBytes());
                LOG.info("TestAsyncFilterWriteListener: Done writing output , queue size left -->" + queue.size());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        if ((queue.peek() == null)) {
            if (output.isReady()) {
                ac.complete();
                LOG.info("TestAsyncFilterWriteListener: Finished call to ac.complete");
            } else {
                LOG.info("TestAsyncFilterWriteListener:  out may not be ready , cannot complete");
            }

        }
    }

    @Override
    public void onError(final Throwable t) {
        LOG.info("TestAsyncFilterWriteListener onError");

        String outError = t.getMessage();
        if (output.isReady()) {
            try {
                output.print("TestAsyncFilterWriteListener onError method is called ! " + outError);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                ac.complete();
            }
        }

    }
}
