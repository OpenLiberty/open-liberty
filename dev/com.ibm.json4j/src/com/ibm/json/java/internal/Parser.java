/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java.internal;

import java.io.IOException;
import java.io.Reader;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.json.java.internal.ParserConfig.DuplicateKeyBehavior;

/**
 * Private parser class which handles doing the parsing of the JSON string into tokens.
 */
public class Parser {

    private final Tokenizer tokenizer;
    private Token lastToken;
    private long totalBytesProcessed;

    /**
     * Contructor
     *
     * @param reader The Reader to use when reading in the JSON stream/string.
     *
     * @throws IOException Thrown if an error occurs in tokenizing the JSON string.
     */
    public Parser(Reader reader) throws IOException {
        super();

        this.tokenizer = new Tokenizer(reader);
        this.totalBytesProcessed = 0;
    }

    /**
     * Checks if adding the specified size would exceed the maximum total size limit.
     * Throws an IOException if the limit would be exceeded.
     *
     * @param additionalSize the number of bytes to add
     * @throws IOException if adding the size would exceed the limit
     */
    private void checkTotalSize(long additionalSize) throws IOException {
        long maxTotalSize = ParserConfig.getMaxTotalSize();

        // Check if adding would exceed limit (with overflow protection)
        if (additionalSize > maxTotalSize || totalBytesProcessed > maxTotalSize - additionalSize) {
            throw new IOException("Total parsed content size exceeds maximum allowed size of " +
                                  maxTotalSize + " bytes at line " + tokenizer.getLine() +
                                  ", column " + tokenizer.getCol() +
                                  ". Configure with system property: com.ibm.json4j.max.total.size");
        }

        totalBytesProcessed += additionalSize;
    }

    /**
     * Method to initiate the parse of the toplevel JSON object, which will in turn parse all child JSON objects contained within.
     * Same as calling parse(false);
     *
     * @throws IOException Thrown if an IO error occurd during parse of the JSON object(s).
     */
    public JSONObject parse() throws IOException {
        return parse(false);
    }

    /**
     * Method to initiate the parse of the toplevel JSON object, which will in turn parse all child JSON objects contained within.
     *
     * @param ordered Flag to denote if the parse should contruct a JSON object which maintains serialization order of the attributes.
     *
     * @throws IOException Thrown if an IO error occurd during parse of the JSON object(s).
     */
    public JSONObject parse(boolean ordered) throws IOException {
        lastToken = tokenizer.next();
        return parseObject(ordered);
    }

    /**
     * Method to parse a JSON object out of the current JSON string position.
     *
     * @return JSONObject Returns the parsed out JSON object.
     *
     * @throws IOException Thrown if an IO error occurs during parse, such as a malformed JSON object.
     */
    public JSONObject parseObject() throws IOException {
        return parseObject(false);
    }

    /**
     * Method to parse a JSON object out of the current JSON string position.
     *
     * @param ordered Flag to denote if the parse should contruct a JSON object which maintains serialization order of the attributes.
     * @return JSONObject Returns the parsed out JSON object.
     *
     * @throws IOException Thrown if an IO error occurs during parse, such as a malformed JSON object.
     */
    public JSONObject parseObject(boolean ordered) throws IOException {
        return parseObject(ordered, 0);
    }

