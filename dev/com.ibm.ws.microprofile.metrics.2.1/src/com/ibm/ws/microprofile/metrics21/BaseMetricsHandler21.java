/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics21;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import com.ibm.ws.microprofile.metrics.BaseMetricsHandler;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.exceptions.HTTPMethodNotAllowedException;
import com.ibm.ws.microprofile.metrics.exceptions.HTTPNotAcceptableException;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics.writer.OutputWriter;
import com.ibm.ws.microprofile.metrics21.writer.JSONMetricWriter21;
import com.ibm.ws.microprofile.metrics21.writer.PrometheusMetricWriter21;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@SuppressWarnings("restriction")
public class BaseMetricsHandler21 extends BaseMetricsHandler {

    protected BaseMetrics21 bm21;

    @Override
    protected OutputWriter getOutputWriter(RESTRequest request, RESTResponse response,
            Locale locale) throws IOException, HTTPNotAcceptableException, HTTPMethodNotAllowedException {
        String method = request.getMethod();
        String accept = request.getHeader(Constants.ACCEPT_HEADER);
        Writer writer = response.getWriter();

        if (accept == null) {
            accept = Constants.ACCEPT_HEADER_TEXT;
        }

        if (Constants.METHOD_GET.equals(method)) {
            AcceptableFormat theAcceptableFormat = null;
            String[] acceptHeaderSplit = accept.split(",");
            for (String individualAcceptHeader : acceptHeaderSplit) {
                if (individualAcceptHeader.contains(Constants.ACCEPT_HEADER_TEXT) || individualAcceptHeader.contains(Constants.ACCEPT_HEADER_ANYTHING)) {
                    theAcceptableFormat = compareAF(theAcceptableFormat, individualAcceptHeader, Constants.ACCEPT_HEADER_TEXT);
                } else if (individualAcceptHeader.contains(Constants.ACCEPT_HEADER_JSON)) {
                    theAcceptableFormat = compareAF(theAcceptableFormat, individualAcceptHeader, Constants.ACCEPT_HEADER_JSON);
                } else {
                    continue;
                }
            }

            if (theAcceptableFormat != null && theAcceptableFormat.getAcceptableFormat().equals(Constants.ACCEPT_HEADER_TEXT)) {
                return new PrometheusMetricWriter21(writer, locale);
            } else if (theAcceptableFormat != null && theAcceptableFormat.getAcceptableFormat().equals(Constants.ACCEPT_HEADER_JSON)) {
                return new JSONMetricWriter21(writer);
            } else {
                //invalid header
                throw new HTTPNotAcceptableException();
            }
        } else if (Constants.METHOD_OPTIONS.equals(method)) {
            if (accept.contains(Constants.ACCEPT_HEADER_JSON)) {
                return new JSONMetadataWriter(writer, locale);
            } else {
                throw new HTTPNotAcceptableException();
            }
        } else {
            throw new HTTPMethodNotAllowedException();
        }
    }
}
