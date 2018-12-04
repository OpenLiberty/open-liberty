/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class PreMainUtil {
    public static final String FACTORY_INIT_PROPERTY = "com.ibm.serialization.validators.factory.instance";
    public static final String KEY_SERIALFILTER_AGENT_ENABLE = "com.ibm.websphere.serialfilter.enable";
    public static final String KEY_SERIALFILTER_AGENT_ACTIVE = "com.ibm.websphere.serialfilter.active";
    public static final String DEBUG_PROPERTY = "com.ibm.websphere.kernel.instrument.serialfilter.debug";
    private static final String BETA = "EARLY_ACCESS";
    private static final String PRODUCT_EDITION = "com.ibm.websphere.productEdition";
    private static final String WPI_FILE = "versions/WebSphereApplicationServer.properties";
    // Since logger is not activated while processing premain, the trace data needs to be logged by using System.out.
    public static boolean isDebugEnabled() {
        String value = System.getProperty(DEBUG_PROPERTY);
        if (value != null && "true".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }
    
    public static boolean isBeta() {
        try {
            File wasProductInfoFile = getWasProductInfoFile();
            if (isDebugEnabled()) {
                System.out.println("WAS Product Info File Location : " + wasProductInfoFile);
            }
            Properties props = new Properties();
            props.load(new FileInputStream(wasProductInfoFile));
            String edition = props.getProperty(PRODUCT_EDITION);
            if (isDebugEnabled()) {
                System.out.println("Edition : " + edition);
            }
            if (edition != null && BETA.equals(edition)) {
                return true;
            } else {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }
    
    public static boolean isEnableAgentPropertySet() {
        String enableSerialFilter = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty(KEY_SERIALFILTER_AGENT_ENABLE);
            }
        });
        boolean enabled = "true".equalsIgnoreCase(enableSerialFilter);
        if (isDebugEnabled()) {
            System.out.println("Enabling Serial Filter property is set : " + enabled);
        }

        return enabled;
    }

    private static File getWasProductInfoFile() throws URISyntaxException {
        URI installDirUri = PreMainUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        URI wasProductInfoUri = installDirUri.resolve(WPI_FILE);
        return new File(wasProductInfoUri);
    }
}
