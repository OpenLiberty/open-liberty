/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.pmi;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.server.ModuleConfigParser;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsTemplateLookup;

public class PerfModules implements PmiConstants {
    public static final String[] moduleIDs50 =
    {
        BEAN_MODULE,
        CACHE_MODULE,
        CONNPOOL_MODULE,
        J2C_MODULE,
        RUNTIME_MODULE,
        ORBPERF_MODULE,
        SYSTEM_MODULE,
        SESSIONS_MODULE,
        THREADPOOL_MODULE,
        TRAN_MODULE,
        WEBAPP_MODULE,
        WLM_MODULE,
        WEBSERVICES_MODULE,
        WSGW_MODULE
    }; // 86523.26

    public static final String[] moduleIDs40 =
    {
        BEAN_MODULE,
        CONNPOOL_MODULE,
        RUNTIME_MODULE,
        JVMPI_MODULE,
        SESSIONS_MODULE,
        THREADPOOL_MODULE,
        TRAN_MODULE,
        WEBAPP_MODULE,
    };

    public static String[] moduleIDs = moduleIDs50;
    private static String modulePrefix = DEFAULT_MODULE_PREFIX;

    // a hashtable to keep the references for all the module configs
    private static HashMap moduleConfigs = new HashMap();

    // custom config file. xml file -> PmiModuleConfig map
    //private static HashMap customModuleConfigs = new HashMap();

    // a parser object to parse the XML config file for each module
    private static ModuleConfigParser parser = new ModuleConfigParser();

    final static int MODULE_INDEX = 1;
    final static int SUBMODULE_INDEX = MODULE_INDEX + 2;
    final static int METHOD_PATH = SUBMODULE_INDEX + 2;

    private static TraceComponent tc = Tr.register(PerfModules.class);
    private static Exception parseException;

    private static java.lang.reflect.Method mGetPmiModuleConfigMethod;
    private static boolean bEnableStatsTemplateLookup = true;

    private static ArrayList lookupList = new ArrayList(3);

    private static String ENABLE_PARSING_FLAG = "com.ibm.websphere.pmi.enableModuleConfigParsing";

