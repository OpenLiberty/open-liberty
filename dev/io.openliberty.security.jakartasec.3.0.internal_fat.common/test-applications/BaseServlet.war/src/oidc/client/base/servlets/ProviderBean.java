/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class ProviderBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private final Properties config = new Properties();

    @PostConstruct
    public void init() {

        InputStream configFile = ProviderBean.class.getResourceAsStream(Constants.PROVIDER_CONFIG_PROPERTIES);
        if (configFile != null) {
            try {
                System.out.println("Loading config from: " + Constants.PROVIDER_CONFIG_PROPERTIES);
                config.load(configFile);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load " + Constants.PROVIDER_CONFIG_PROPERTIES);
            }
        }

        //        Properties props = System.getProperties();
        //        for (Entry<Object, Object> entry : props.entrySet()) {
        //            System.out.println("ProviderBean: Property: " + entry.getKey() + " with value: " + entry.getValue());
        //        }

    }

    public String getProviderRoot() {

        System.out.println("In getProviderRoot");
        String value = "http://localhost:" + System.getProperty("bvt.prop.security_1_HTTP_default");
        if (config.containsKey(Constants.PROVIDER_BASE)) {
            value = config.getProperty(Constants.PROVIDER_BASE);
        } else {
            // not a standard value, so this value will only matter if the config is trying to use the value
            System.out.println("Must be set in " + Constants.PROVIDER_CONFIG_PROPERTIES + " before it can be used using default value of: " + value);
        }

        return value;
    }

    public String getProviderSecureRoot() {

        System.out.println("In getProviderSecureRoot");
        String value = "https://localhost:" + System.getProperty("bvt.prop.security_1_HTTP_default.secure");
        if (config.containsKey(Constants.PROVIDER_SECURE_BASE)) {
            value = config.getProperty(Constants.PROVIDER_SECURE_BASE);
        } else {
            // not a standard value, so this value will only matter if the config is trying to use the value
            System.out.println("Must be set in " + Constants.PROVIDER_CONFIG_PROPERTIES + " before it can be used using default value of: " + value);
        }

        return value;
    }

    public String getClientRoot() {

        System.out.println("In getClientRoot");
        String value = "http://localhost:" + System.getProperty("bvt.prop.security_2_HTTP_default");
        if (config.containsKey(Constants.CLIENT_BASE)) {
            value = config.getProperty(Constants.CLIENT_BASE);
        } else {
            // not a standard value, so this value will only matter if the config is trying to use the value
            System.out.println("Must be set in " + Constants.PROVIDER_CONFIG_PROPERTIES + " before it can be used using default value of: " + value);
        }

        return value;
    }

    public String getClientSecureRoot() {

        System.out.println("In getClientSecureRoot");
        String value = "https://localhost:" + System.getProperty("bvt.prop.security_2_HTTP_default.secure");
        if (config.containsKey(Constants.CLIENT_SECURE_BASE)) {
            value = config.getProperty(Constants.CLIENT_SECURE_BASE);
        } else {
            // not a standard value, so this value will only matter if the config is trying to use the value
            System.out.println("Must be set in " + Constants.PROVIDER_CONFIG_PROPERTIES + " before it can be used using default value of: " + value);
        }

        return value;
    }

    public String getProvider() {

        System.out.println("In getProvider");
        String value = "OP1";
        if (config.containsKey(Constants.PROVIDER)) {
            value = config.getProperty(Constants.PROVIDER);
        } else {
            // not a standard value, so this value will only matter if the config is trying to use the value
            System.out.println("Must be set in " + Constants.PROVIDER_CONFIG_PROPERTIES + " before it can be used using default value of: " + value);
        }

        return value;
    }

    // I don't think we'll be needing/using a setter
}
