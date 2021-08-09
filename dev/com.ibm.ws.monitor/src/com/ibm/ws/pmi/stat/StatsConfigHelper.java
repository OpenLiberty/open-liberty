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
package com.ibm.ws.pmi.stat;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.StringTokenizer;

import com.ibm.websphere.pmi.PerfModules;
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiDataInfo;
import com.ibm.websphere.pmi.PmiModuleConfig;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.pmi.server.NLS;

public class StatsConfigHelper implements PmiConstants {
    // UID/statsType -> NLS file map
    private static HashMap rawConfigMap = new HashMap();

    private static HashMap localeMap = new HashMap();

    //default locale resourcebundle cache    
    private static HashMap nlsMap = new HashMap();

    private static boolean debug = false;
    private static final String PMI_RESOURCE_BUNDLE = "com.ibm.ws.pmi.properties.PMIText";
    private static final String PMI_RESOURCE_BUNDLE_PREFIX_50 = "com.ibm.websphere.pmi.property.";
    private static final TraceComponent tc = Tr.register(StatsConfigHelper.class);

    public static void setDebug(boolean dbg) {
        debug = dbg;
    }

    // 234782 - JPM: Create key for NLS based on resource bundle name and locale
    private static final String getNlsKey(String resourceBundle, Locale locale) {
        return new StringBuffer(resourceBundle).append("#").append(locale.toString()).toString();
    }

    private static HashMap getConfigMap(Locale l) {
        if (l == null)
            l = Locale.getDefault();

        HashMap aConfigMap = (HashMap) localeMap.get(l.toString());
        if (aConfigMap == null) {
            aConfigMap = new HashMap();
            localeMap.put(l.toString(), aConfigMap);
        }

        return aConfigMap;
    }

    private static PmiModuleConfig getConfig(Locale l, String statsType) {
        PmiModuleConfig acfg = (PmiModuleConfig) (getConfigMap(l)).get(statsType);
        if (debug && acfg != null)
            System.out.println("[StatsConfigHelper] getConfig cache hit for: " + statsType);

        return acfg;
    }

    // Only default locale resourcebundles will be cached.
    // Most of the times only one locale will be in used unless server and client are in different locales
    // in which case we dont cache
    private static NLS getNLS(Locale l, String resourceBundle, String statsType) {
        // init Locale
        boolean bDefaultLocale = false;
        if (l == null || l == Locale.getDefault()) {
            bDefaultLocale = true;
            l = Locale.getDefault();
        }

        NLS aNLS = null;
        if (resourceBundle == null) {
            // resourceBundle is NULL
            // trial 1: try prepending the 5.0 default resourcebundle location prefix (to support commerce, portal, etc.)
            // trial 2: if that fails return default 6.0 PMI resourcebundle
            int trial = 1;
            do {
                if (trial == 1)
                    resourceBundle = PMI_RESOURCE_BUNDLE_PREFIX_50 + statsType;
                else if (trial == 2)
                    resourceBundle = PMI_RESOURCE_BUNDLE;

                // 234782 - JPM: Check in NLS cache
                aNLS = (NLS) nlsMap.get(getNlsKey(resourceBundle, l));
                if (aNLS != null)
                    return aNLS;
                else
                    aNLS = new NLS(resourceBundle, l, true, false); // first time create 

                if (aNLS.isResourceLoaded())
                    break;

                ++trial;
            } while (trial <= 2);
        } else {
            // resourcebundle != null
            // Custom resource bundle
            // 234782 - JPM: Check in NLS cache
            aNLS = (NLS) nlsMap.get(getNlsKey(resourceBundle, l));
            if (aNLS != null)
                return aNLS;
            else
                aNLS = new NLS(resourceBundle, l, true, false); // first time create         

            // resourcebundle not loaded. so try using contextClassLoader to support
            // custom pmi in applications (war/ear file)
            // custom pmi resource bundle that is not found in the path
            // may its an application so use context classloader                                
            if (!aNLS.isResourceLoaded()) {
                // ------ security code
                final String _rbName = resourceBundle;
                final Locale _locale = l;

                try {
                    aNLS = (NLS)
                                    AccessController.doPrivileged(
                                                    new PrivilegedExceptionAction()
                                                    {
                                                        @Override
                                                        public Object run() throws Exception
                                                        {
                                                            return new NLS(_rbName, _locale, Thread.currentThread().getContextClassLoader());
                                                        }
                                                    });
                } catch (Exception e1) {
                    Tr.warning(tc, "PMI0030W", resourceBundle);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Error loading custom resource bundle using context classloader: " + e1.getMessage());
                }
                if (aNLS == null || !aNLS.isResourceLoaded()) {
                    aNLS = null;
                }
                // ------ security code
            }
        }
        // 234782 - JPM: Cache NLS object if successfully created
        if (aNLS != null && aNLS.isResourceLoaded()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Caching resource bundle: " + resourceBundle + " locale: " + l);

            // 234782 - JPM: Cache resourcebundle for this locale
            nlsMap.put(getNlsKey(resourceBundle, l), aNLS);
        }

        return aNLS;
    }

