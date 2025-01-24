/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package netty.readlistener.listeners;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

public class NettyReadListener implements ReadListener {

    private ServletInputStream input = null;

    private ServletOutputStream output = null;

    private AsyncContext ac = null;

    public NettyReadListener(ServletInputStream in, ServletOutputStream out, AsyncContext c) {
        input = in;
        output = out;
        ac = c;
    }

    @Override
    public void onDataAvailable() {
        System.out.println("PAN: onDataAvailable called");
        Thread.dumpStack();
        try {
            StringBuilder sb = new StringBuilder();
            output.println("=onDataAvailable");
            int len = -1;
            byte b[] = new byte[1024];
            while (input.isReady() && (len = input.read(b)) != -1) {
                String data = new String(b, 0, len);
                sb.append("=").append(data);
            }
            output.print(sb.toString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void onAllDataRead() {
        System.out.println("PAN: onAllDataRead called");
        Thread.dumpStack();
        try {
            output.println("=onAllDataRead");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            ac.complete();
        }
    }

    @Override
    public void onError(final Throwable t) {

        System.out.println("PAN: onError called");
        ac.complete();
        t.printStackTrace();
    }
}
