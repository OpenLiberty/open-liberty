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
package com.ibm.websphere.jsonsupport;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 *
 */
public interface JSON {

    /**
     * Converts a POJO to a JSON representation. The POJO is expected to follow
     * normal MBean naming patterns, so the following class:
     * <tt>
     * <pre>
     * class SimplePOJO {
     * String getName() { return "myName"; }
     * boolean getEnabled() { return true; }
     * }
     * </pre>
     * </tt>
     * 
     * Will be marshalled to the String <tt>{"name":"myName","enabled":true}</tt>
     * 
     * @param o The Object to marshall as JSON
     * @return Returns the marshalled JSON String
     * @throws JSONMarshallException Thrown when the POJO can not be marshalled
     */
    public String stringify(final Object o) throws JSONMarshallException;

    /**
     * Marshalls the Object as UTF-8 encoded bytes.
     * 
     * @see #stringify(Object)
     * @param o The Object to marshall as JSON
     * @return Returns the marshalled JSON String
     * @throws JSONMarshallException Thrown when the POJO can not be marshalled
     */
    public byte[] asBytes(final Object o) throws JSONMarshallException;

    /**
     * Unmarshalls the JSON input as the target Class type. Any unrecognized fields will be ignored.
     * 
     * @param in The String to unmarshall
     * @param type The type to create from the input JSON
     * @return The created POJO
     * @throws JSONMarshallException Thrown when the POJO can not be unmarshalled
     */
    public <T> T parse(final String in, final Class<? extends T> type) throws JSONMarshallException;

    /**
     * Unmarshalls the JSON input as the target Class type. Any unrecognized fields will be ignored.
     * 
     * @param in The UTF-8 byte array to unmarshall
     * @param type The type to create from the input JSON
     * @return The created POJO
     * @throws JSONMarshallException Thrown when the POJO can not be unmarshalled
     */
    public <T> T parse(final byte[] in, final Class<? extends T> type) throws JSONMarshallException;

    /**
     * Unmarshalls the JSON input as the target Class type. Any unrecognized fields will be ignored.
     * 
     * @param in The File with contents to unmarshall
     * @param type The type to create from the input JSON
     * @return The created POJO
     * @throws JSONMarshallException Thrown when the POJO can not be unmarshalled
     */
    public <T> T parse(final File in, final Class<? extends T> type) throws IOException, JSONMarshallException;

    /**
     * Serializes Java object as JSON output and writes to file specified
     * 
     * @param out File to which to write JSON
     * @param pojo Java object to serialize
     * @throws JSONMarshallException
     */
    void serializeToFile(File out, Object pojo) throws JSONMarshallException;

    /**
     * Serializes Java object as JSON output and writes to the Writer object specified
     * 
     * @param out Writer to which to write JSON
     * @param pojo Java object to serialize
     * @throws JSONMarshallException
     */
    void serializeToWriter(Writer out, Object pojo) throws JSONMarshallException;
}
