/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.utils;

import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class StAXUtils {

    private static final TraceComponent tc = Tr.register(StAXUtils.class);
    private static final EELevel eeLevel = checkEELevel();

    // The JAXB Validator API was dropped in EE10, so we can check that API to know if we're on EE10 or above.
    // Then if we check for Jakarta EE 9 version of the Validator API, we can just skip trying to load XLXP.
    private static final String JAVA_EE_JAXB_VALIDATOR = "javax.xml.bind.Validator";

    // Enum used to track which EE Level we're on.
    private static enum EELevel {
        EE8,
        EE9,
        EE10;
    }

    // XLXP's StAX implementation
    public static final String IBM_XLXP2_XML_EVENT_FACTORY = "com.ibm.xml.xlxp2.api.stax.XMLEventFactoryImpl";
    public static final String IBM_XLXP2_XML_INPUT_FACTORY = "com.ibm.xml.xlxp2.api.wssec.WSSXMLInputFactory";
    public static final String IBM_XLXP2_XML_OUTPUT_FACTORY = "com.ibm.xml.xlxp2.api.stax.XMLOutputFactoryImpl";

    // Woodstox StAX Implementation
    public static final String WOODSTOX_XML_EVENT_FACTORY = "com.ctc.wstx.stax.WstxEventFactory";
    public static final String WOODSTOX_XML_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    public static final String WOODSTOX_XML_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";

    /*
     * This method finds and returns the classloader for a given StAX provider. The specific provider changes based on which EE Level we are on:
     *
     * EE10 - Woodstox StAX Provider, defaults to JRE StAX Provider if Woodstox is disabled via configuration
     * EE9 - Default JRE StAX Provider
     * EE8 - When running on Java 8 and WLP returns XLXP StAX provider, otherwise defaults to JRE's StAX Provider
     *
     */
    public static ClassLoader getStAXProviderClassLoader() {
        ClassLoader cl;
        // We don't use XLXP with EE9 so just use the JRE's StAX provider
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "EELevel is " + eeLevel + ", loading related StAX Provider");
        }
        switch (eeLevel) {
            case EE9:
                // We don't use XLXP with EE9 so just use the JRE's StAX provider
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Jakarta EE 9 found, using the JRE's StAX provider");
                }
                cl = ClassLoader.getSystemClassLoader();
                break;
            case EE10:
                if (StaxUtils.ALLOW_INSECURE_PARSER_VAL) {
                    // Honor the CXF Property that disables the Woodstox StAX Provider from being picked up.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The System Property `org.apache.cxf.stax.allowInsecureParser` is set, using JRE's StAX Provider");
                    }
                    cl = ClassLoader.getSystemClassLoader();
                } else {
                    try {
                        // Use Woodstox StAX providers on EE10
                        Class.forName(WOODSTOX_XML_OUTPUT_FACTORY);
                        Class.forName(WOODSTOX_XML_INPUT_FACTORY);
                        Class<?> eventFactoryClass = Class.forName(WOODSTOX_XML_EVENT_FACTORY);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Jakarta EE 10 found, using Woodstox's StAX provider");
                        }
                        cl = eventFactoryClass.getClassLoader();
                    } catch (ClassNotFoundException e) {
                        // If the Woodstox StAX providers aren't found for some reason, should just fall back on JRE's StAX provider.
                        // Throw a warning though, since APIs should always be available.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Unable to load Woodstox StAX provider " + e.getMessage() + ", StAX from JRE is used");
                        }
                        cl = ClassLoader.getSystemClassLoader();
                    }

                }
                break;
            default:
                // We're on EE8 - we need to try to load XLXP's StAX provider
                // (XLXP is only present when running on JAVA 8 and WLP)
                // If not found, use the default StAX provider
                try {

                    Class.forName(IBM_XLXP2_XML_OUTPUT_FACTORY);
                    Class.forName(IBM_XLXP2_XML_INPUT_FACTORY);
                    Class<?> eventFactoryClass = Class.forName(IBM_XLXP2_XML_EVENT_FACTORY);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Java EE and XLXP found, using XLXP's StAX provider");
                    }
                    cl = eventFactoryClass.getClassLoader();
                } catch (ClassNotFoundException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to load IBM STAX XLXP2 Provider " + e.getMessage() + ", StAX from JRE is used");
                    }
                    cl = ClassLoader.getSystemClassLoader();
                }
        }
        return cl;
    }

    /*
     * This method checks the EE Level by leveraging the transformation tool. If the javax.xml.bind.Validator API is present but
     * contains jakarta, we know it's EE 9, if hasn't been transformed we know we are on EE 8. If the class isn't found at all
     * we know we are on EE 10, because this API was removed in xmlBinding-4.0. Since we expect EE 10 to throw a CNFE, this method ignores
     * the corresponding FFDC.
     */
    @FFDCIgnore(ClassNotFoundException.class)
    private static EELevel checkEELevel() {
        try {
            if (Class.forName(JAVA_EE_JAXB_VALIDATOR).getName().contains("jakarta")) {
                return EELevel.EE9;
            } else {
                return EELevel.EE8;
            }

        } catch (ClassNotFoundException e1) {
            return EELevel.EE10;
        }
    }
}
