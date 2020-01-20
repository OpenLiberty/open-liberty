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

    public static final Boolean USE_KAFKA_PRODUCER_RECORD = isBetaCodeEnabled(USE_KAFKA_PRODUCER_RECORD_PROP);

    public static boolean isBetaCodeEnabled(String customProperty) {
        boolean isBetaCodeEnabled = false;
        Boolean customPropertyOverride = getBooleanProperty(customProperty);
        if (customPropertyOverride != null) {
            isBetaCodeEnabled = customPropertyOverride;
        } else {
            isBetaCodeEnabled = isEarlyAccess();
        }
        return isBetaCodeEnabled;
    }

    public static boolean isEarlyAccess() {
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

    @Trivial
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(propertyName));
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

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
