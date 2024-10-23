/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package trailers.listeners;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ReadListenerGetTrailers implements ReadListener {

    private ServletInputStream input = null;
    private HttpServletResponse res = null;
    private HttpServletRequest request = null;
    private AsyncContext ac = null;
    private PrintWriter pw = null;
    private boolean usingNetty;

    private static final Logger LOG = Logger.getLogger(ReadListenerGetTrailers.class.getName());

    public ReadListenerGetTrailers(ServletInputStream in, HttpServletResponse r,
                                   AsyncContext c, HttpServletRequest req, boolean usingNetty) {
        input = in;
        res = r;
        ac = c;
        request = req;
        this.usingNetty = usingNetty;
    }

    @Override
    public void onDataAvailable() throws IOException {

        if (pw == null)
            pw = res.getWriter();

        pw.println("ReadListenerGetTrailers onDataAvailable method called");

        if(!usingNetty){
            if (request.isTrailerFieldsReady()) {
                pw.println("FAIL : isTrailerFieldsReady() returned true before data was read.");
            } else {
                pw.println("PASS : isTrailerFieldsReady() returned false before data was read.");
                try {
                    request.getTrailerFields();
                    pw.println("FAIL : getTrailerFields() did not throw IllegalStateException before data was read.");
                } catch (IllegalStateException ise) {
                    pw.println("PASS : getTrailerFields() threw IllegalStateException before data was read.");
                }
            }
        } else if(!request.isTrailerFieldsReady()){
            pw.println("FAIL : isTrailerFieldsReady() returned false while using Netty.");
        }

        int len = -1;
        byte b[] = new byte[1024];

        while (input.isReady() && (len = input.read(b)) != -1) {
            LOG.info("ReadListenerGetTrailers onDataAvailable, isReady true num bytes read : " + len);
        }

    }

    @Override
    public void onAllDataRead() throws IOException {

        pw.println("ReadListenerGetTrailers onAllDataRead method called");

        if (request.isTrailerFieldsReady()) {
            pw.println("PASS : isTrailerFieldsReady() returned true after data was read.");
            try {
                Map<String, String> trailers = request.getTrailerFields();
                pw.println("PASS : getTrailerFields() did not throw IllegalStateException before data was read.");

                Iterator<String> trailerIterator = trailers.keySet().iterator();

                while (trailerIterator.hasNext()) {
                    String trailerName = trailerIterator.next();
                    pw.println("Trailer field found :  " + trailerName + " = " + trailers.get(trailerName));
                }

            } catch (IllegalStateException ise) {
                pw.println("FAIL : isTrailerFieldsReady() theow IllegalStateException before data was read.");
            }

        } else {
            pw.println("FAIL : isTrailerFieldsReady() returned false after data was read.");
        }

        ac.complete();

    }

    @Override
    public void onError(final Throwable t) {
        LOG.info("FAIL: TestAsyncReadListener OnError called");
        ac.complete();
    }

}
