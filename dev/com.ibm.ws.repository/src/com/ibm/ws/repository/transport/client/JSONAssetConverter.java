/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.transport.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.model.Asset;

/**
 * This class contains utilities for converting {@link Asset}s to and from JSON
 */
public class JSONAssetConverter {

    /**
     * Write a JSON representation of the asset to a stream
     *
     * @param stream
     *            The stream to write to
     * @param pojo
     *            The asset to write
     * @throws IOException
     */
    public static void writeValue(OutputStream stream, Object pojo) throws IOException {
        DataModelSerializer.serializeAsStream(pojo, stream);
    }

    /**
     * Write a JSON representation of the asset to a stream
     *
     * @param stream
     *            The stream to write to
     * @param pojo
     *            The asset to write
     * @throws IOException
     */
    public static String writeValueAsString(Object pojo) throws IOException {
        return DataModelSerializer.serializeAsString(pojo);
    }

    /**
     * Read a list of assets from an input stream
     *
     * @param inputStream
     *            The stream to read from
     * @return The list of assets
     * @throws IOException
     */
    public static List<Asset> readValues(InputStream inputStream) throws IOException {
        return DataModelSerializer.deserializeList(inputStream, Asset.class);
    }

    /**
     * Read a single assets from an input stream
     *
     * @param inputStream
     *            The stream to read from
     * @return The list of assets
     * @throws IOException
     * @throws BadVersionException
     */
    public static Asset readValue(InputStream inputStream) throws IOException, BadVersionException {
        return DataModelSerializer.deserializeObject(inputStream, Asset.class);
    }

    /**
     * Read a single assets from an input stream without verifying it (e.g. its version)
     * CAUTION: Only use this if you are going to delete the Asset.
     * Package-level visibility to keep usage to a minimum.
     *
     * @param inputStream
     *            The stream to read from
     * @return The list of assets
     * @throws IOException
     */
    static Asset readUnverifiedValue(InputStream inputStream) throws IOException {
        return DataModelSerializer.deserializeObjectWithoutVerification(inputStream, Asset.class);
    }

    /**
     * Reads in a single object from a JSON input stream
     * 
     * @param inputStream The input stream containing the JSON object
     * @param type The type of the object to be read from the stream
     * @return The object
     * @throws IOException
     */
    public static <T> T readValue(InputStream inputStream, Class<T> type) throws IOException, BadVersionException {
        return DataModelSerializer.deserializeObject(inputStream, type);
    }

    /**
     * Reads in a list of objects from a JSON input stream
     * 
     * @param inputStream The input stream containing the JSON array of objects
     * @param type The type of the object in the array to be read from the stream
     * @return The list of objects
     * @throws IOException
     */
    public static <T> List<T> readValues(InputStream inputStream, Class<T> type) throws IOException {
        return DataModelSerializer.deserializeList(inputStream, type);
    }

}
