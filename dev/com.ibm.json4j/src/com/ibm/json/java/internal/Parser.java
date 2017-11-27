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

package com.ibm.json.java.internal;

import java.io.IOException;
import java.io.Reader;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;

/**
 * Private parser class which handles doing the parsing of the JSON string into tokens.
 */
public class Parser
{

    private Tokenizer tokenizer;
    private Token     lastToken;

    /**
     * Contructor
     * @param reader The Reader to use when reading in the JSON stream/string.
     *
     * @throws IOException Thrown if an error occurs in tokenizing the JSON string.
     */
    public Parser(Reader reader, boolean largeNumbers) throws IOException {
        super();

        this.tokenizer = new Tokenizer(reader, largeNumbers);
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
     * @return JSONObject Returns the parsed out JSON object.
     *
     * @throws IOException Thrown if an IO error occurs during parse, such as a malformed JSON object.
     */
    public JSONObject parseObject() throws IOException {
        return parseObject(false);
    }

    /**
     * Method to parse a JSON object out of the current JSON string position.
     * @param ordered Flag to denote if the parse should contruct a JSON object which maintains serialization order of the attributes.     
     * @return JSONObject Returns the parsed out JSON object.
     *
     * @throws IOException Thrown if an IO error occurs during parse, such as a malformed JSON object.
     */
    public JSONObject parseObject(boolean ordered) throws IOException {
        JSONObject result = null;

        if (!ordered)
        {
            result = new JSONObject();
        }
        else
        {
            result = new OrderedJSONObject();
        }

        if (lastToken != Token.TokenBraceL) throw new IOException("Expecting '{' " + tokenizer.onLineCol() + " instead, obtained token: '" + lastToken + "'");
        lastToken = tokenizer.next();

        while (true)
        {
            if (lastToken == Token.TokenEOF) throw new IOException("Unterminated object " + tokenizer.onLineCol());

            if (lastToken == Token.TokenBraceR)
            {
                lastToken = tokenizer.next();
                break;
            }

            if (!lastToken.isString()) throw new IOException("Expecting string key " + tokenizer.onLineCol());
            String key = lastToken.getString();

            lastToken = tokenizer.next();
            if (lastToken != Token.TokenColon) throw new IOException("Expecting colon " + tokenizer.onLineCol());

            lastToken = tokenizer.next();
            Object val = parseValue(ordered);

            result.put(key, val);

            if (lastToken == Token.TokenComma)
            {
                lastToken = tokenizer.next();
            }

            else if (lastToken != Token.TokenBraceR)
            {
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
     * @param ordered Flag to denote if the parse should contruct JSON objects which maintain serialization order of the attributes for all JSONOjects in the array.     
     * 
     * @throws IOException Thrown if a parse error occurs, such as a malformed JSON array.
     */
    public JSONArray parseArray(boolean ordered) throws IOException {
        JSONArray result = new JSONArray();

        if (lastToken != Token.TokenBrackL) throw new IOException("Expecting '[' " + tokenizer.onLineCol());
        lastToken = tokenizer.next();

        while (true)
        {
            if (lastToken == Token.TokenEOF) throw new IOException("Unterminated array " + tokenizer.onLineCol());

            /**
             * End of the array.
             */
            if (lastToken == Token.TokenBrackR)
            {
                lastToken = tokenizer.next();
                break;
            }

            Object val = parseValue(ordered);
            result.add(val);

            if (lastToken == Token.TokenComma)
            {
                lastToken = tokenizer.next();
            }
            else if (lastToken != Token.TokenBrackR)
            {
                throw new IOException("expecting either ',' or ']' " + tokenizer.onLineCol());
            }
        }

        return result;
    }

    /**
     * Method to parse the current JSON property value from the last token. 
     * @return The java object type that represents the JSON value.
     *
     * @throws IOException Thrown if an IO error (read incomplete token) occurs.
     */
    public Object parseValue() throws IOException {
        return parseValue(false);
    }

    /**
     * Method to parse the current JSON property value from the last token. 
     * @return The java object type that represents the JSON value.
     * @param ordered Flag to denote if the parse should contruct JSON objects and arrays which maintain serialization order of the attributes.     
     *
     * @throws IOException Thrown if an IO error (read incomplete token) occurs.
     */
    public Object parseValue(boolean ordered) throws IOException {
        if (lastToken == Token.TokenEOF) throw new IOException("Expecting property value " + tokenizer.onLineCol());

        if (lastToken.isNumber())
        {
            Object result = lastToken.getNumber();
            lastToken = tokenizer.next();
            return result;
        }

        if (lastToken.isString())
        {
            Object result = lastToken.getString();
            lastToken = tokenizer.next();
            return result;
        }

        if (lastToken == Token.TokenFalse)
        {
            lastToken = tokenizer.next();
            return Boolean.FALSE;
        }

        if (lastToken == Token.TokenTrue)
        {
            lastToken = tokenizer.next();
            return Boolean.TRUE;
        }

        if (lastToken == Token.TokenNull)
        {
            lastToken = tokenizer.next();
            return null;
        }

        if (lastToken == Token.TokenBrackL) return parseArray(ordered);
        if (lastToken == Token.TokenBraceL) return parseObject(ordered);

        throw new IOException("Invalid token " + tokenizer.onLineCol());
    }

}
