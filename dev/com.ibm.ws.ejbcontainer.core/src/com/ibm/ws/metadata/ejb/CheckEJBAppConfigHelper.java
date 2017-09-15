/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Helper class for controlling validation of proper metadata configuration
 * for EJB-based applications. <p>
 * 
 * This class is intended to be invoked from the MetaData, EJBContainer, and
 * Injection components during metadata processing to determine when to perform
 * additional checking and/or when to log a warning, and when to case a poor
 * configuration to result in a failure. <p>
 */
public final class CheckEJBAppConfigHelper
{
    private static final String CLASS_NAME = CheckEJBAppConfigHelper.class.getName();
    private static final TraceComponent metadataTc = Tr.register(CheckEJBAppConfigHelper.class,
                                                                 "MetaData",
                                                                 "com.ibm.ws.metadata.metadata");
    private static final TraceComponent ejbTc = Tr.register(CLASS_NAME + "_ejb",
                                                            CheckEJBAppConfigHelper.class,
                                                            "EJBContainer",
                                                            "com.ibm.ejs.container.container");
    private static final TraceComponent injTc = Tr.register(CLASS_NAME + "_inj",
                                                            CheckEJBAppConfigHelper.class,
                                                            "Injection",
                                                            "com.ibm.ejs.container.container");

    private static final String svCheckEJBAppConfigName = "com.ibm.websphere.ejbcontainer.checkEJBApplicationConfiguration";

    private static boolean svIsPropertySet = System.getProperty(svCheckEJBAppConfigName) != null;
    private static boolean svCheckEJBAppConfig = svIsPropertySet ? Boolean.getBoolean(svCheckEJBAppConfigName) : false;

    private static boolean svDevelopmentMode = false;

    /**
     * Checks whether validation messages should be logged or not. <p>
     * 
     * This is determined by checking the following:
     * <ul>
     * <li> checkEJBApplicationConfiguration system property
     * <li> Metadata trace component is set to the debug or lower level
     * <li> EJBContainer trace component is set to the debug or lower level
     * <li> Injection trace component is set to the debug or lower level
     * <li> development mode server configuration
     * </ul>
     * 
     * If any of these conditions are true, then this method will return true.
     * Otherwise it will return false. <p>
     * 
     * In general, an application should be allowed to start and run as long
     * as configuration issues found are not likely to cause the application
     * to behave unexpectedly or result in data integrity issues. <p>
     * 
     * Those configuration issues that may occur additional performance
     * overhead to determine should use this method to determine if
     * they should be checked for and logged as a warning. <p>
     * 
     * @return true if the system property is set to true OR if
     *         <code>tc.isDebugEnabled()</code> is true OR development mode server
     */
    public static boolean isValidationLoggable()
    {
        if (svCheckEJBAppConfig ||
            svDevelopmentMode ||
            (TraceComponent.isAnyTracingEnabled() &&
            (metadataTc.isDebugEnabled() ||
             ejbTc.isDebugEnabled() ||
            injTc.isDebugEnabled())))
        {
            return true;
        }

        return false;
    }

    /**
     * Same as isValidationLoggable(), except the application custom property
     * setting is taken into consideration. <p>
     * 
     * Note that if the system property is set (whether true or false) it will
     * override the application custom property. Thus, the system property may
     * be used to 'turn off' this setting for applications that have it enabled
     * with the custom property. <p>
     * 
     * @param appCustomPropertySetting value of the current application custom property
     */
    // F743-33178
    public static boolean isValidationLoggable(boolean appCustomPropertySetting)
    {
        // If the app custom property is set true... return true; unless the
        // system property has been set, as the system property overrides the
        // app custom property whether true or false.
        if (appCustomPropertySetting && !svIsPropertySet) {
            return true;
        }

        // Otherwise, check development mode and trace and/or system property if set
        return isValidationLoggable();
    }

    /**
     * Checks whether more significant validation messages should
     * result in a failure or not. <p>
     * 
     * This is determined by checking the following:
     * <ul>
     * <li> checkEJBApplicationConfiguration system property
     * </ul>
     * 
     * If any of these conditions are true, then this method will return true.
     * Otherwise it will return false. <p>
     * 
     * In general, an application should be allowed to start and run as long
     * as configuration issues found are not likely to cause the application
     * to behave unexpectedly or result in data integrity issues. <p>
     * 
     * Those configuration issues that do not fall in the above categorization,
     * but are deemed more egregious should use this method to determine if
     * they should indeed result in a failure. <p>
     * 
     * @return true if the system property is set to true
     */
    public static boolean isValidationFailable()
    {
        // Turning on trace or running in development mode should NOT result
        // in additional failures.  Additional failures should only occur if the
        // customer specifically requests them.
        if (svCheckEJBAppConfig)
        {
            return true;
        }

        return false;
    }

    /**
     * Same as isValidationFailable(), except the application custom property
     * setting is taken into consideration. <p>
     * 
     * Note that if the system property is set (whether true or false) it will
     * override the application custom property. Thus, the system property may
     * be used to 'turn off' this setting for applications that have it enabled
     * with the custom property. <p>
     * 
     * @param appCustomPropertySetting value of the current application custom property
     */
    // F743-33178
    public static boolean isValidationFailable(boolean appCustomPropertySetting)
    {
        // If the system property is set, it overrides the app custom property
        // whether the system property is true or false.
        if (svIsPropertySet) {
            return svCheckEJBAppConfig;
        }

        // Otherwise, just return the app custom property setting
        return appCustomPropertySetting;
    }

    /**
     * Process specific way to indicate whether or not the current process
     * is running in development mode. 'false' is the default. <p>
     * 
     * This class does not determine whether or not the process is running in
     * development mode, as the configuration may vary by process type. For
     * example, the standard application server configures this in server.xml,
     * whereas the embeddable EJB container is considered to always run in
     * development mode. <p>
     * 
     * @param enable true indicates the process in running in development mode.
     */
    public static void setDevelopmentMode(boolean enable)
    {
        if (TraceComponent.isAnyTracingEnabled() && ejbTc.isDebugEnabled())
            Tr.debug(ejbTc, "setDevelopmentMode : " + enable);

        svDevelopmentMode = enable;
    }

    /**
     * Allows the caller to refresh the checkEJBApplicationConfiguration
     * setting from the system property, in case it has changed. <p>
     */
    public static void refreshCheckEJBAppConfigSetting()
    {
        svCheckEJBAppConfig = Boolean.getBoolean(svCheckEJBAppConfigName);

        if (TraceComponent.isAnyTracingEnabled() && ejbTc.isDebugEnabled())
            Tr.debug(ejbTc, "refreshValidationMode : " + svCheckEJBAppConfig);
    }

    private CheckEJBAppConfigHelper()
    {
        // Do not allow instances to be created
    }
}