    // Method to translate the Stats instance/group name in the tree. Used by Admin Console
    public static String getTranslatedStatsName(String statsName, String statsType, Locale locale) {
        String[] _stats = parseStatsType(statsType);
        for (int i = 0; i < _stats.length; i++) {
            PmiModuleConfig cfg = getTranslatedStatsConfig(_stats[i], locale);
            if (cfg != null) {
                NLS aNLS = getNLS(locale, cfg.getResourceBundle(), statsType);
                if (aNLS != null) {
                    String trName;
                    try {
                        trName = aNLS.getString(statsName);
                    } catch (MissingResourceException mre) {
                        trName = statsName;
                    }

                    if (trName != null)
                        return trName;
                }
            }
        }
        return statsName;
    }

    // Method to translate the Stats instance/group name. Used by Admin Console
    public static String getTranslatedStatsName(String statsName, String statsType) {
        return getTranslatedStatsName(statsName, statsType, Locale.getDefault());
    }

    // No translation.
    public static PmiModuleConfig getStatsConfig(String statsType) {
        PmiModuleConfig cfg = (PmiModuleConfig) rawConfigMap.get(statsType);
        if (cfg == null)
            cfg = _getStatsConfig(statsType, false, null);
        return cfg;
    }

    // Translated based on the server locale
    public static PmiModuleConfig getTranslatedStatsConfig(String statsType) {
        return getTranslatedStatsConfig(statsType, Locale.getDefault());
    }

    // Translated based on the server locale
    public static PmiModuleConfig getTranslatedStatsConfig(String statsType, Locale locale) {
        PmiModuleConfig aCfg = getConfig(locale, statsType);
        if (aCfg == null)
            aCfg = _getStatsConfig(statsType, true, locale);

        return aCfg;
    }

