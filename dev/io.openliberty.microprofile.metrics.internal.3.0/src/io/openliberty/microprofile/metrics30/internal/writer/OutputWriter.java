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
package io.openliberty.microprofile.metrics30.internal.writer;

import java.io.IOException;

import com.ibm.ws.microprofile.metrics.exceptions.EmptyRegistryException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchMetricException;
import com.ibm.ws.microprofile.metrics.exceptions.NoSuchRegistryException;

/**
 *
 */
public interface OutputWriter {

    /**
     *
     * @param registryName
     * @param metricName
     * @throws NoSuchRegistryException
     * @throws NoSuchMetricException
     * @throws IOException
     * @throws EmptyRegistryException
     */
    public void write(String registryName, String metricName) throws NoSuchRegistryException, NoSuchMetricException, IOException, EmptyRegistryException;

    /**
     *
     * @param registryName
     * @throws NoSuchRegistryException
     * @throws EmptyRegistryException
     * @throws IOException
     */
    public void write(String registryName) throws NoSuchRegistryException, EmptyRegistryException, IOException;

    /**
     *
     * @throws IOException
     */
    public void write() throws IOException;
}
