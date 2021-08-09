/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.eclipse.equinox.metatype.impl.ExtendableHelper;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.ws.config.xml.internal.DepthAwareXMLStreamReader;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.metatype.MetaTypeHelper;
import com.ibm.ws.kernel.service.util.DesignatedXMLInputFactory;

/**
 * Custom metatype parser using StAX
 */
public class SchemaMetaTypeParser {

    public static final char LOCALE_SEPARATOR = '_';

    private static final QName ID = new QName("id");
    private static final QName DESCRIPTION = new QName("description");
    private static final QName NAME = new QName("name");
    private static final QName TYPE = new QName("type");
    private static final QName REQUIRED = new QName("required");
    private static final QName CARDINALITY = new QName("cardinality");
    private static final QName DEFAULT = new QName("default");
    private static final QName LABEL = new QName("label");
    private static final QName VALUE = new QName("value");
    private static final QName OCDREF = new QName("ocdref");
    private static final QName PID = new QName("pid");
    private static final QName FACTORY_PID = new QName("factoryPid");
    private static final QName MIN = new QName("min");
    private static final QName MAX = new QName("max");
    private static final QName LOCALIZATION = new QName("localization");

    public static final String METATYPE_URI = "http://www.osgi.org/xmlns/metatype/v";
    public static final String METADATA = "MetaData";
    public static final String OCD = "OCD";
    public static final String AD = "AD";
    public static final String OPTION = "Option";
    public static final String DESIGNATE = "Designate";
    public static final String OBJECT = "Object";
    public static final String VAR_INDICATOR = "%";
    public static final String METATYPE_PAT = "OSGI-INF/metatype";
    public static final String METATYPE_PROP = "OSGI-INF/l10n";
    public static final String XML_EXT = ".xml";
    public static final String PROP_EXT = ".properties";
    private static final ResourceBundle _msgs = ResourceBundle.getBundle(XMLConfigConstants.NLS_PROPS);

    private final XMLInputFactory inputFactory;
    private final List<MetaTypeInformationSpecification> metatypeList;
    MetaTypeInformationSpecification metatype = null;
    Properties messages = null;
    Bundle bundle = null;
    private final Locale locale;
    private final Map<String, List<File>> prodJars;

    public SchemaMetaTypeParser(Locale locale, List<File> j, String productName) {
        inputFactory = DesignatedXMLInputFactory.newInstance();
        metatypeList = new ArrayList<MetaTypeInformationSpecification>();
        this.locale = locale;
        this.prodJars = new HashMap<String, List<File>>();
        this.prodJars.put(productName, j);
    }

    /**
     * Constructor.
     * 
     * @param generatorOptions The user options wrapper.
     * @param prodJars The Map of bundle jars organized by product (core, usr, prodExt1, prodExt2 ...)
     */
    public SchemaMetaTypeParser(Locale locale, Map<String, List<File>> prodJars) {
        inputFactory = DesignatedXMLInputFactory.newInstance();
        metatypeList = new ArrayList<MetaTypeInformationSpecification>();
        this.locale = locale;
        this.prodJars = prodJars;
    }

