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
import java.util.Collection;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.LibertyVariable.Source;
import com.ibm.wsspi.logging.Introspector;

@Component(service = { Introspector.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM"
           })
public class ConfigVariableIntrospection implements Introspector {

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
        return "Lists all variables from server configuration files, command line properties, and the file system";
    }

    @Override
    public void introspect(PrintWriter writer) {
        // Put out a header before the information
        writer.println("User Config Variables");
        writer.println("---------------------");

        // Get the keys into a sorted map for display
        Collection<LibertyVariable> env = getConfigVariables();

        // Write the values
        for (LibertyVariable lv : env) {
            if (lv.getSource() == Source.XML_CONFIG) {
                writer.print(lv.getName());
                writer.print("=");
                writer.println(lv.getObscuredValue().replaceAll("\\\n", "<nl>"));
            }
        }
        writer.println("---------------------\n");

        writer.println("Service Binding Variables from " + configVariables.getServiceBindingRootDirectory());
        writer.println("---------------------");

        // Write the values
        for (LibertyVariable lv : env) {

            if (lv.getSource() == Source.SERVICE_BINDING) {
                writer.print(lv.getName());
                writer.print("=");
                writer.println(lv.getObscuredValue().replaceAll("\\\n", "<nl>"));
            }
        }
        writer.println("---------------------\n");

        writer.println("Command Line Variables");
        writer.println("---------------------");

        // Write the values
        for (LibertyVariable lv : env) {

            if (lv.getSource() == Source.COMMAND_LINE) {
                writer.print(lv.getName());
                writer.print("=");
                writer.println(lv.getObscuredValue().replaceAll("\\\n", "<nl>"));
            }
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
    private Collection<LibertyVariable> getConfigVariables() {
        return AccessController.doPrivileged(new PrivilegedAction<Collection<LibertyVariable>>() {
            @Override
            public Collection<LibertyVariable> run() {
                return configVariables.getAllLibertyVariables();
            }
        });
    }
}
