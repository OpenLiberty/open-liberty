/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java;

import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.json.java.internal.Serializer;
import com.ibm.json.java.internal.SerializerVerbose;

/**
 * Extension of ArrayList that only allows values which are JSON-able.  
 * See JSONObject for a list of valid values.
 * 
 * Instances of this class are not thread-safe.
 */
public class JSONArray extends ArrayList implements JSONArtifact
{

    /**
     * Serial UID for serialization checking.
     */
    private static final long serialVersionUID = 9076798781015779954L;

    /**
     * Create a new instance of this class.
     */
    public JSONArray()
    {
        super();
    }

    /**
     * Create a new instance of this class with the specified initial capacity.
     */
    public JSONArray(int initialCapacity)
    {
        super(initialCapacity);
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#add(int, java.lang.Object)
     */
    public void add(int index, Object element)
    {
        checkElement(element);
        super.add(index, element);
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    public boolean add(Object element)
    {
        checkElement(element);
        return super.add(element);
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#addAll(java.util.Collection)
     */
    public boolean addAll(Collection collection)
    {
        checkElements(collection);
        return super.addAll(collection);
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#addAll(int, java.util.Collection)
     */
    public boolean addAll(int index, Collection collection)
    {
        checkElements(collection);
        return super.addAll(index, collection);
    }

    /*
     * (non-Javadoc)
     * @see java.util.ArrayList#set(int, java.lang.Object)
     */
    public Object set(int index, Object element)
    {
        checkElement(element);
        return super.set(index, element);
    }

    /**
     * Convert a stream of JSONArray text into JSONArray form. 
     * @param is The inputStream from which to read the JSON.  It will assume the input stream is in UTF-8 and read it as such.
     * @return The contructed JSONArray Object.
     *
     * @throws IOEXception Thrown if an underlying IO error from the stream occurs, or if malformed JSON is read,
     */
    static public JSONArray parse(InputStream is) throws IOException {
        InputStreamReader isr = null;
        try
        {
            isr = new InputStreamReader(is, "UTF-8");
        }
        catch (Exception ex)
        {
            isr = new InputStreamReader(is);
        }
        return parse(isr);
    }

    /**
     * Convert a stream (in reader form) of JSONArray text into object form. 
     * @param reader The reader from which the JSONArray data is read.
     * @return The contructed JSONArray Object.
     * 
     * @throws IOEXception Thrown if an underlying IO error from the reader occurs, or if malformed JSON is read,
     */
    static public JSONArray parse(Reader reader) throws IOException {

        /** 
         * We can just build a pseudo-object and use it to contain the array text.  
         * Then we just get the value off the object and return it!  Easy.
         */
        StringBuffer buf = new StringBuffer("");
        buf.append("{\"jsonArray\":");

        char data[] = new char[8196];
        int amtRead = 0;
        amtRead = reader.read(data,0,8196);
        while (amtRead != -1)
        {
            buf.append(data,0,amtRead);
            amtRead = reader.read(data,0,8196);
        }
        buf.append("}");
        
        /**
         * Now parse it and return the array object.
         */
        JSONObject obj = JSONObject.parse(buf.toString());
        return (JSONArray)obj.get("jsonArray");
    }

    /**
     * Convert a String of JSONArray text into object form. 
     * @param str The JSONArray string to parse into a Java Object.
     * @return The contructed JSONArray Object.
     *
     * @throws IOEXception Thrown if malformed JSON is read,
     */
    static public JSONArray parse(String str) throws IOException {
        StringReader strReader = new StringReader(str);
        return parse(strReader);
    }


    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(os,false);
     * @param os The output stream to serialize data to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(OutputStream os) throws IOException {
        serialize(os,false);
    }

    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(writer,false);
     * @param os The output stream to serialize data to.
     * @param verbose Whether or not to write the JSON text in a verbose format.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(OutputStream os, boolean verbose) throws IOException {
        Writer writer = null;
        try
        {
            writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        }
        catch (UnsupportedEncodingException uex)
        {
            IOException iox = new IOException(uex.toString());
            iox.initCause(uex);
            throw iox;
        }
        serialize(writer, verbose);
    }


    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(writer,false);
     * @param writer The writer which to serialize the JSON text to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(Writer writer) throws IOException {
        serialize(writer, false);
    }

    /**
     * Convert this object into a stream of JSON text, specifying verbosity.
     * @param writer The writer which to serialize the JSON text to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(Writer writer, boolean verbose) throws IOException {
        Serializer serializer;

        //Try to avoid double-buffering or buffering in-memory
        //writers.
        Class writerClass = writer.getClass();
        if (!StringWriter.class.isAssignableFrom(writerClass) &&
            !CharArrayWriter.class.isAssignableFrom(writerClass) &&
            !BufferedWriter.class.isAssignableFrom(writerClass)) {
            writer = new BufferedWriter(writer);
        }

        if (verbose)
        {
            serializer = new SerializerVerbose(writer);
        }
        else
        {
            serializer = new Serializer(writer);
        }
        serializer.writeArray(this).flush();
    }

    /**
     * Convert this object into a String of JSON text, specifying verbosity.
     * @param verbose Whether or not to serialize in compressed for formatted Strings.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public String serialize(boolean verbose) throws IOException {
        Serializer serializer;
        StringWriter writer = new StringWriter();

        if (verbose)
        {
            serializer = new SerializerVerbose(writer);
        }
        else
        {
            serializer = new Serializer(writer);
        }
        serializer.writeArray(this).flush();

        return writer.toString();
    }

    /**
     * Convert this object into a String of JSON text.  Same as serialize(false);
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public String serialize() throws IOException {
        return serialize(false);
    }

    /**
     * 
     */
    private void checkElement(Object element)
    {
        if (!JSONObject.isValidObject(element)) throw new IllegalArgumentException("invalid type of element");
    }

    /**
     * 
     */
    private void checkElements(Collection collection)
    {
        for (Iterator iter = collection.iterator(); iter.hasNext(); )
        {
            if (!JSONObject.isValidObject(iter.next())) throw new IllegalArgumentException("invalid type of element");
        }
    }
}
