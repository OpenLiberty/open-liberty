/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
import java.io.InputStream;
import java.io.Reader;
import java.io.PushbackReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.CharArrayReader;

/**
 * Helper class that does generic parsing of a JSON stream and returns the appropriate 
 * JSON structure (JSONArray or JSONObject).  Note that it is slightly more efficient to directly 
 * parse with the appropriate object than to use this class to do a generalized parse.  
 */
public class JSON {
    /**
     * Parse a Reader of JSON text into a JSONArtifact. 
     * @param reader The character reader to read the JSON data from.
     * @param order Boolean flag indicating if the order of the JSON data should be preserved.  This parameter only has an effect if the stream is JSON Object { ... } formatted data.
     * Note that the provided reader is not closed on completion of read; that is left to the caller.
     *
     * @return Returns an instance of JSONArtifact (JSONObject, OrderedJSONObject, or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if reader is null
     */
    public static JSONArtifact parse(Reader reader, boolean order) throws IOException, NullPointerException {
        if (reader != null) {
            PushbackReader pReader = null;

            //Determine if we should buffer-wrap the reader before passing it on
            //to the appropriate parser.
            boolean bufferIt = false;

            Class readerClass = reader.getClass();

            if (!StringReader.class.isAssignableFrom(readerClass) && 
                !CharArrayReader.class.isAssignableFrom(readerClass) &&
                !PushbackReader.class.isAssignableFrom(readerClass) &&
                !BufferedReader.class.isAssignableFrom(readerClass)) {
                bufferIt = true;
            }

            if (PushbackReader.class.isAssignableFrom(readerClass)) {
                pReader = (PushbackReader) reader;
            } else {
                pReader = new PushbackReader(reader);
            }

            Reader rdr = pReader;
            int ch = pReader.read();
            while (ch != -1) {
                switch (ch) {
                    case '{':
                        pReader.unread(ch);
                        if (bufferIt) {
                            rdr = new BufferedReader(pReader);
                        }
                        if (order) {
                            return OrderedJSONObject.parse(rdr);
                        } else {
                            return JSONObject.parse(rdr);
                        }
                    case '[':
                        pReader.unread(ch);
                        if (bufferIt) {
                            rdr = new BufferedReader(pReader);
                        }
                        return JSONArray.parse(rdr);
                    case ' ':
                    case '\t':
                    case '\f':
                    case '\r':
                    case '\n':
                    case '\b':
                        ch = pReader.read();
                        break;
                    default:
                        throw new IOException("Unexpected character: [" + (char)ch + "] while scanning JSON String for JSON type.  Invalid JSON."); 
                }
            }
            throw new IOException("Encountered end of stream before JSON data was read.  Invalid JSON");
        } else {
            throw new NullPointerException("reader cannot be null.");
        }
    }

    /**
     * Parse a Reader of JSON text into a JSONArtifact.  
     * This call is the same as JSON.parse(reader, false).
     * Note that the provided reader is not closed on completion of read; that is left to the caller.
     * @param reader The character reader to read the JSON data from.
     *
     * @return Returns an instance of JSONArtifact (JSONObject, OrderedJSONObject, or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if reader is null
     */
    public static JSONArtifact parse(Reader reader) throws IOException, NullPointerException {
        return parse(reader,false);
    }

    /**
     * Parse a InputStream of JSON text into a JSONArtifact. 
     * Note that the provided InputStream is not closed on completion of read; that is left to the caller.
     * @param is The input stream to read from.  The content is assumed to be UTF-8 encoded and handled as such.
     * @param order Boolean flag indicating if the order of the JSON data should be preserved.  This parameter only has an effect if the stream is JSON Object { ... } formatted data.
     *
     * @return Returns an instance of JSONArtifact (JSONObject or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if reader is null
     */
    public static JSONArtifact parse(InputStream is, boolean order) throws IOException, NullPointerException {
        if (is != null) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            } catch (Exception ex) {
                IOException iox = new IOException("Could not construct UTF-8 character reader for the InputStream");
                iox.initCause(ex);
                throw iox;
            }
            return parse(reader,order);
        } else {
            throw new NullPointerException("is cannot be null");
        }

    }

    /**
     * Parse an InputStream of JSON text into a JSONArtifact.
     * This call is the same as JSON.parse(is, false).
     * Note that the provided InputStream is not closed on completion of read; that is left to the caller.
     * @param is The input stream to read from.  The content is assumed to be UTF-8 encoded and handled as such.
     *
     * @return Returns an instance of JSONArtifact (JSONObject, OrderedJSONObject, or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if reader is null
     */
    public static JSONArtifact parse(InputStream is) throws IOException, NullPointerException {
        return parse(is,false);
    }

    /**
     * Parse a string of JSON text into a JSONArtifact. 
     * @param str The String to read from.  
     * @param order Boolean flag indicating if the order of the JSON data should be preserved.  This parameter only has an effect if the stream is JSON Object { ... } formatted data.
     *
     * @return Returns an instance of JSONArtifact (JSONObject or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if str is null
     */
    public static JSONArtifact parse(String str, boolean order) throws IOException, NullPointerException {
        if (str != null) {
            return parse(new StringReader(str), order);
        } else {
            throw new NullPointerException("str cannot be null");
        }
    }

    /**
     * Parse a string of JSON text into a JSONArtifact. 
     * This call is the same as JSON.parse(str, false).
     * @param str The String to read from.
     *
     * @return Returns an instance of JSONArtifact (JSONObject, OrderedJSONObject, or JSONArray), corrisponding to if the input stream was Object or Array notation.
     *
     * @throws IOException Thrown on IO errors during parse.
     * @throws NullPointerException Thrown if str is null
     */
    public static JSONArtifact parse(String str) throws IOException, NullPointerException {
        return parse(str, false);
    }
}
