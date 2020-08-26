/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.osgi;

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.wsspi.logging.Introspector;
import com.ibm.wsspi.logging.SensitiveIntrospector;

@Component(service = { Introspector.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM"
           })
public class ConfigVariableIntrospection extends SensitiveIntrospector {

    ConfigVariables configVariables;

    @Reference
    protected void setVariables(ConfigVariables cv) {
        this.configVariables = cv;
    }

    protected void unsetVariables(ConfigVariables cv) {
        if (cv == this.configVariables) {
            this.configVariables = null;
        }
    }

    @Override
    public String getIntrospectorName() {
        return "ConfigVariables";
    }

    @Override
    public String getIntrospectorDescription() {
        return "The User Config Variables";
    }

    @Override
    public void introspect(PrintWriter writer) {
        // Put out a header before the information
        writer.println("User Config Variables");
        writer.println("---------------------");

        // Get the keys into a sorted map for display
        Map<String, String> env = new TreeMap<String, String>(getConfigVariables());

        // Write the values
        for (Map.Entry<String, String> entry : env.entrySet()) {
            writer.print(entry.getKey());
            writer.print("=");
            writer.println(getObscuredValue(entry.getKey(), entry.getValue()).replaceAll("\\\n", "<nl>"));
        }
        writer.println("---------------------");
        writer.flush();
    }

    /**
     * Get the Config variables in a doPrivileged block.
     *
     * @return the Config variables
     */
    @Sensitive
    private Map<String, String> getConfigVariables() {
        return AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            public Map<String, String> run() {
                return configVariables.getUserDefinedVariables();
            }
        });
    }
}
