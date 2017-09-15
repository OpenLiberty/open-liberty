/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.pmi.server;

//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.w3c.dom.Element;
//import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiDataInfo;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.xml.ParserFactory;

/**
 * This is a xml parser which is designed to process Performance configuration
 * xml files. Rewrite it using SAX parser.
 */
public class ModuleConfigParser extends DefaultHandler implements PmiConstants {
    public static final String STATS_DTD = "/com/ibm/websphere/pmi/xml/stats.dtd";
    public static final String VALIDATE_DESCRIPTOR_PROPERTY = "com.ibm.websphere.pmi.validateDescriptors";
    // this is the object we will put our parsing result in
    private PmiModuleConfig oneModule;
    private PmiDataInfo currentData;

    private PmiDataInfo submoduleData;
    private String currentChars = null;
    private final TraceComponent tc = Tr.register(ModuleConfigParser.class);
    private String submoduleName = null;
    private boolean isModule = true;
    private String _fileName = null;
    private boolean _validate = false;
    private boolean bExtension = false;
    private Bundle loadXMLUsingBundle = null;

    public Bundle getLoadXMLUsingBundle() {
        return loadXMLUsingBundle;
    }

    public void setLoadXMLUsingBundle(Bundle b) {
        this.loadXMLUsingBundle = b;
    }

    /**
     * the stack of object to save the context information during parsing.
     */
    private Locator locator = null;
    // constants:
    private static String PERF_MODULE = "PerfModule";
    private static String STATS = "Stats"; // => PerfModule
    private static String PERF_LONG = "PerfLong";
    private static String COUNT_STAT = "CountStatistic"; // => PerfLong
    private static String PERF_DOUBLE = "PerfDouble";
    private static String DOUBLE_STAT = "DoubleStatistic"; // => PerfDouble
    private static String PERF_LOAD = "PerfLoad";
    private static String BOUNDED_RANGE_STAT = "BoundedRangeStatistic"; // => PerfLoad
    private static String PERF_STAT = "PerfStat";
    private static String TIME_STAT = "TimeStatistic"; // => PerfStat
    private static String AVG_STAT = "AverageStatistic"; // => PerfStat
    private static String RANGE_STAT = "RangeStatistic"; // => PerfLoad
    private static String PERF_SUBMODULE = "PerfSubModule";
    private static String UID = "UID";
    private static String DESCRIPTION = "description";
    private static String NAME = "name";
    private static String LEVEL = "level";
    private static String STATISTICSET = "statisticSet";
    private static String PLATFORM = "platform";
    private static String UNIT = "unit";
    private static String RESETTABLE = "resettable";
    private static String AGGREGATABLE = "aggregatable";
    private static String ZOS_AGGREGATABLE = "zosAggregatable";
    private static String ON_REQUEST = "updateOnRequest";
    private static String CATEGORY = "category";
    private static String COMMENT = "comment";
    private static String DEPENDENCY = "dependency";
    private static String MBEAN_TYPE = "mBeanType";
    private static String STATS_NLS_FILE = "resourceBundle";
    private static String TYPE = "type";
    private static String EXTENSION_TAG = "EXTENSION";

    public ModuleConfigParser() {
        // init
        oneModule = null;
    }

    public PmiModuleConfig parse(String fileName) throws Exception {
        return loadDescriptor(fileName, false);
    }

    // added synchronized to avoid scenarios where multiple threads register PMI module (at the same time)
    // and requires parsing of file
    public synchronized PmiModuleConfig parse(String fileName, boolean validate) throws Exception {
        return loadDescriptor(fileName, validate);
    }

    public PmiModuleConfig loadDescriptor(String fileName, boolean validate)
                    throws Exception {
        return loadDescriptor(fileName, validate, true);
    }

