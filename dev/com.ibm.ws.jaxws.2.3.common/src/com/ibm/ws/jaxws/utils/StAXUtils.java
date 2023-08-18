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

    // The JAXB Validator API was dropped in EE10, so we can check that API to know if we're on EE10 or above
    private static Boolean isEE10 = false;
    private static final String JAVA_EE_JAXB_VALIDATOR = "javax.xml.bind.Validator";
    private static final String JAKARTA_EE9_JAXB_VALIDATOR = "jakarta.xml.bind.Validator";

    // Since we are already checking for the Jakarta EE 9 version of the Validator API, we should just skip trying to load XLXP.
    private static Boolean isEE9 = false;
    private static Boolean isEE8 = false;

    // XLXP's StAX implementation
    public static final String IBM_XLXP2_XML_EVENT_FACTORY = "com.ibm.xml.xlxp2.api.stax.XMLEventFactoryImpl";
    public static final String IBM_XLXP2_XML_INPUT_FACTORY = "com.ibm.xml.xlxp2.api.wssec.WSSXMLInputFactory";
    public static final String IBM_XLXP2_XML_OUTPUT_FACTORY = "com.ibm.xml.xlxp2.api.stax.XMLOutputFactoryImpl";

    // Woodstox StAX Implementation
    public static final String WOODSTOX_XML_EVENT_FACTORY = "com.ctc.wstx.stax.WstxEventFactory";
    public static final String WOODSTOX_XML_INPUT_FACTORY = "com.ctc.wstx.stax.WstxInputFactory";
    public static final String WOODSTOX_XML_OUTPUT_FACTORY = "com.ctc.wstx.stax.WstxOutputFactory";

    @FFDCIgnore(ClassNotFoundException.class)
    public static ClassLoader getStAXProviderClassLoader() {
        // Only need to check EE level once, so skip EE level check if all values are still false.
        if (isEE10 == false && isEE9 == false && isEE8 == false) {
            // Need to know if we're running on EE 10 so we can use Woodstox StAX impl if true
            checkForEELevel();
        }
        if (isEE10) {
            if (StaxUtils.ALLOW_INSECURE_PARSER_VAL) {
                // Honor the CXF Property that disables the Woodstox StAX Provider from being picked up.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The System Property `org.apache.cxf.stax.allowInsecureParser` is set, using JRE's StAX Provider");
                }
                return ClassLoader.getSystemClassLoader();
            } else {
                try {
                    // Use Woodstox StAX providers on EE10
                    Class.forName(WOODSTOX_XML_OUTPUT_FACTORY);
                    Class.forName(WOODSTOX_XML_INPUT_FACTORY);
                    Class<?> eventFactoryClass = Class.forName(WOODSTOX_XML_EVENT_FACTORY);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Jakarta EE 10 found, using Woodstox's StAX provider");
                    }
                    return eventFactoryClass.getClassLoader();
                } catch (ClassNotFoundException e) {
                    // If the Woodstox StAX providers aren't found for some reason, should just fall back on JRE's StAX provider.
                    // Throw a warning though, since APIs should always be available. 
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to load Woodstox StAX provider " + e.getMessage() + ", StAX from JRE is used");
                    }
                    return ClassLoader.getSystemClassLoader();
                }

            }

        } else if (isEE9) {
            // We don't use XLXP with EE9 so just use the JRE's StAX provider
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Jakarta EE 9 found, using the JRE's StAX provider");
            }
            return ClassLoader.getSystemClassLoader();
        } else {
            // We're not on a Jakarta EE platform, so we need to try to load XLXP's StAX provider
            try {

                Class.forName(IBM_XLXP2_XML_OUTPUT_FACTORY);
                Class.forName(IBM_XLXP2_XML_INPUT_FACTORY);
                Class<?> eventFactoryClass = Class.forName(IBM_XLXP2_XML_EVENT_FACTORY);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Java EE and XLXP found, using XLXP's StAX provider");
                }
                return eventFactoryClass.getClassLoader();
            } catch (ClassNotFoundException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to load IBM STAX XLXP2 Provider " + e.getMessage() + ", StAX from JRE is used");
                }
                return ClassLoader.getSystemClassLoader();
            }
        }
    }

    /**
     * @return
     */
    @FFDCIgnore(ClassNotFoundException.class)
    private static void checkForEELevel() {
        try {
            Class.forName(JAKARTA_EE9_JAXB_VALIDATOR);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Jakarta EE 9 version of JAXB's Validator API found, isEE9 = true");
            }
            isEE9 = true;
        } catch (ClassNotFoundException e1) {

            try {
                // Check for JAVA EE version of JAXB's Validator API
                Class.forName(JAVA_EE_JAXB_VALIDATOR);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Java EE version of JAXB's Validator API found, isEE8 = true");
                }
                isEE8 = true;
            } catch (ClassNotFoundException e) {
                // Wasn't Jakarta EE 9, so we know it's EE10 and above. 
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No JAXB Validator API found, isEE10 = true");
                }
                isEE10 = true;
            }

        }

    }
}
