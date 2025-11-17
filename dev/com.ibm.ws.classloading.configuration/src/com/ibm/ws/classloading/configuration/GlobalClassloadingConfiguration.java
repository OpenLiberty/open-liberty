/*******************************************************************************
 * Copyright (c) 2013, 2025 IBM Corporation and others.
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
package com.ibm.ws.classloading.configuration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

@Component(service = GlobalClassloadingConfiguration.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE,
           configurationPid = "com.ibm.ws.classloading.global", property = "service.vendor=IBM")
public class GlobalClassloadingConfiguration {
    private static String USE_JAR_URLS_KEY = "useJarUrls";
    private static String LIBRARY_PRECEDENCE_KEY = "libraryPrecedence";
    static final TraceComponent tc = Tr.register(GlobalClassloadingConfiguration.class);

    private final AtomicBoolean issuedBetaMessage = new AtomicBoolean(false);
    private volatile boolean useJarUrls = false;
    private volatile LibraryPrecedence libraryPrecedence = LibraryPrecedence.afterApp;

    @Activate
    protected void activate(Map<String, Object> properties) {
        modified(properties);
    }

    @Modified
    protected void modified(Map<String, Object> props) {
        this.useJarUrls = (Boolean) props.get(USE_JAR_URLS_KEY);
        LibraryPrecedence checkValue = LibraryPrecedence.valueOf((String) props.get(LIBRARY_PRECEDENCE_KEY));
        if (checkValue == LibraryPrecedence.beforeApp) {
            if (!ProductInfo.getBetaEdition()) {
                checkValue = LibraryPrecedence.afterApp;
                if (issuedBetaMessage.compareAndSet(false, true)) {
                    Tr.info(tc, "BETA: The attribute '" + LIBRARY_PRECEDENCE_KEY + "' can only be used with the Open Liberty BETA.");
                }
            } else {
                // Running beta exception, issue message if we haven't already issued one for this class
                if (issuedBetaMessage.compareAndSet(false, true)) {
                    Tr.info(tc, "BETA: The attribute '" + LIBRARY_PRECEDENCE_KEY + "' is being used with the value '" + checkValue + "'");
                }
            }
        }
        libraryPrecedence = checkValue;
    }

    /**
     * @return
     */
    public boolean useJarUrls() {
        return useJarUrls;
    }

    public enum LibraryPrecedence {
        beforeApp,
        afterApp
    }

    public LibraryPrecedence libraryPrecedence() {
        return libraryPrecedence;
    }
}
