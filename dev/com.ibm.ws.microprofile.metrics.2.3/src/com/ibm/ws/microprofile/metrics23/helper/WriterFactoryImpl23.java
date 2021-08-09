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
package com.ibm.ws.microprofile.metrics23.helper;

import java.io.Writer;
import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.microprofile.metrics.WriterFactory;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics.writer.PrometheusMetricWriter;
import com.ibm.ws.microprofile.metrics23.writer.JSONMetricWriter23;
import com.ibm.ws.microprofile.metrics23.writer.PrometheusMetricWriter23;

@Component(service = { WriterFactory.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class WriterFactoryImpl23 implements WriterFactory {

    /** {@inheritDoc} */
    @Override
    public JSONMetricWriter getJSONMetricWriter(Writer writer) {
        return new JSONMetricWriter23(writer);
    }

    /** {@inheritDoc} */
    @Override
    public JSONMetadataWriter getJSONMetadataWriter(Writer writer, Locale locale) {
        return new JSONMetadataWriter(writer, locale);
    }

    /** {@inheritDoc} */
    @Override
    public PrometheusMetricWriter getPrometheusMetricsWriter(Writer writer, Locale locale) {
        return new PrometheusMetricWriter23(writer, locale);
    }

}
