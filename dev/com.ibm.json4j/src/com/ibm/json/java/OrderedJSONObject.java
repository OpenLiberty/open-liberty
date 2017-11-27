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

import java.io.BufferedReader;
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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.json.java.internal.Parser;
import com.ibm.json.java.internal.Serializer;
import com.ibm.json.java.internal.SerializerVerbose;

/**
 * Extension of the basic JSONObject.  This class allows control of the serialization order of attributes.  
 * The order in which items are put into the instance controls the order in which they are serialized out.  For example, the
 * last item put is the last item serialized.  
 * <BR><BR>
 * JSON-able values are: null, and instances of String, Boolean, Number, JSONObject and JSONArray.
 * <BR><BR>
 * Instances of this class are not thread-safe.
 */
public class OrderedJSONObject extends JSONObject
{

    private static final long serialVersionUID = -3269263069889337299L;
    private ArrayList order                    = null;

    /**
     * Create a new instance of this class. 
     */
    public OrderedJSONObject()
    {
        super();
        this.order = new ArrayList();
    }

    /**
     * Convert a stream (in reader form) of JSON text into object form.
     * @param reader The reader from which the JSON data is read.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     *
     * @throws IOEXception Thrown if an underlying IO error from the reader occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(Reader reader) throws IOException {
        return parse(reader, false);
    }

    /**
     * Convert a stream (in reader form) of JSON text into object form. 
     * @param reader The reader from which the JSON data is read.
     * @param largeNumbers Set to true to support arbitrarily large numbers.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     * 
     * @throws IOEXception Thrown if an underlying IO error from the reader occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(Reader reader, boolean largeNumbers) throws IOException {
        reader = new BufferedReader(reader);
        return new Parser(reader, largeNumbers).parse(true);
    }

    /**
     * Convert a String of JSON text into object form.
     * @param str The JSON string to parse into a Java Object.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     *
     * @throws IOEXception Thrown if malformed JSON is read,
     */
    static public JSONObject parse(String str) throws IOException {
        return parse(str, false);
    }

    /**
     * Convert a String of JSON text into object form. 
     * @param str The JSON string to parse into a Java Object.
     * @param largeNumbers Set to true to support arbitrarily large numbers.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     *
     * @throws IOEXception Thrown if malformed JSON is read,
     */
    static public JSONObject parse(String str, boolean largeNumbers) throws IOException {
        StringReader strReader = new StringReader(str);
        return parse(strReader, largeNumbers);
    }

    /**
     * Convert a stream of JSON text into object form.
     * @param is The InputStream from which to read the JSON.  It will assume the input stream is in UTF-8 and read it as such.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     *
     * @throws IOEXception Thrown if an underlying IO error from the stream occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(InputStream is) throws IOException {
        return parse(is, false);
    }

    /**
     * Convert a stream of JSON text into object form. 
     * @param is The InputStream from which to read the JSON.  It will assume the input stream is in UTF-8 and read it as such.
     * @return The contructed JSON Object.  Note that the JSONObject will be an instance of OrderedJSONObject and as such, attribute order is maintained.
     *
     * @throws IOEXception Thrown if an underlying IO error from the stream occurs, or if malformed JSON is read,
     */
    static public JSONObject parse(InputStream is, boolean largeNumbers) throws IOException {
        InputStreamReader isr = null;
        try
        {
            isr = new InputStreamReader(is, "UTF-8");
        }
        catch (Exception ex)
        {
            isr = new InputStreamReader(is);
        }
        return parse(isr, largeNumbers);
    }

    /**
     * Method to put a JSON'able object into the instance.  Note that the order of initial puts controls the order of serialization.  
     * Meaning that the first time an item is put into the object determines is position of serialization.  Subsequent puts with the same
     * key replace the existing entry value and leave serialization position alone.  For moving the position, the object must be removed, 
     * then re-put.
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

        /**
         * Only put it in the ordering list if it isn't already present.
         */
        if (!this.containsKey(key))
        {
            this.order.add(key);
        }
        return super.put(key, value);
    }
    
    /**
     * Method to remove an entry from the OrderedJSONObject instance.
     * @see java.util.HashMap#remove(java.lang.Object)
     */
    public Object remove(Object key)
    {
        Object retVal = null;

        if (null == key) throw new IllegalArgumentException("key must not be null");
        if (this.containsKey(key))
        {
            retVal = super.remove(key);

            for (int i = 0; i < this.order.size(); i++)
            {
                Object obj = this.order.get(i);
                if (obj.equals(key))
                {
                    this.order.remove(i);
                    break;
                }
            }
        }
        return retVal;
    }

    /**
     * (non-Javadoc)
     * @see java.util.HashMap#clear()
     */
    public void clear()
    {
        super.clear();
        this.order.clear();
    }

    /** 
     * Returns a shallow copy of this HashMap instance: the keys and values themselves are not cloned.
     */
    public Object clone()
    {
        OrderedJSONObject clone = (OrderedJSONObject)super.clone();
        Iterator order = clone.getOrder();
        ArrayList orderList = new ArrayList();
        while (order.hasNext())
        {
            orderList.add(order.next());
            clone.order = orderList;
        }
        return clone;
    }

    /**
     * Method to obtain the order in which the items will be serialized.
     * @return An iterator that represents the attribute names in the order that they will be serialized.
     */
    public Iterator getOrder()
    {
        return this.order.iterator();
    }
}
