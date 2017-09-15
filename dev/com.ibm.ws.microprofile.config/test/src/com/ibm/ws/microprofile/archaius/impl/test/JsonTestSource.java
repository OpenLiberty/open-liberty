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
package com.ibm.ws.microprofile.archaius.impl.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class JsonTestSource implements ConfigSource {

    Properties p = new Properties();
    private final int ordinal = 100;
    private String name;

    public JsonTestSource(String resourceName) {

        p = new Properties();
        InputStreamReader isReader = null;

        try {
            //For now we are just going to deal with .json files that have the form
            //We will add more structure later
            //            {
            //                "PrinterName": "jdvhurer4",
            //                "PrinterService" : "printServe.acme.com:631"
            //            }
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            isReader = new InputStreamReader(stream, "UTF-8");
            JsonReader jsonReader = Json.createReader(isReader);
            JsonStructure jsonStruct = jsonReader.read();
            if (jsonStruct instanceof JsonObject) {
                JsonObject jo = (JsonObject) jsonStruct;
                Set<String> keys = jo.keySet();
                // Unfortunately props.putAll(jo) loads all the double quotes in the values...
                for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
                    String key = iterator.next();
                    String value = jo.getString(key);
                    p.put(key, value);
                }
            } else {
                throw new IllegalArgumentException("Json did not parse as JsonObject" + jsonStruct.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isReader != null) {
                try {
                    isReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     */
    public JsonTestSource() {
        this("META-INF/config.json");
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return propertiesToMap(p);
    }

    /**
     * @return
     */
    private ConcurrentMap<String, String> propertiesToMap(Properties p) {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<String, String>();
        Set<Entry<Object, Object>> entries = p.entrySet();
        for (Iterator<Entry<Object, Object>> iterator = entries.iterator(); iterator.hasNext();) {
            Entry<Object, Object> entry = iterator.next();
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return p.getProperty(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }
};
