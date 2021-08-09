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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.CharArrayWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.ibm.json.java.internal.Parser;
import com.ibm.json.java.internal.Serializer;
import com.ibm.json.java.internal.SerializerVerbose;

/**
 * Models a JSON Object.
 * 
 * Extension of HashMap that only allows String keys, and values which are JSON-able. 
 * <BR><BR>
 * JSON-able values are: null, and instances of String, Boolean, Number, JSONObject and JSONArray.
 * <BR><BR>
 * Instances of this class are not thread-safe.
 */
public class JSONObject extends HashMap  implements JSONArtifact
{

    private static final long serialVersionUID = -3269263069889337298L;

    /**
     * Return whether the object is a valid value for a property.
     * @param object The object to check for validity as a JSON property value.
     */
    public static boolean isValidObject(Object object)
    {
        if (null == object) return true;
        return isValidType(object.getClass());
    }

    /**
     * Return whether the class is a valid type of value for a property.
     * @param clazz The class type to check for validity as a JSON object type.
     */
    public static boolean isValidType(Class clazz)
    {
        if (null == clazz) throw new IllegalArgumentException();

        if (String.class  == clazz) return true;
        if (Boolean.class == clazz) return true;
        if (JSONObject.class.isAssignableFrom(clazz)) return true;
        if (JSONArray.class == clazz) return true;
        if (Number.class.isAssignableFrom(clazz)) return true;

        return false;
    }

    /**
     * Convert a stream (in reader form) of JSON text into object form. 
     * @param reader The reader from which the JSON data is read.
     * @return The contructed JSON Object.
     * 
     * @throws IOEXception Thrown if an underlying IO error from the reader occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(Reader reader) throws IOException {
        return new Parser(reader).parse();
    }

    /**
     * Convert a String of JSON text into object form. 
     * @param str The JSON string to parse into a Java Object.
     * @return The contructed JSON Object.
     *
     * @throws IOEXception Thrown if malformed JSON is read,
     */
    static public JSONObject parse(String str) throws IOException {
        StringReader strReader = new StringReader(str);
        return parse(strReader);
    }

    /**
     * Convert a stream of JSON text into object form. 
     * @param is The inputStream from which to read the JSON.  It will assume the input stream is in UTF-8 and read it as such.
     * @return The contructed JSON Object.
     *
     * @throws IOEXception Thrown if an underlying IO error from the stream occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(InputStream is) throws IOException {
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
     * Create a new instance of this class. 
     */
    public JSONObject()
    {
        super();
    }

    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(os,false);
     * Note that encoding is always written as UTF-8, as per JSON spec.
     * @param os The output stream to serialize data to.
     *
     * @throws IOException Thrown on IO errors during serialization.
     */
    public void serialize(OutputStream os) throws IOException {
        serialize(os,false);
    }

    /**
     * Convert this object into a stream of JSON text.  Same as calling serialize(writer,false);
     * Note that encoding is always written as UTF-8, as per JSON spec.
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
        serializer.writeObject(this).flush();
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
        serializer.writeObject(this).flush();

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
     * (non-Javadoc)
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object key, Object value)
    {
        if (null == key) throw new IllegalArgumentException("key must not be null");
        if (!(key instanceof String)) throw new IllegalArgumentException("key must be a String");

        if (!isValidObject(value))
        {
            if (value != null)
            {
                throw new IllegalArgumentException("Invalid type of value.  Type: [" + value.getClass().getName() + "] with value: [" + value.toString() + "]");
            }
            else
            {
                throw new IllegalArgumentException("Invalid type of value.");
            }
        }
        return super.put(key, value);
    }

    /**
     * Over-ridden toString() method.  Returns the same value as serialize(), which is a compact JSON String.
     * If an error occurs in the serialization, the return will be of format: JSON Generation Error: [<some error>]
     */
    public String toString(){
        String str = null;
        try
        {
            str = serialize(false);    
        }
        catch (IOException iox){
            str = "JSON Generation Error: [" + iox.toString() + "]";
        }
        return str;
    }
}