    /**
     * Method to parse a JSON object out of the current JSON string position with depth tracking.
     *
     * @param ordered Flag to denote if the parse should contruct a JSON object which maintains serialization order of the attributes.
     * @param depth   Current nesting depth
     * @return JSONObject Returns the parsed out JSON object.
     *
     * @throws IOException Thrown if an IO error occurs during parse, such as a malformed JSON object.
     */
    private JSONObject parseObject(boolean ordered, int depth) throws IOException {
        JSONObject result = null;

        if (!ordered) {
            result = new JSONObject();
        } else {
            result = new OrderedJSONObject();
        }

        // Track object overhead (HashMap/LinkedHashMap base size)
        checkTotalSize(64);

        int maxMembers = ParserConfig.getMaxObjectMembers();
        int currentMembers = 0;
        DuplicateKeyBehavior dupBehavior = ParserConfig.getDuplicateKeyBehavior();

        if (lastToken != Token.TokenBraceL)
            throw new IOException("Expecting '{' " + tokenizer.onLineCol() + " instead, obtained token: '" + lastToken + "'");
        lastToken = tokenizer.next();

        while (true) {
            if (lastToken == Token.TokenEOF)
                throw new IOException("Unterminated object " + tokenizer.onLineCol());

            if (lastToken == Token.TokenBraceR) {
                lastToken = tokenizer.next();
                break;
            }

            // Check member count limit BEFORE adding
            if (currentMembers >= maxMembers) {
                throw new IOException("Object member count exceeds maximum allowed count of " +
                                      maxMembers + " members at line " + tokenizer.getLine() +
                                      ", column " + tokenizer.getCol() +
                                      ". Configure with system property: com.ibm.json4j.max.object.members");
            }

            if (!lastToken.isString())
                throw new IOException("Expecting string key " + tokenizer.onLineCol());
            String key = lastToken.getString();

            // Track key size (string overhead + chars + map entry overhead)
            checkTotalSize(key.length() * 2L + 24); // 24 bytes for string + map entry overhead

            // Check for duplicate keys
            if (result.containsKey(key)) {
                String dupMsg = "Duplicate key '" + key + "' found at line " +
                                tokenizer.getLine() + ", column " + tokenizer.getCol();

                if (dupBehavior == DuplicateKeyBehavior.ERROR) {
                    throw new IOException(dupMsg +
                                          ". Configure with system property: com.ibm.json4j.duplicate.key.behavior");
                } else if (dupBehavior == DuplicateKeyBehavior.WARN) {
                    System.err.println("WARNING: " + dupMsg + " (overwriting previous value)");
                }
                // SILENT: do nothing, just overwrite (existing behavior)
            }

            lastToken = tokenizer.next();
            if (lastToken != Token.TokenColon)
                throw new IOException("Expecting colon " + tokenizer.onLineCol());

            lastToken = tokenizer.next();
            Object val = parseValue(ordered, depth);

            result.put(key, val);
            currentMembers++;

            if (lastToken == Token.TokenComma) {
                lastToken = tokenizer.next();
            }

            else if (lastToken != Token.TokenBraceR) {
                throw new IOException("expecting either ',' or '}' " + tokenizer.onLineCol());
            }
        }

        return result;
    }

    /**
     * Method to parse out a JSON array from a JSON string
     * Same as calling parseArray(false)
     *
     * @throws IOException Thrown if a parse error occurs, such as a malformed JSON array.
     */
    public JSONArray parseArray() throws IOException {
        return parseArray(false);
    }

    /**
     * Method to parse out a JSON array from a JSON string
     *
     * @param ordered Flag to denote if the parse should contruct JSON objects which maintain serialization order of the attributes for all JSONOjects in the array.
     *
     * @throws IOException Thrown if a parse error occurs, such as a malformed JSON array.
     */
    public JSONArray parseArray(boolean ordered) throws IOException {
        return parseArray(ordered, 0);
    }

