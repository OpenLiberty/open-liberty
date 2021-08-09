/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics30.internal.helper;

import java.io.Writer;
import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.microprofile.metrics.WriterFactory;
import com.ibm.ws.microprofile.metrics.writer.JSONMetadataWriter;
import com.ibm.ws.microprofile.metrics.writer.JSONMetricWriter;
import com.ibm.ws.microprofile.metrics.writer.PrometheusMetricWriter;

import io.openliberty.microprofile.metrics30.internal.writer.JSONMetadataWriter30;
import io.openliberty.microprofile.metrics30.internal.writer.JSONMetricWriter30;
import io.openliberty.microprofile.metrics30.internal.writer.PrometheusMetricWriter30;

@Component(service = { WriterFactory.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class WriterFactoryImpl30 implements WriterFactory {

    /** {@inheritDoc} */
    @Override
    public JSONMetricWriter getJSONMetricWriter(Writer writer) {
        return new JSONMetricWriter30(writer);
    }

    /** {@inheritDoc} */
    @Override
    public JSONMetadataWriter getJSONMetadataWriter(Writer writer, Locale locale) {
        return new JSONMetadataWriter30(writer, locale);
    }

    /** {@inheritDoc} */
    @Override
    public PrometheusMetricWriter getPrometheusMetricsWriter(Writer writer, Locale locale) {
        return new PrometheusMetricWriter30(writer, locale);
    }

}
