/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//package com.ibm.ws.microprofile.appConfig.customSources.test;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.Properties;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//
//import org.eclipse.microprofile.config.spi.ConfigSource;
//
//public class XMLPropertiesConfigSource extends HashMapConfigSource implements ConfigSource {
//
//    public XMLPropertiesConfigSource(ConcurrentMap<String, String> properties, int ordinal, String id) {
//        super(properties, ordinal, "XML Properties File Config Source: " + id);
//    }
//
//    public XMLPropertiesConfigSource(URL resource, int ordinal, String id) {
//        this(loadProperties(resource), ordinal, id);
//    }
//
//    public static Properties loadProperties(InputStream inputStream) throws IOException {
//        Properties properties = new Properties();
//        properties.loadFromXML(inputStream);
//        return properties;
//    }
//
//    public static ConcurrentMap<String, String> loadProperties(URL resource) {
//        ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
//
//        InputStream stream = null;
//
//        try {
//            stream = resource.openStream();
//            Properties properties = new Properties();
//            properties.loadFromXML(stream);
//            Set<String> propNames = properties.stringPropertyNames();
//            for (String name : propNames) {
//                props.put(name, properties.getProperty(name));
//            }
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            if (stream != null) {
//                try {
//                    stream.close();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//
//        return props;
//    }
//}