    static {
        String enableParsingFlag = System.getProperty(ENABLE_PARSING_FLAG);
        if (enableParsingFlag != null && enableParsingFlag.equalsIgnoreCase("true")) {
            bEnableStatsTemplateLookup = false;
            /*
             * if(tc.isDebugEnabled())
             * Tr.debug (tc, "com.ibm.websphere.pmi.enableModuleConfigParsing is enabled. Disabling StatsTemplateLookup.");
             */} else {
            bEnableStatsTemplateLookup = true;
            /*
             * if(tc.isDebugEnabled())
             * Tr.debug (tc, "Enabling StatsTemplateLookup.");
             */
            // register PMI component's StatsTemplateLookup class
            try {
                Class lookupClass = Class.forName("com.ibm.ws.pmi.preprocess.pmi_StatsTemplateLookup");
                StatsFactory.registerStatsTemplateLookup((StatsTemplateLookup) lookupClass.newInstance());
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "PMI StatsTemplateLookup class not found.");
                }
            }
        }
    }

    /**
     * Init moduleConfigs with array moduleIDs - use modulePrefix
     * because they are all websphere default PMI modules
     */
    private synchronized static void initConfigs() {
        for (int i = 0; i < moduleIDs.length; i++) {
            // add module configs one by one
            //addModuleInfo(moduleIDs[i] + modulePrefix);
            // don't do validation since in pre-defined module
            getConfigFromXMLFile(getXmlFileName(modulePrefix + moduleIDs[i]), true, false);
        }
    }

    /*
     * VELA:
     * moduleConfigs Hashmap will use UID as key instead of shortname
     * the modulename for pre-defined will be shortname in PerfDescriptor
     * the statsType for pre-defined will be UID in Stats
     */
    // return PmiModuleConfig for a given moduleID
    public static PmiModuleConfig getConfig(String moduleID) {
        if (moduleID == null)
            return null;

        PmiModuleConfig config = (PmiModuleConfig) moduleConfigs.get(moduleID);

        if (config == null) {
            // beanModule and com.ibm.websphere.pmi.xml.beanModule will work
            // if moduleID doesn't have a '.' then use pre-defined modulePrefix
            int hasDot = moduleID.indexOf('.');
            if (hasDot == -1) {
                // not found in moduleConfigs; try as a pre-defined module
                // prepend com.ibm.websphere.pmi.xml. to moduleID
                String preDefinedMod = DEFAULT_MODULE_PREFIX + moduleID;
                config = getConfigFromXMLFile(getXmlFileName(preDefinedMod), true, false);
            }
        }

        if (config == null) {
            // input moduleID has '.' in it. use as it is            
            // do validation since its NOT pre-defined module
            config = getConfigFromXMLFile(getXmlFileName(moduleID), true, true);
        }

        return config;
    }

    // return the config for the moduleID
    public static PmiModuleConfig findConfig(PmiModuleConfig[] configs, String moduleID) {
        if (moduleID == null)
            return null;

        for (int i = 0; i < configs.length; i++) {
            if (configs[i].getUID().equals(moduleID)) // VELA
                //configs[i].getShortName().equals(moduleID))
                return configs[i];
        }
        return null;
    }

    // return the config for the MBean ObjectName
    /*
     * public static PmiModuleConfig findConfig(PmiModuleConfig[] configs, ObjectName on) {
     * if (on == null) return null;
     * 
     * String type = on.getKeyProperty("type");
     * 
     * // special cases for servlet/ejb type since they are submodule
     * if (type.equals(MBeanTypeList.SERVLET_MBEAN))
     * type = MBeanTypeList.WEBAPP_MBEAN;
     * else if (type.equals(MBeanTypeList.ENTITY_MBEAN)
     * || type.equals(MBeanTypeList.STATEFUL_MBEAN)
     * || type.equals(MBeanTypeList.STATELESS_MBEAN)
     * || type.equals(MBeanTypeList.MSD_MBEAN)//663045
     * || type.equals(MBeanTypeList.SINGLETON_MBEAN))//663045
     * type = MBeanTypeList.EJBMODULE_MBEAN;
     * 
     * for (int i=0; i<configs.length ; i++) {
     * 
     * String mtype = configs[i].getMbeanType();
     * if (mtype != null && mtype.equals(type))
     * return configs[i];
     * }
     * return null;
     * }
     */

    // Custom PMI
    // bFromCache = false: parse xml and create a new instance of PmiModuleConfig instead of reading from cache. 
    // This is used for JSR77 support (from com.ibm.ws.pmi.stat.StatsConfigHelper) to get static info in client side
    // In JSR77 support we need English (translated) name, unit, description. So we dont want to
    // the cached object in this class as it may be used by PmiClient etc.
    // FIXED: Alternative is to create clone PmiModuleConfig. If someone calls getStatistics in server JVM there the file
    // will parsed one more time
    public synchronized static PmiModuleConfig getConfigFromXMLFile(String xmlFilePath, boolean bFromCache, boolean bValidate) {
        //System.out.println ("[PerfModules] getConfigFromXMLFile(): " +xmlFilePath);
        PmiModuleConfig config = null;
        // if xmlFilePath = /com/ibm/websphere/pmi/customMod.xml
        // the moduleName/ID = customMod

        // Check in HashMap only if "bFromCache" is true
        // Otherwise, create a new instance of PmiModuleConfig - used in StatsImpl
        // to return the translated static info        

        // VELA: PmiModuleConfig is cached with UID and not the shortName
        String modUID = getModuleUID(xmlFilePath);
        //System.out.println("### ModUID="+modUID);
        config = (PmiModuleConfig) moduleConfigs.get(modUID);
        //System.out.println("### config="+config);
        if (bFromCache) {
            // PmiModuleConfig is cached with moduleID (last part of the xml file name) as key.            
            // This ASSUMES that moduleID is same as the XML file name
            // If moduleID and xml file names are different then XML will be parsed and loaded each time
            // NOTE:Module Name *MUST* be unique!
            /*
             * File f = new File (xmlFilePath);
             * String fileName = f.getName();
             * int dotLoc = fileName.lastIndexOf(".");
             * if (dotLoc != -1)
             * {
             * String modName = fileName.substring (0, dotLoc-1);
             * config = (PmiModuleConfig)moduleConfigs.get (modName);
             * 
             * if(config != null)
             * {
             * return config;
             * }
             * }
             */
            if (config != null) {
                return config;
            }
        } else {
            // FIXED: When bFromCache = false and create a copy of the PmiModuleConfig instead of parsing the XML file again
            // added copy () to PmiModuleConfig and PmiDataInfo
            if (config != null) {
                return config.copy();
            }
        }
        //System.out.println("#### not found in cache");
        // Not found in cache. Try StatsTemplateLookup before loading from disk.          
        if (bEnableStatsTemplateLookup) {
            int lookupCount = lookupList.size();
            for (int i = 0; i < lookupCount; i++) {
                config = ((StatsTemplateLookup) lookupList.get(i)).getTemplate(modUID);
                if (config != null)
                    break;
                else {
                    // may be pre-defined. cut "com.ibm.websphere.pmi.xml." - 26 chars
                    if (modUID.startsWith(DEFAULT_MODULE_PREFIX)) {
                        config = ((StatsTemplateLookup) lookupList.get(i)).getTemplate(modUID.substring(26));
                        if (config != null)
                            break;
                    }
                }
            }

            if (config != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "StatsTemplateLookup available for: " + xmlFilePath);

                config.setMbeanType("");
                moduleConfigs.put(config.getUID(), config);

                if (!bFromCache)
                    return config.copy();
                else
                    return config;
            } else {
                //System.out.println("---1 StatsTemplateLookup NOT available for: " + xmlFilePath );
                if (tc.isDebugEnabled())

                    Tr.debug(tc, "StatsTemplateLookup NOT available for: " + xmlFilePath);
            }
        }
        //System.out.println("StatsTemplateLookup NOT available for: " + xmlFilePath );

        // Not found in hard-coded lookup class. Load file from disk and parse it        
        // d175652: ------ security code
        final String _xmlFile = xmlFilePath;
        final boolean _bDTDValidation = bValidate;
        parseException = null;

        try {
            //System.out.println("######## tryingt to get Config for "+_xmlFile);
            config = (PmiModuleConfig)
                            AccessController.doPrivileged(
                                            new PrivilegedExceptionAction()
                            {
                                public Object run() throws Exception
                            {
                                return parser.parse(_xmlFile, _bDTDValidation);
                            }
                            });
        } catch (PrivilegedActionException e) {
            parseException = e.getException();
        }

        // ------ security code

        // Not in HashMap. Parse file
        //config = parser.parse(xmlFilePath, bValidate);   // validate with DTD
        //System.out.println("######## Config ="+config);
        if (config != null) {
            if (bFromCache) {
                // there is no mbean type defined in template for custom pmi template
                // so set to none - to avoid PMI0006W in PmiJmxMapper
                config.setMbeanType("");

                //String moduleID = config.getShortName();
                //moduleConfigs.put(moduleID, config);

                // VELA: cache using UID as key "com.ibm.websphere.pmi.xml.beanModule"
                moduleConfigs.put(config.getUID(), config);

                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Read PMI config for stats type: " + config.getUID());

                return config;
            } else
                return config.copy();
        } else {
            return config; // null
        }
    }

    // Custom PMI
    public static PmiModuleConfig getConfigFromXMLFile(String xmlFilePath) {
        return getConfigFromXMLFile(xmlFilePath, true, true);
    }

    // return all the ModuleConfigs
    public static PmiModuleConfig[] getConfigs40() {
        int index = modulePrefix.indexOf("xml.");
        if (index > 0) {
            moduleIDs = moduleIDs40;
            modulePrefix = modulePrefix.substring(0, index);
            moduleConfigs = new HashMap();
        }
        initConfigs();
        return getConfigs();
    }

    /**
     * Convert data id to data name
     */
    public static String getDataName(String moduleName, int dataId) {
        PmiModuleConfig config = PerfModules.getConfig(moduleName);
        if (config == null)
            return null;

        PmiDataInfo info = config.getDataInfo(dataId);
        if (info == null)
            return null;
        else
            return info.getName();
    }

    /**
     * Convert data name to dataId
     */
    public static int getDataId(String moduleName, String dataName) {
        PmiModuleConfig config = PerfModules.getConfig(moduleName);
        // attach module name to it - this has to be consistent with the PMI xml config files
        // under com/ibm/websphere/pmi/xxModule.xml
        if (dataName.indexOf('.') < 0)
            dataName = moduleName + "." + dataName;
        if (config != null)
            return config.getDataId(dataName);
        else
            return -1;
    }

    public static PmiModuleConfig[] getConfigs() {
        if (moduleConfigs.size() == 0)
            initConfigs();

        PmiModuleConfig[] res = new PmiModuleConfig[moduleConfigs.size()];

        Object[] values = moduleConfigs.values().toArray();
        for (int i = 0; i < values.length; i++) {
            res[i] = (PmiModuleConfig) values[i];
        }

        return res;
    }

    /**
     * For user to add customized module:
     * create module config, add to hash table
     */
    public synchronized static PmiModuleConfig addModuleInfo(String key) //String moduleID, String modulePrefix)
    {
        return null;
        /*
         * PmiModuleConfig config = (PmiModuleConfig)moduleConfigs.get (key);
         * 
         * if (config == null)
         * {
         * config = parser.parse (getXmlFileName(key));
         * if (config != null)
         * {
         * moduleConfigs.put (key, config);
         * if(tc.isDebugEnabled())
         * Tr.debug(tc, "Read PMI config for stats type: " + config.getUID());
         * 
         * }
         * }
         * 
         * return config;
         */
        /*
         * 
         * if(moduleConfigs.containsKey(moduleID))
         * return false;
         * 
         * // pre-defined path. no DTD validation
         * PmiModuleConfig config = parser.parse(getXmlFileName(modulePrefix + moduleID));
         * if(config==null)
         * {
         * return false;
         * }
         * else
         * {
         * moduleConfigs.put(moduleID, config);
         * return true;
         * }
         */
    }

    /**
     * Return either the submodule name or the category name depending on its path.
     */
    /*
     * public static String getGroupName(PerfLevelSpec pld)
     * {
     * if(pld == null)
     * return null;
     * 
     * // note: It is not good, but we have to do a special thing for CacheModule, OrbModule, and WLMModule
     * // since they do not following the normal PMI path.
     * String moduleName = pld.getModuleName();
     * String[] path = pld.getPath();
     * 
     * if(path != null && path.length > METHOD_PATH && path[METHOD_PATH].equals(BEAN_METHODS_SUBMODULE))
     * {
     * return path[METHOD_PATH];
     * }
     * else if(path != null && path.length >= SUBMODULE_INDEX)
     * {
     * if(moduleName.equals(CACHE_MODULE) || moduleName.equals(ORBPERF_MODULE) || moduleName.equals(WLM_MODULE))
     * {
     * return path[SUBMODULE_INDEX - 1];
     * }
     * else if(path.length > SUBMODULE_INDEX)
     * {
     * return path[SUBMODULE_INDEX];
     * }
     * }
     * return null;
     * }
     */
    /**
     * Convert "." to "/" in moduleID and attach "xml" as the suffix
     * Note: this a convention that has to be followed.
     * Otherwise, cannot find the xml config file.
     */
    public static String getXmlFileName(String moduleID) {
        String fileName = "/" + moduleID.replace('.', '/') + ".xml";
        return fileName;
    }

    // VELA
    public static String getModuleUID(String templateName) {
        // /com/ibm/websphere/pmi/beanModule.xml -> com.ibm.websphere.pmi.beanModule
        String ftype = templateName.replace('/', '.');

        // replace first '.'
        if (ftype.charAt(0) == '.') {
            ftype = ftype.substring(1);
        }

        // cut last ".xml"
        return ftype.substring(0, ftype.length() - 4);
    }

    public static String getParseExceptionMsg() {
        if (parseException != null)
            return parseException.getMessage();
        else
            return null;
    }

    public static void registerTemplateLookupClass(StatsTemplateLookup lookupClass) {
        lookupList.add(lookupClass);
    }

    /**
     * @param configXmlPath
     * @param currentBundle
     * @return
     */
    public static PmiModuleConfig getConfigFromXMLFile(String configXmlPath, Bundle currentBundle) {
        //System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^ -1 Bundle name = " + currentBundle);
        if (currentBundle == null) {
            return getConfigFromXMLFile(configXmlPath);
        }
        //System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^ 0 Bundle name = " + currentBundle);
        if (parser == null) {
            return getConfigFromXMLFile(configXmlPath);
        }
        //System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^ 1 Bundle name = " + currentBundle);
        parser.setLoadXMLUsingBundle(currentBundle);
        return getConfigFromXMLFile(configXmlPath);
    }
}