    private static PmiModuleConfig _getStatsConfig(String statsType, boolean bXlate, Locale locale) {
        if (statsType == null)
            return null;
        if (locale == null)
            locale = Locale.getDefault();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getStatsConfig for {statType, locale}:  " + statsType + ", " + locale.getLanguage());
        // 1. Identify Stats type
        String[] _statsType;
        String preCustomSubModule = null;
        boolean bPreCustomSubModule = false, bPreCustomCategory = false;

        // check if its pre-defined type with #                
        int hashPos = statsType.indexOf('#');
        if (hashPos > 0) {
            _statsType = new String[] { statsType.substring(0, hashPos) };
            if (hashPos + 1 < statsType.length())
                preCustomSubModule = statsType.substring(hashPos + 1, statsType.length());
            // hardcoding to support beanModule category since we are not supporting categories
            // anymore. other option is to use '$' as the seperator instead of '#'. 
            // currently, using '#' so there is just one convention
            if (preCustomSubModule == null)
                bPreCustomSubModule = true;
            else if (preCustomSubModule.startsWith("ejb."))
                bPreCustomCategory = true;
            else
                bPreCustomSubModule = true;
        } else {
            // parse for comma seperated stats type: Custom PMI only - when templates of different types are nested
            _statsType = parseStatsType(statsType);
        }
        if (_statsType == null || _statsType.length == 0)
            return null;

        // 2. LOOP BEGIN: create PMIModuleConfig for each Stats type
        PmiModuleConfig[] config = new PmiModuleConfig[_statsType.length];
        for (int i = 0; i < _statsType.length; i++) {
            // Check in cache
            if (bXlate) {
                PmiModuleConfig cfg = getConfig(locale, _statsType[i]);
                if (cfg != null) {
                    config[i] = cfg;
                    continue;
                }
            } else {
                PmiModuleConfig cfg = (PmiModuleConfig) rawConfigMap.get(_statsType[i]);
                if (cfg != null) {
                    config[i] = cfg;
                    continue;
                }
            }

            // NOT in cache, first hit
            // Get a new instance of PmiModuleConfig: fromCache = false, validate = false
            int hasDot = _statsType[i].indexOf('.');
            if (hasDot == -1) {
                // Typically all custom pmi modules will have a '.' (an assumption)
                // No '.'. Try as a pre-defined module by prepending com.ibm.websphere.pmi.xml.
                String preDefinedMod = "/com/ibm/websphere/pmi/xml/" + _statsType[i] + ".xml";
                config[i] = PerfModules.getConfigFromXMLFile(preDefinedMod, false, false);
            }

            if (config[i] == null) {
                // may be custom pmi module
                config[i] = PerfModules.getConfigFromXMLFile(PerfModules.getXmlFileName(_statsType[i]), false, true);
            }

            if (config[i] == null) {
                continue;
            }
            // 3. Translation & cache config           
            if (bXlate) {
                translate(config[i], locale);
                /*
                 * 
                 * // check if there is a custom nls file or use default PMI nls
                 * NLS aNLS = getNLS (locale, config[i].getResourceBundle());
                 * if (aNLS != null)
                 * {
                 * translate (config[i], aNLS);
                 * }
                 * 
                 * // do not do it here since we need to handld preCustomSubModule (step 4)
                 * // Cache translated config
                 * //getConfigMap(locale).put (config[i].getUID(), config[i]);
                 */} else {
                // do not do it here since we need to handld preCustomSubModule (step 4)
                // Cache raw config
                //rawConfigMap.put (config[i].getUID(), config[i]);                
            }
        } // LOOP END
          // 4. Combine individual configs into one
        PmiModuleConfig oneConfig = new PmiModuleConfig(statsType);

        // set resource bundle for stats of one given type.
        // required for Stats instance/group name translation
        if (_statsType.length >= 1 && config[0] != null) {
            oneConfig.setResourceBundle(config[0].getResourceBundle());
            oneConfig.setDescription(config[0].getDescription());
        }

        if (bPreCustomSubModule) {
            if (config[0] != null) {
                /*
                 * PmiDataInfo[] data = config[0].listData(preCustomSubModule);
                 * for (int k = 0; k < data.length; k++)
                 * oneConfig.addData (data[k]);
                 */

                PmiDataInfo[] data = config[0].listData(null);
                for (int k = 0; k < data.length; k++) {
                    // filter sub-module entry
                    if (data[k].getType() == PmiConstants.TYPE_SUBMODULE)
                        continue;
                    else if (data[k].getSubmoduleName() == null) {
                        // webAppModule#
                        if (preCustomSubModule == null)
                            oneConfig.addData(data[k]);
                    } else {
                        // webAppModule#webAppModule.servlet
                        if (data[k].getSubmoduleName().equals(preCustomSubModule))
                            oneConfig.addData(data[k]);
                    }
                }
            }

        } else if (bPreCustomCategory) {
            // supports ONLY beanModule category
            if (config[0] != null) {
                PmiDataInfo[] data = config[0].listData(preCustomSubModule);
                for (int k = 0; k < data.length; k++) {
                    if (data[k].getSubmoduleName() == null) {
                        // add categorized data only from the beanModule and not the methods sub-module
                        oneConfig.addData(data[k]);
                    }
                }
            }
        } else {
            // Custom PMI path           
            for (int i = 0; i < _statsType.length; i++) {
                if (config[i] != null) {
                    if (debug)
                        System.out.println("[StatsConfigHelper] adding config for: " + config[i].getUID());

                    PmiDataInfo[] data = config[i].listData(null);
                    for (int k = 0; k < data.length; k++) {
                        // the data being aggregated here should be concious of
                        // the aggregation setting.  All entries below the 'primary' group
                        // are considered 'children'.  If the child statistic is not aggregatable
                        // it shouldn't be seen by the parent.
                        if (i != 0) {
                            if ((data[k].isAggregatable()) &&
                                (data[k].getType() != PmiConstants.TYPE_SUBMODULE))
                                oneConfig.addData(data[k]);
                        } else {
                            if (data[k].getType() != PmiConstants.TYPE_SUBMODULE)
                                oneConfig.addData(data[k]);
                        }
                    }
                }
            }
        }

        // do not cache empty config.
        //if (oneConfig.getNumData() == 0)
        //    return oneConfig;

        // Cache oneConfig
        if (bXlate)
            getConfigMap(locale).put(oneConfig.getUID(), oneConfig);
        else
            rawConfigMap.put(oneConfig.getUID(), oneConfig);

        return oneConfig;
    }

