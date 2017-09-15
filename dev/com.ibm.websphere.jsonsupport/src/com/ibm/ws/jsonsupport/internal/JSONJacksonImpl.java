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
package com.ibm.ws.jsonsupport.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.websphere.jsonsupport.JSONMarshallException;
import com.ibm.websphere.jsonsupport.JSONSettings;
import com.ibm.websphere.jsonsupport.JSONSettings.Include;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class JSONJacksonImpl implements JSON {

    private final ObjectMapper mapper;

    public JSONJacksonImpl() {
        this(null);
    }

    public JSONJacksonImpl(JSONSettings settings) {
        mapper = new ObjectMapper();
        if (settings == null)
            return;
        //set inclusion
        Include inclusion = settings.getInclusion();
        if (inclusion == Include.ALWAYS) {
            mapper.getSerializationConfig().setSerializationInclusion(Inclusion.ALWAYS);
        } else if (inclusion == Include.NON_NULL) {
            mapper.getSerializationConfig().setSerializationInclusion(Inclusion.NON_NULL);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String stringify(Object o) throws JSONMarshallException {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonGenerationException e) {
            throw new JSONMarshallException("Error during JSON writing", e);
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] asBytes(Object o) throws JSONMarshallException {
        try {
            return stringify(o).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new JSONMarshallException("Encountered a JVM without UTF-8 support, this should never happen", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T parse(String in, Class<? extends T> type) throws JSONMarshallException {
        try {
            return mapper.readValue(in, type);
        } catch (JsonParseException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Fatal problems occurred while mapping content", e);
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T parse(byte[] in, Class<? extends T> type) throws JSONMarshallException {
        try {
            return mapper.readValue(in, 0, in.length, type);
        } catch (JsonParseException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Fatal problems occurred while mapping content", e);
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }

    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(FileNotFoundException.class)
    public <T> T parse(File in, Class<? extends T> type) throws IOException, JSONMarshallException {
        try {
            return mapper.readValue(in, type);
        } catch (JsonParseException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Fatal problems occurred while mapping content", e);
        } catch (FileNotFoundException e) {
            // This is an expected error flow, do not FFDC or log anything
            throw e;
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public void serializeToFile(File out, Object pojo) throws JSONMarshallException {
        try {
            mapper.writeValue(out, pojo);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonGenerationException e) {
            throw new JSONMarshallException("Error during JSON writing", e);
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void serializeToWriter(Writer out, Object pojo) throws JSONMarshallException {
        try {
            mapper.writeValue(out, pojo);
        } catch (JsonMappingException e) {
            throw new JSONMarshallException("Unable to parse non-well-formed content", e);
        } catch (JsonGenerationException e) {
            throw new JSONMarshallException("Error during JSON writing", e);
        } catch (IOException e) {
            throw new JSONMarshallException("I/O exception of some sort has occurred", e);
        }
    }
}