    /**
     * load a xml file.
     * 
     * @exception DescriptorParseException
     *                Reading the descriptor xml file failed.
     * @return a PmiModuleCOnfig object.
     */
    public PmiModuleConfig loadDescriptor(String fileName, boolean validate, boolean loadFromClassPathOnly)
                    throws Exception {
        loadFromClassPathOnly = false;
        if (tc.isEntryEnabled())
            Tr.entry(tc, "loadDescriptor: parsing stats xml " + fileName + "; dtd validation: " + validate);

        // reset variables:
        oneModule = null;
        currentData = null;
        submoduleName = null;
        submoduleData = null;
        _fileName = fileName;
        _validate = validate;
        InputStream istream = null;
        boolean usingCC = false;

        if (fileName.indexOf('=') != (-1)) {
            // We check for an equals symbol, so that we can determine if
            // this resource should be loaded from the extenstion point repository.
            // Extension point lookups are encoded using a 'EXTENSION=xxxx'
            // encoding scheme.
            String[] tokens = fileName.split("=");
            if (tokens.length > 1) {
                if (tokens[0].equalsIgnoreCase(EXTENSION_TAG)) {
                    bExtension = true;
                    _fileName = tokens[1];
                    try {
                        parseFromExtension(_fileName);
                    } catch (Exception ex) {
                        if (tc.isDebugEnabled())
                            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.pmi.server.ModuleConfigParser.loadDescriptor", "97", this);
                        throw ex;
                    }
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "loadDescriptor");

                    return oneModule;
                }
            }
        }
        // No extension
        bExtension = false;
        if (istream == null) {
            istream = getResourcefromBundle(fileName);
            //System.out.println("ISTREME is ++++++++++++++++ " + istream);
            if (istream == null) {
                istream = getClass().getResourceAsStream(fileName);
            }
            usingCC = false;
            if (istream == null) {
                istream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
                usingCC = true;
            }
            if (istream == null) {
                istream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName.substring(1));
                usingCC = true;
            }
            if (istream == null) {
                Tr.error(tc, "PMI0010W", fileName);
                return null;
            }

        }

        try {
            //**JDK1.4.1 fix
            //**XMLReader parser = ParserFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            XMLReader parser = ParserFactory.createXMLReader();
            parser.setFeature("http://xml.org/sax/features/namespaces", true);

            // commenting this -D flag
            // boolean validateDescriptors = Boolean.getBoolean(VALIDATE_DESCRIPTOR_PROPERTY);
            // skip DTD validation for pre-defined modules (perf.dtd)
            // do DTD validataion for custom PMI modules (stats.dtd)
            parser.setFeature("http://xml.org/sax/features/validation", validate);
            //** FIX: need to remove this feature?
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", validate);
            parser.setEntityResolver(this);
            parser.setContentHandler(this);
            parser.setErrorHandler(this);
            parser.parse(new InputSource(istream));
        } catch (Exception ex) {
            if (tc.isDebugEnabled())
                ex.printStackTrace();
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.pmi.server.ModuleConfigParser.loadDescriptor", "97", this);
            throw ex;
        } finally {
            istream.close();
        }

        String uid = PerfModules.getModuleUID(fileName);

        //int index = uid.lastIndexOf(".");
        //String newUID = uid.substring(index+1);
        // instantiate oneModule
        //oneModule = new PmiModuleConfig (newUID);
        com.ibm.ws.pmi.stat.StatsConfigHelper.translateAndCache(oneModule, null);
        //        System.out.println("OneModule = " + oneModule);
        if (tc.isEntryEnabled())
            Tr.exit(tc, "loadDescriptor");

        return oneModule;
    }

    /**
     * @return
     */
    private InputStream getResourcefromBundle(String path) {
        //System.out.println("Looking for file " + path);
        //System.out.println("Looking for Bundle " + loadXMLUsingBundle);

        if (loadXMLUsingBundle != null) {
            URL u = loadXMLUsingBundle.getEntry(path);
            if (u != null) {
                try {
                    return u.openStream();
                } catch (IOException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "IO Exception was thrown. Please check FFDC Files.");
                    }
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.pmi.server.ModuleConfigParser.getResourcefromBundle", "257", this);

                }
            }
        }
        return null;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public InputSource resolveEntity(java.lang.String publicId, java.lang.String systemId)
                    throws SAXException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resolveEntity", systemId);
        InputSource inputSource = null;
        try {
            // validate against stats.dtd ONLY and NOT perf.dtd
            if (systemId.endsWith("stats.dtd")) {
                InputStream in = getClass().getResourceAsStream(STATS_DTD);
                inputSource = new InputSource(in);
            }
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.pmi.server.ModuleConfigParser.resolveEntity", "126", this);
            Tr.error(tc, "PMI0010W", systemId);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "resolveEntity");
            return null;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "resolveEntity");
        return inputSource;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "error", exception);
        throw new SAXException(exception);
    }

    @Override
    public void startDocument() throws SAXException {
        //if(tc.isEntryEnabled()) Tr.entry(tc, "startDocument");
        //if(tc.isEntryEnabled()) Tr.exit(tc, "startDocument");
    }

    @Override
    public void endDocument() throws SAXException {
        //if(tc.isEntryEnabled()) Tr.entry(tc, "endDocument");
        //if(tc.isEntryEnabled()) Tr.exit(tc, "endDocument");
    }

    @Override
    public void startElement(String uri, String localpart, String rawname, Attributes attributes)
                    throws SAXException {
        //if(tc.isEntryEnabled())
        //    Tr.entry(tc, "startElement, localpart=" + localpart);
        if (rawname.equals(PERF_MODULE)) {
            // VELA: this path is for pre-defined/pre-custom only.
            isModule = true;
            //String uid = attributes.getValue(UID);
            // Ignoring UID for pre-defined modules.
            // Use the last part of the fileName as UID since the pre-defined module XML files are
            // always found under com.ibm.websphere.pmi.xml
            // In Vela UID indicates the stats type. When binding static config prepend com.ibm.websphere.pmi.xml
            // to stats type and locate the xml file.
            // This design will work even if the fileName is different than the UID specified in the XML

            String uid = PerfModules.getModuleUID(_fileName);

            int index = uid.lastIndexOf(".");
            String newUID = uid.substring(index + 1);
            // instantiate oneModule
            oneModule = new PmiModuleConfig(newUID);

            return;
        }

        if (rawname.equals(STATS)) {
            isModule = true;
            String type = attributes.getValue(TYPE);
            // VELA: if DTD validation is enabled check if stats type and file location match
            // DTD validation is enabled for Custom PMI and not for pre-defined modules
            if (_validate) {
                String moduleUID;
                if (bExtension)
                    moduleUID = _fileName; //filename is really the 'extension' name in this case
                else
                    moduleUID = PerfModules.getModuleUID(_fileName);
                if (!(moduleUID.equals(type))) {
                    //System.out.println ("Template doesn't match with Stat type: template = " +_fileName + "; type = " + type);
                    throw new SAXException("Stats template name does not match with stat type: template = " + _fileName + "; stats type = " + type);
                }
            }
            // instantiate oneModule
            oneModule = new PmiModuleConfig(type);
            return;
        }

        if (rawname.equals(DESCRIPTION)) {
            // do nothing
        } else if (rawname.equals(STATS_NLS_FILE)) {
            // do nothing
        } else if (rawname.equals(COUNT_STAT) || rawname.equals(TIME_STAT) ||
                   rawname.equals(RANGE_STAT) || rawname.equals(BOUNDED_RANGE_STAT) ||
                   rawname.equals(AVG_STAT) || rawname.equals(DOUBLE_STAT) ||
                   rawname.equals(PERF_LONG) || rawname.equals(PERF_LOAD) ||
                   rawname.equals(PERF_STAT) || rawname.equals(PERF_DOUBLE) ||
                   rawname.equals(PERF_SUBMODULE)) {
            isModule = false;
            int dataId = Integer.parseInt(attributes.getValue("ID"));
            currentData = new PmiDataInfo(dataId);
            currentData.setName(attributes.getValue(NAME));
            currentData.setSubmoduleName(submoduleName);
            if (rawname.equals(COUNT_STAT) || rawname.equals(PERF_LONG))
                currentData.setType(TYPE_LONG);
            else if (rawname.equals(BOUNDED_RANGE_STAT) || rawname.equals(PERF_LOAD))
                currentData.setType(TYPE_LOAD);
            else if (rawname.equals(RANGE_STAT))
                currentData.setType(TYPE_RANGE);
            else if (rawname.equals(PERF_STAT) || rawname.equals(TIME_STAT))
                currentData.setType(TYPE_STAT);
            else if (rawname.equals(PERF_DOUBLE) || rawname.equals(DOUBLE_STAT))
                currentData.setType(TYPE_DOUBLE);
            else if (rawname.equals(AVG_STAT))
                currentData.setType(TYPE_AVGSTAT);
            else if (rawname.equals(PERF_SUBMODULE)) {
                currentData.setType(TYPE_SUBMODULE);
                submoduleName = currentData.getName();
            }
            oneModule.addData(currentData);
        } else if (rawname.equals(LEVEL)) {
            // do nothing
        } else if (rawname.equals(CATEGORY)) {
            // do nothing
        } else if (rawname.equals(UNIT)) {
            // do nothing
        } else if (rawname.equals(AGGREGATABLE)) {
            // do nothing
        } else if (rawname.equals(ZOS_AGGREGATABLE)) {
            // do nothing
        } else if (rawname.equals(RESETTABLE)) {
            // do nothing
        } else if (rawname.equals(COMMENT)) {
            // do nothing
        }
        currentChars = null;
    }

    @Override
    public void endElement(String uri, String localpart, String rawname) throws SAXException {
        //if(tc.isDebugEnabled())
        //    Tr.debug(tc, "endElement={uri="+uri+", localpart="+localpart+", rawName=" + rawname + ", chars=" + currentChars + '}');
        if (rawname.equals(PERF_MODULE) || rawname.equals(STATS)) {
        } else if (rawname.equals(DESCRIPTION)) {
            if (isModule) {
                oneModule.setDescription(currentChars);
            } else {
                currentData.setDescription(currentChars);
            }
        } else if (rawname.equals(STATS_NLS_FILE)) {
            if (isModule) {
                oneModule.setResourceBundle(currentChars); //NLS file
            }
        } else if (rawname.equals(PERF_LONG) || rawname.equals(PERF_LOAD) ||
                   rawname.equals(PERF_STAT) || rawname.equals(PERF_DOUBLE) ||
                   rawname.equals(COUNT_STAT) || rawname.equals(BOUNDED_RANGE_STAT) ||
                   rawname.equals(RANGE_STAT) || rawname.equals(AVG_STAT) ||
                   rawname.equals(TIME_STAT) || rawname.equals(DOUBLE_STAT)) {
            // do nothing
        } else if (rawname.equals(PERF_SUBMODULE)) {
            submoduleName = null;
        } else if (rawname.equals(LEVEL)) {
            if (currentChars.equals("low"))
                currentData.setLevel(LEVEL_LOW);
            else if (currentChars.equals("medium"))
                currentData.setLevel(LEVEL_MEDIUM);
            else if (currentChars.equals("high"))
                currentData.setLevel(LEVEL_HIGH);
            else if (currentChars.equals("maximum"))
                currentData.setLevel(LEVEL_MAX);
            else
                currentData.setLevel(LEVEL_DISABLE);
        } else if (rawname.equals(CATEGORY)) {
            currentData.setCategory(currentChars);
        } else if (rawname.equals(UNIT)) {
            currentData.setUnit(currentChars);
        } else if (rawname.equals(STATISTICSET)) {
            currentData.setStatisticSet(currentChars);
        } else if (rawname.equals(PLATFORM)) {
            currentData.setPlatform(currentChars);
        } else if (rawname.equals(RESETTABLE)) {
            if (currentChars.equals("false"))
                currentData.setResettable(false);
        } else if (rawname.equals(AGGREGATABLE)) {
            if (currentChars.equals("false"))
                currentData.setAggregatable(false);
        } else if (rawname.equals(ZOS_AGGREGATABLE)) {
            if (currentChars.equals("false"))
                currentData.setZosAggregatable(false);
        } else if (rawname.equals(COMMENT)) {
            currentData.setComment(currentChars);
        } else if (rawname.equals(ON_REQUEST)) {
            if (currentChars.equals("true"))
                currentData.setOnRequest(true);
        } else if (rawname.equals(DEPENDENCY)) {
            try {
                int dataId = Integer.parseInt(currentChars);
                currentData.addDependency(dataId);
            } catch (Exception e) {

            }
        } else if (rawname.equals(MBEAN_TYPE)) {
            // set in PmiModuleConfig
            oneModule.setMbeanType(currentChars);
            // Note: If this mapping has any conlict with the mapping in pmiJmxMapper.xml,
            //       the one in pmiJmxMapper.xml wins.
            /*
             * if(isModule)
             * PmiRegistry.jmxMapper.setMapping(oneModule.getShortName(), null, null, currentChars);
             * else // should be submodule
             * PmiRegistry.jmxMapper.setMapping(oneModule.getShortName(), null, currentData.getName(), currentChars);
             */}
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        //if(tc.isEntryEnabled()) Tr.entry(tc, "characters");
        String tmpStr = new String(ch, start, length);
        if (currentChars == null)
            currentChars = tmpStr;
        else
            currentChars += tmpStr;
        //if(tc.isEntryEnabled()) Tr.exit(tc, "characters", currentChars);
    }

    private String getXmlFileName(String moduleID) {
        String fileName = "/" + moduleID.replace('.', '/') + ".xml";
        // debug only
        return fileName;
        //return moduleID + ".xml";
    }

    private static AttributesImpl attrList; // declare once, reuse

    private void parseFromExtension(String strExtension) {
        //System.out.println("DSAfasdfsadfasdf");
        /*
         * IExtensionRegistry registry = ExtensionRegistryFactory.instance().getExtensionRegistry(); //LIDB3418.77.1
         * if (registry == null)
         * {
         * Tr.error(tc,"Unable to get eclipse extension registry");
         * }
         * // get the extension point
         * String epid = ExtensionRegistryFactory.instance().getDefaultPluginID() + ".pmiCustomExtension";
         * IExtensionPoint extensionPoint = registry.getExtensionPoint(epid);
         * if (extensionPoint == null)
         * {
         * Tr.error(tc,"Unable to get extension point - " + epid);
         * }
         * if (tc.isDebugEnabled())
         * {
         * Tr.debug(tc,"Processing extension point " + epid);
         * }
         * // retrieve the named extension
         * IExtension extension = extensionPoint.getExtension(strExtension);
         * if( extension == null )
         * {
         * Tr.error(tc, "PMI0010W", strExtension);
         * return;
         * }
         * 
         * // iterate through the configuration elements
         * attrList = new AttributesImpl();
         * IConfigurationElement[] customElements = extension.getConfigurationElements();
         * for( int i=0; i<customElements.length; i++ )
         * processExtensionElement(customElements[i]);
         */}

    // This is a recursive method used to process a configuration element and its children.
    // The method calls it self after each
    /*
     * private void processExtensionElement(IConfigurationElement element)
     * {
     * try
     * {
     * System.out.println("Sdfasdfadsf");
     * getAttributesFromExtensionElement(element);
     * String name = element.getName();
     * startElement("", "", name, attrList);
     * IConfigurationElement [] children = element.getChildren();
     * if( children != null )
     * {
     * for( int i=0; i<children.length; i++ )
     * processExtensionElement(children[i]);
     * }
     * currentChars = element.getValue();
     * if( currentChars == null )
     * currentChars = "";
     * endElement("", "", name );
     * }
     * catch(Exception ex)
     * {
     * if (tc.isDebugEnabled())
     * ex.printStackTrace();
     * }
     * }
     * private void getAttributesFromExtensionElement(IConfigurationElement element )
     * {
     * // create or clear
     * attrList.clear();
     * 
     * // iterate through the attributes and add them to the table
     * String [] strAttributes = element.getAttributeNames();
     * if( strAttributes.length > 0 )
     * {
     * for( int i=0; i<strAttributes.length; i++ )
     * {
     * // this is a bit kludged. I know that internally, we are only
     * // using the 'getValue()' method on the attribute object.
     * // So, I'm only going to build up that portion of the attribute object.
     * String value = element.getAttribute(strAttributes[i]);
     * attrList.addAttribute(strAttributes[i],strAttributes[i],strAttributes[i],"String",value);
     * }
     * }
     * }
     */

    /** a test driver - it should be removed after testing */
    public static void main(String[] args) throws Exception {
        ModuleConfigParser mParser = new ModuleConfigParser();
        mParser.parse("/com/ibm/websphere/pmi/custom/test/PmiServletModule1.xml", true);
    }
}