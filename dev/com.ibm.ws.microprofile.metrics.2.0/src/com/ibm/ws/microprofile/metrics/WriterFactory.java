/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics;

import java.io.Writer;
import java.util.Locale;

import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics.writer.PrometheusMetricWriter;

/**
 *
 */
public interface WriterFactory {

    public PrometheusMetricWriter getPrometheusMetricsWriter(Writer writer, Locale locale);

    public JSONMetricWriter getJSONMetricWriter(Writer writer);

    public JSONMetadataWriter getJSONMetadataWriter(Writer writer, Locale locale);
}