    /**
     * Method to parse out a JSON array from a JSON string with depth tracking.
     *
     * @param ordered Flag to denote if the parse should contruct JSON objects which maintain serialization order of the attributes for all JSONOjects in the array.
     * @param depth   Current nesting depth
     *
     * @throws IOException Thrown if a parse error occurs, such as a malformed JSON array.
     */
    private JSONArray parseArray(boolean ordered, int depth) throws IOException {
        JSONArray result = new JSONArray();

        // Track array overhead (ArrayList base size)
        checkTotalSize(40);

        int maxSize = ParserConfig.getMaxArraySize();
        int currentSize = 0;

        if (lastToken != Token.TokenBrackL)
            throw new IOException("Expecting '[' " + tokenizer.onLineCol());
        lastToken = tokenizer.next();

        while (true) {
            if (lastToken == Token.TokenEOF)
                throw new IOException("Unterminated array " + tokenizer.onLineCol());

            /**
             * End of the array.
             */
            if (lastToken == Token.TokenBrackR) {
                lastToken = tokenizer.next();
                break;
            }

            // Check size limit BEFORE adding element
            if (currentSize >= maxSize) {
                throw new IOException("Array size exceeds maximum allowed size of " +
                                      maxSize + " elements at line " + tokenizer.getLine() +
                                      ", column " + tokenizer.getCol() +
                                      ". Configure with system property: com.ibm.json4j.max.array.size");
            }

            Object val = parseValue(ordered, depth);
            result.add(val);
            currentSize++;

            // Track array element reference overhead (8 bytes per reference)
            checkTotalSize(8);

            if (lastToken == Token.TokenComma) {
                lastToken = tokenizer.next();
            } else if (lastToken != Token.TokenBrackR) {
                throw new IOException("expecting either ',' or ']' " + tokenizer.onLineCol());
            }
        }

        return result;
    }

    /**
     * Method to parse the current JSON property value from the last token.
     *
     * @return The java object type that represents the JSON value.
     *
     * @throws IOException Thrown if an IO error (read incomplete token) occurs.
     */
    public Object parseValue() throws IOException {
        return parseValue(false);
    }

    /**
     * Method to parse the current JSON property value from the last token.
     *
     * @return The java object type that represents the JSON value.
     * @param ordered Flag to denote if the parse should contruct JSON objects and arrays which maintain serialization order of the attributes.
     *
     * @throws IOException Thrown if an IO error (read incomplete token) occurs.
     */
    public Object parseValue(boolean ordered) throws IOException {
        return parseValue(ordered, 0);
    }

    /**
     * Method to parse the current JSON property value from the last token with depth tracking.
     *
     * @return The java object type that represents the JSON value.
     * @param ordered Flag to denote if the parse should contruct JSON objects and arrays which maintain serialization order of the attributes.
     * @param depth   Current nesting depth
     *
     * @throws IOException Thrown if an IO error (read incomplete token) occurs.
     */
    private Object parseValue(boolean ordered, int depth) throws IOException {
        int maxDepth = ParserConfig.getMaxNestingDepth();

        // Check depth limit BEFORE recursing
        if (depth >= maxDepth) {
            throw new IOException("JSON nesting depth exceeds maximum allowed depth of " +
                                  maxDepth + " levels at line " + tokenizer.getLine() +
                                  ", column " + tokenizer.getCol() +
                                  ". Configure with system property: com.ibm.json4j.max.nesting.depth");
        }

        if (lastToken == Token.TokenEOF)
            throw new IOException("Expecting property value " + tokenizer.onLineCol());

        if (lastToken.isNumber()) {
            Object result = lastToken.getNumber();
            // Track number size - estimate based on string representation
            // Numbers are stored as Long or Double, so we estimate memory impact
            if (result instanceof Long) {
                checkTotalSize(8); // 8 bytes for Long
            } else if (result instanceof Double) {
                checkTotalSize(8); // 8 bytes for Double
            }
            lastToken = tokenizer.next();
            return result;
        }

        if (lastToken.isString()) {
            String result = lastToken.getString();
            // Track string size - 2 bytes per char in Java (UTF-16) plus object overhead
            checkTotalSize(result.length() * 2L + 24); // 24 bytes for String object overhead
            lastToken = tokenizer.next();
            return result;
        }

        if (lastToken == Token.TokenFalse) {
            lastToken = tokenizer.next();
            return Boolean.FALSE;
        }

        if (lastToken == Token.TokenTrue) {
            lastToken = tokenizer.next();
            return Boolean.TRUE;
        }

        if (lastToken == Token.TokenNull) {
            lastToken = tokenizer.next();
            return null;
        }

        if (lastToken == Token.TokenBrackL)
            return parseArray(ordered, depth + 1);
        if (lastToken == Token.TokenBraceL)
            return parseObject(ordered, depth + 1);

        throw new IOException("Invalid token " + tokenizer.onLineCol());
    }

}
