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
package com.ibm.websphere.pmi.stat;

import java.util.Locale;
import com.ibm.websphere.pmi.PmiModuleConfig;

/**
 * Stats helper class.
 * 
 * This class helps to enable/disable textual information (name, description, and unit) in the Stats object.
 * By default, textual information is enabled with default Locale.
 * <br>
 * The Stats textual information is initialized in the client side. This initialization happens automatically
 * when the Stats configuration files are located in the classpath.
 * <br>
 * If the Stats configuration files are not in classpath then the client side should be initialized with the Stats
 * configuration objects using <code>initTextInfo</code> method. The Stats configuration objects can be obtained
 * from the server by invoking <code>public PmiModuleConfig[] getConfigs(java.util.Locale locale)</code>
 * on the Perf MBean.
 * 
 * @ibm-api
 */

public class WSStatsHelper {
    /**
     * Initialize the client side with the Stats configuration objects.
     * 
     * @param cfg Stats configuration object from the server
     * @param locale Locale of the configuration
     */
    public static void initTextInfo(PmiModuleConfig[] cfg, Locale locale) {
        //com.ibm.ws.pmi.stat.StatsConfigHelper.initConfig(cfg, locale);
    }

    /**
     * This method is use to enable/disable the textual information in the Stats object.
     * By default textual information is enabled.
     * 
     * @param textInfoEnabled enable/disable
     */
    public static void setTextInfoEnabled(boolean textInfoEnabled) {
        com.ibm.ws.pmi.stat.StatsImpl.setEnableTextInfo(textInfoEnabled);
    }

    /**
     * This method allows translation of the textual information.
     * By default translation is enabled using the default Locale.
     * 
     * @param textInfoTranslationEnabled enable/disable
     * @param locale Locale to use for translation
     */
    public static void setTextInfoTranslationEnabled(boolean textInfoTranslationEnabled, Locale locale) {
        com.ibm.ws.pmi.stat.StatsImpl.setEnableNLS(textInfoTranslationEnabled, locale);
    }

    /**
     * Returns true if textual information is enabled.
     * 
     * @return true - enabled; false - disabled
     */
    public static boolean getTextInfoEnabled() {
        return com.ibm.ws.pmi.stat.StatsImpl.getEnableTextInfo();
    }

    /**
     * Returns true if textual information translation is enabled.
     * 
     * @return true - enabled; false - disabled
     */
    public static boolean getTextInfoTranslationEnabled() {
        return com.ibm.ws.pmi.stat.StatsImpl.getEnableNLS();
    }

    /**
     * Returns the Locale that is used for textual information translation.
     * 
     * @return Locale used for translation
     */
    public static Locale getTextInfoTranslationLocale() {
        return com.ibm.ws.pmi.stat.StatsImpl.getNLSLocale();
    }
}
