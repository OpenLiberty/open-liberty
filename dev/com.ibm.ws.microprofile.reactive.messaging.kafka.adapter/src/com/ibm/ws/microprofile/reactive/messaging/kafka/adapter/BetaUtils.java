/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

/**
 * At the moment this is a temporary class, only for beta ... but maybe it could be moved to com.ibm.ws.kernel.productinfo and made generic?
 */
public class BetaUtils {

    public static final String EARLY_ACCESS = "EARLY_ACCESS";
    public static final String USE_KAFKA_PRODUCER_RECORD_PROP = "use.kafka.producer.record";

    public static final boolean USE_KAFKA_PRODUCER_RECORD = isBetaCodeEnabled(USE_KAFKA_PRODUCER_RECORD_PROP);

    /**
     * Return true if beta level code should be enabled.
     * This is primarily decided based on the runtime edition; If the edition is EARLY_ACCESS then beta code should be enabled.
     * However, it is possible to override this using a given system property; If the property is set, either true or false, then it
     * takes precedence.
     *
     * @param customProperty the name of an optional system property which can be used to override
     * @return true if beta level code should be enabled. Otherwise false.
     */
    public static boolean isBetaCodeEnabled(String customProperty) {
        boolean isBetaCodeEnabled = false;
        Boolean customPropertyOverride = null;
        if (customProperty != null) {
            customPropertyOverride = getBooleanProperty(customProperty);
        }
        if (customPropertyOverride != null) {
            isBetaCodeEnabled = customPropertyOverride;
        } else {
            isBetaCodeEnabled = isEarlyAccess();
        }
        return isBetaCodeEnabled;
    }

    /**
     * Is the runtime edition EARLY_ACCESS? This method runs as the server security context and uses a J2S PrivilegedAction.
     *
     * @return true if the runtime edition is EARLY_ACCESS. Otherwise false.
     */
    public static boolean isEarlyAccess() {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> unPrivilegedIsEarlyAccess());
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Is the runtime edition EARLY_ACCESS?
     *
     * @return true if the runtime edition is EARLY_ACCESS. Otherwise false.
     */
    public static boolean unPrivilegedIsEarlyAccess() {
        boolean isEarlyAccess = false;
        try {
            final Map<String, ProductInfo> productInfos = ProductInfo.getAllProductInfo();

            for (ProductInfo info : productInfos.values()) {
                if (EARLY_ACCESS.equals(info.getEdition())) {
                    isEarlyAccess = true;
                }
            }
        } catch (Exception e) {
            //FFDC and move on ... assume not early access
        }
        return isEarlyAccess;
    }

    /**
     * Get the value of a system property. This method runs as the server security context and uses a J2S PrivilegedAction.
     *
     * @param propertyName The name of the system property
     * @return The value of the system property or null if it does not exist
     */
    @Trivial
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(propertyName));
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Get a Boolean system property using the same semantics as Boolean.valueOf().
     *
     * @param propertyName The name of the system property
     * @return true if the value of the system property equals "true" (ignoring case). false if the value is anything else. null if the system property does not exist.
     */
    public static Boolean getBooleanProperty(String propertyName) {
        String propertyText = getProperty(propertyName);

        Boolean propertyValue;
        if (propertyText == null) {
            propertyValue = null;
        } else {
            propertyValue = Boolean.valueOf(propertyText);
        }
        return propertyValue;
    }

}