    private static String _getStringFromRB(java.util.ResourceBundle rb, String key) {
        try {
            return rb.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    private static PmiModuleConfig translate(PmiModuleConfig cfg, Locale locale) {
        NLS nls = getNLS(locale, cfg.getResourceBundle(), cfg.getUID());
        if (nls != null) {
            try {
                cfg.setDescription(nls.getString(cfg.getDescription()));
            } catch (MissingResourceException mre) {
                cfg.setDescription(cfg.getDescription());
            }

            PmiDataInfo[] counterInfo = cfg.listAllData();
            for (int i = 0; i < counterInfo.length; i++) {
                try {
                    String a = nls.getString(counterInfo[i].getName(), counterInfo[i].getName());
                    counterInfo[i].setName(a);

                } catch (MissingResourceException mre) {
                    counterInfo[i].setName(counterInfo[i].getName());
                }
                try {
                    counterInfo[i].setDescription(nls.getString(counterInfo[i].getDescription(), counterInfo[i].getDescription()));
                } catch (MissingResourceException mre) {
                    counterInfo[i].setDescription(counterInfo[i].getDescription());
                }
                try {
                    counterInfo[i].setUnit(nls.getString(counterInfo[i].getUnit()));
                } catch (MissingResourceException mre) {
                    counterInfo[i].setUnit(counterInfo[i].getUnit());
                }
            }
        }
        return cfg;
    }

    // called by PmiRegistry.
    public static void translateAndCache(PmiModuleConfig cfg, Locale l) {
        PmiModuleConfig aCfg = getConfig(l, cfg.getUID());
        if (aCfg == null) {
            aCfg = cfg.copy(); // create a copy before translating

            // filter sub-module entry 
            if (aCfg != null) {
                PmiDataInfo[] data = aCfg.listData(null);
                for (int k = data.length - 1; k >= 0; k--) {

                    if (data[k].getType() == PmiConstants.TYPE_SUBMODULE)
                        aCfg.removeData(data[k]);
                }
            }
            aCfg = translate(aCfg, l); //getNLS (l, cfg.getResourceBundle()));
            getConfigMap(l).put(aCfg.getUID(), aCfg);
        }
    }

    public static PmiModuleConfig[] getAllConfigs(Locale l) {
        HashMap aMap = getConfigMap(l);
        Iterator i = aMap.values().iterator();
        PmiModuleConfig[] cfg = new PmiModuleConfig[aMap.size()];
        int k = 0;
        while (i.hasNext()) {
            cfg[k++] = (PmiModuleConfig) i.next();
        }
        return cfg;
    }

    public static void initConfig(PmiModuleConfig[] cfg, Locale l) {
        if (cfg == null)
            return;

        HashMap aMap = getConfigMap(l);
        for (int i = 0; i < cfg.length; i++)
            aMap.put(cfg[i].getUID(), cfg[i]);
    }

    private static String[] parseStatsType(String type) {
        StringTokenizer parser = new StringTokenizer(type, ",");
        String[] statTypes = new String[parser.countTokens()];
        int i = 0;
        while (parser.hasMoreTokens()) {
            statTypes[i++] = parser.nextToken();
        }
        return statTypes;
    }

    public static String getStatsType(int type) {
        switch (type) {
            case TYPE_LONG:
                return "CountStatistic";
            case TYPE_DOUBLE:
                return "DoubleStatistic";
            case TYPE_STAT:
                return "TimeStatistic";
            case TYPE_LOAD:
                return "BoundedRangeStatistic";
            case TYPE_AVGSTAT:
                return "AverageStatistic";
            case TYPE_RANGE:
                return "RangeStatistic";
            default:
                return "undefined";
        }
    }

    public static String getLevelString(int level) {
        switch (level) {
            case LEVEL_NONE:
                return LEVEL_NONE_STRING;
            case LEVEL_LOW:
                return LEVEL_LOW_STRING;
            case LEVEL_MEDIUM:
                return LEVEL_MEDIUM_STRING;
            case LEVEL_HIGH:
                return LEVEL_HIGH_STRING;
            case LEVEL_MAX:
                return LEVEL_MAX_STRING;

            default:
                return "undefined";
        }
    }

    public static void main(String[] args) {
        System.out.println(getTranslatedStatsName("root.MyStats", "com.ibm.websphere.fvt.pmi.custom.CustomStats1,com.ibm.websphere.fvt.pmi.custom.EmptyStats"));
        if (args == null || args.length < 2) {
            System.out.println("Usage: StatsConfigHelper <statsType> <r, e, t>");
            return;
        }

        debug = true;
        PmiModuleConfig c = null;
        if (args[1].equals("r"))
            c = StatsConfigHelper.getStatsConfig(args[0]);
        else if (args[1].equals("e"))
            c = StatsConfigHelper.getTranslatedStatsConfig(args[0], null);
        else if (args[1].equals("t"))
            c = StatsConfigHelper.getTranslatedStatsConfig(args[0], new Locale(args[2]));

        if (c != null)
            System.out.println(c.toString());
        else
            System.out.println("Config is null");

        if (args[1].equals("r"))
            c = StatsConfigHelper.getStatsConfig(args[0]);
        else if (args[1].equals("e"))
            c = StatsConfigHelper.getTranslatedStatsConfig(args[0], null);
        else if (args[1].equals("t"))
            c = StatsConfigHelper.getTranslatedStatsConfig(args[0], new Locale(args[2]));

        if (c != null)
            System.out.println(c.toString());
        else
            System.out.println("Config is null");

    }
}