    /**
     * Since we don't have have the luxury of OSGi framework getting the OCDs for us,
     * we need to look up all the metatype.xml files from each jar manually and
     * construct a metatypeinformation which will be passed to SchemaWriter
     * 
     * @param jars
     * @return StreamSource containing all the metatype XMLs
     */
    public List<MetaTypeInformationSpecification> getMetatypeInformation() {

        try {
            for (Map.Entry<String, List<File>> prodEntry : prodJars.entrySet()) {
                List<File> jars = prodEntry.getValue();
                String productName = prodEntry.getKey();
                for (File jar : jars) {
                    JarFile jarFile = new JarFile(jar);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    List<String> metatypePath = new ArrayList<String>();
                    HashMap<String, URL> metatypePropMap = new HashMap<String, URL>();

                    //can we optimize for jars with no metatype?
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        if (entry.isDirectory())
                            continue;

                        if (entryName.endsWith(XML_EXT) && entryName.startsWith(METATYPE_PAT)) {
                            metatypePath.add(entryName);
                        }
                        if (entryName.endsWith(PROP_EXT)) {
                            metatypePropMap.put(entryName, new URL("jar:" + jar.toURI() + "!/" + entryName));
                        }
                    }

                    boolean generateNewMetatype = true;
                    //combine all the metatypes while filling in the messages
                    for (String metatype : metatypePath) {
                        JarEntry metatypeEntry = jarFile.getJarEntry(metatype);
                        InputStream metatypeInputStream = jarFile.getInputStream(metatypeEntry);
                        //replace properties with jarFile
                        parse(metatypeInputStream, jarFile, generateNewMetatype, metatypePropMap, productName);
                        generateNewMetatype = false;
                    }
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } catch (XMLStreamException xse) {
            throw new RuntimeException(xse);
        }

        return metatypeList;
    }

    private void parse(InputStream metatypeXML, JarFile jarFile, boolean generateNewMetatype, Map<String, URL> metatypePropMap, String productName) throws XMLStreamException, IOException {

        XMLStreamReader xmlStreamReader = inputFactory.createXMLStreamReader(metatypeXML);
        DepthAwareXMLStreamReader parser = new DepthAwareXMLStreamReader(xmlStreamReader);
        if (generateNewMetatype) {
            bundle = new SchemaBundle(jarFile, metatypePropMap, productName);
            metatype = new MetaTypeInformationSpecification(bundle);
            metatypeList.add(metatype);
        }
        int depth = parser.getDepth();
        while (parser.hasNext(depth)) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = xmlStreamReader.getName().getLocalPart();
                if (name.equalsIgnoreCase(METADATA)) {
                    parseMetaData(parser);
                }
            }
        }
    }

    private void parseMetaData(DepthAwareXMLStreamReader parser) throws XMLStreamException {

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            QName qname = parser.getAttributeName(i);
            if (qname.equals(LOCALIZATION)) {
                try {
                    messages = new Properties();
                    String attrVal = parser.getAttributeValue(i);
                    URL messagePath = generateMetatypePropertiesName(attrVal, locale.toString());
                    if (messagePath == null) {
                        messagePath = generateMetatypePropertiesName(attrVal, null);
                    }

                    InputStream is = messagePath != null ? messagePath.openStream() : null;
                    if (is != null) {
                        messages.load(is);
                    } else {
                        warning("missing.metatype.file", bundle.getSymbolicName() + '/' + bundle.getVersion());
                    }
                } catch (IOException e) {
                    //do nothing, could be that metatype has no messages
                }
            }
        }
        int depth = parser.getDepth();
        while (parser.hasNext(depth)) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = parser.getName().getLocalPart();
                if (name.equalsIgnoreCase(OCD)) {
                    metatype.addObjectClassSpecification(parseOCD(parser));
                } else if (name.equalsIgnoreCase(DESIGNATE)) {
                    metatype.addDesignateSpecification(parseDesignate(parser));
                }
            }
        }

        metatype.validate();
    }

    private void warning(String message, Object... args) {
        // Don't generate an error when this is being run by the mbean. Not all
        // bundles may be included by a running server. 
        String msg = message;
        try {
            msg = _msgs.getString(message);
        } catch (MissingResourceException mre) {
            // Ignore this, we just output the message key if we get here.
        }
        if (args.length > 0) {
            msg = MessageFormat.format(msg, args);
        }
        // Tr isn't initialized if isRuntime is false.
        System.out.println(msg);
    }

    /**
     * Tries to generate the correct metatype properties location according to locale
     * 
     * @param metatypeName
     * @return a String with the correct metatype properties location
     */
    private URL generateMetatypePropertiesName(String metatypeName, String locale) {
        String lookupName;
        if (locale != null && locale.length() > 0) {
            lookupName = metatypeName + LOCALE_SEPARATOR + locale + PROP_EXT;
        } else {
            lookupName = metatypeName + PROP_EXT;
        }

        URL url = bundle.getEntry(lookupName);

        if (url == null) {
            int pos = locale == null ? -1 : locale.lastIndexOf(LOCALE_SEPARATOR);
            if (pos != -1) {
                url = generateMetatypePropertiesName(metatypeName, locale.substring(0, pos));
            }
        }
        return url;
    }

    private String getLocalizedName(String nameKey) {
        if (nameKey != null && nameKey.startsWith(VAR_INDICATOR)) {
            String key = nameKey.substring(1);
            String localized = messages.getProperty(key);
            return (localized != null) ? localized : nameKey;
        } else {
            return nameKey;
        }
    }

    /**
     * @param xmlStreamReader
     * @return
     */
    private ObjectClassDefinitionSpecification parseOCD(DepthAwareXMLStreamReader parser) throws XMLStreamException {
        ObjectClassDefinitionSpecification ocd = new ObjectClassDefinitionSpecification();

        //extension attributes <URI, <attr, val>>
        Map<String, Map<String, String>> extAttributes = new HashMap<String, Map<String, String>>();

        //get OCD attributes
        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            QName attrName = parser.getAttributeName(i);
            String attrVal = parser.getAttributeValue(i);

            if (attrName.equals(ID)) {
                ocd.setID(attrVal);
            } else if (attrName.equals(NAME)) {
                ocd.setName(getLocalizedName(attrVal));
            } else if (attrName.equals(DESCRIPTION)) {
                ocd.setDescription(getLocalizedName(attrVal));
            }

            String namespace = attrName.getNamespaceURI();
            //support extension attributes
            if (namespace.length() != 0 && !!!namespace.startsWith(METATYPE_URI)) {
                if (extAttributes.containsKey(namespace)) {
                    Map<String, String> attrMap = extAttributes.get(namespace);
                    attrMap.put(attrName.getLocalPart(), attrVal);
                } else {
                    HashMap<String, String> attrMap = new HashMap<String, String>();
                    attrMap.put(attrName.getLocalPart(), attrVal);
                    extAttributes.put(namespace, attrMap);
                }
            }
        }

        ocd.setExtensionAttributes(new ExtendableHelper(extAttributes));

        //parse OCD content
        int depth = parser.getDepth();
        while (parser.hasNext(depth)) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = parser.getName().getLocalPart();
                if (name.equalsIgnoreCase(AD)) {
                    ocd.addAttribute(parseAD(parser));
                }
            }
        }
        return ocd;
    }

    /**
     * @param xmlStreamReader
     * @return
     */
    private AttributeDefinitionSpecification parseAD(DepthAwareXMLStreamReader parser) throws XMLStreamException {
        AttributeDefinitionSpecification ad = new AttributeDefinitionSpecification();

        //extension attributes <URI, <attr, val>>
        Map<String, Map<String, String>> extAttributes = new HashMap<String, Map<String, String>>();

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            QName qname = parser.getAttributeName(i);
            String attrVal = parser.getAttributeValue(i);

            if (qname.equals(ID)) {
                ad.setId(attrVal);
            } else if (qname.equals(NAME)) {
                ad.setName(getLocalizedName(attrVal));
                String defaultName = ad.getName();
                String attributeName = getLocalizedName(attrVal + "$Ref");
                ad.setAttributeName(attributeName.equals(attrVal + "$Ref") ? defaultName : attributeName);
            } else if (qname.equals(DESCRIPTION)) {
                ad.setDescription(getLocalizedName(attrVal));
            } else if (qname.equals(TYPE)) {
                ad.setType(parseType(attrVal));
            } else if (qname.equals(CARDINALITY)) {
                ad.setCardinality(Integer.parseInt(attrVal));
            } else if (qname.equals(DEFAULT)) {
                List<String> values = MetaTypeHelper.parseValue(attrVal);
                ad.setDefaultValue(values.toArray(new String[values.size()]));
            } else if (qname.equals(REQUIRED)) { // default true
                ad.setRequired(Boolean.parseBoolean(attrVal));
            } else if (qname.equals(MIN)) {
                ad.setMinValue(attrVal);
            } else if (qname.equals(MAX)) {
                ad.setMaxValue(attrVal);
            }

            String namespace = qname.getNamespaceURI();
            //support extension attributes
            if (namespace.length() != 0 && !!!namespace.startsWith(METATYPE_URI)) {
                if (extAttributes.containsKey(namespace)) {
                    Map<String, String> attrMap = extAttributes.get(namespace);
                    attrMap.put(qname.getLocalPart(), attrVal);
                } else {
                    HashMap<String, String> attrMap = new HashMap<String, String>();
                    attrMap.put(qname.getLocalPart(), attrVal);
                    extAttributes.put(namespace, attrMap);
                }
            }
        }

        ad.setExtendedAttributes(new ExtendableHelper(extAttributes));

        int depth = parser.getDepth();
        while (parser.hasNext(depth)) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = parser.getName().getLocalPart();
                if (name.equalsIgnoreCase(OPTION)) {
                    ad.addValueOption(parseOption(parser));
                }
            }
        }
        return ad;
    }

    /**
     * @param xmlStreamReader
     * @return
     */
    private DesignateSpecification parseDesignate(DepthAwareXMLStreamReader parser) throws XMLStreamException {
        DesignateSpecification ds = new DesignateSpecification();

        String pid = null;
        String factoryPid = null;

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            QName qname = parser.getAttributeName(i);
            String attrVal = parser.getAttributeValue(i);

            if (qname.equals(PID)) {
                pid = attrVal;
            } else if (qname.equals(FACTORY_PID)) {
                factoryPid = attrVal;
            }
        }

        if (factoryPid != null) {
            ds.setIsFactory(true);
            ds.setPid(factoryPid);
        } else if (pid != null) {
            ds.setIsFactory(false);
            ds.setPid(pid);
        } else {
            throw new IllegalStateException("pid and factoryPid is null");
        }

        int depth = parser.getDepth();
        while (parser.hasNext(depth)) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String name = parser.getName().getLocalPart();
                if (name.equalsIgnoreCase(OBJECT)) {
                    ds.setOcdId(parseObject(parser));
                }
            }
        }

        return ds;
    }

    private String parseObject(DepthAwareXMLStreamReader parser) {
        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            QName qName = parser.getAttributeName(i);
            String attrVal = parser.getAttributeValue(i);
            if (qName.equals(OCDREF)) {
                return attrVal;
            }
        }
        return null;
    }

    /**
     * 
     * @param xmlStreamReader
     * @return
     */
    private String[] parseOption(DepthAwareXMLStreamReader parser) {
        int attrCount = parser.getAttributeCount();
        String[] option = new String[2];
        for (int i = 0; i < attrCount; i++) {
            QName qname = parser.getAttributeName(i);
            String attrVal = parser.getAttributeValue(i);

            if (qname.equals(VALUE)) {
                option[0] = attrVal;
            } else if (qname.equals(LABEL)) {
                option[1] = getLocalizedName(attrVal);
            }
        }
        return option;
    }

    /**
     * @param attrVal
     * @return
     */
    private int parseType(String type) {
        type = type.toLowerCase();
        if (type.equals("long")) {
            return AttributeDefinition.LONG;
        } else if (type.equals("double")) {
            return AttributeDefinition.DOUBLE;
        } else if (type.equals("float")) {
            return AttributeDefinition.FLOAT;
        } else if (type.equals("integer")) {
            return AttributeDefinition.INTEGER;
        } else if (type.equals("byte")) {
            return AttributeDefinition.BYTE;
        } else if (type.equals("char")) {
            return AttributeDefinition.CHARACTER;
        } else if (type.equals("boolean")) {
            return AttributeDefinition.BOOLEAN;
        } else if (type.equals("short")) {
            return AttributeDefinition.SHORT;
        } else if (type.equals("password")) {
            return AttributeDefinition.PASSWORD;
        } else {
            //defaults to string at least that's what SchemaWriter does
            return AttributeDefinition.STRING;
        }
    }

}
