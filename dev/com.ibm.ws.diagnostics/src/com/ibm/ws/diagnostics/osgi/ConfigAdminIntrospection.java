/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.osgi;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.wsspi.logging.SensitiveIntrospector;

/**
 * This introspection lists the Configurations registered with ConfigAdmin
 */
public class ConfigAdminIntrospection extends SensitiveIntrospector {
    private ConfigurationAdmin configAdmin;

    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {
        if (configAdmin == this.configAdmin) {
            this.configAdmin = null;
        }
    }

    @Override
    public String getIntrospectorName() {
        return "ConfigAdminIntrospection";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspect all Configurations.";
    }

    @Override
    public void introspect(PrintWriter pw) throws IOException, InvalidSyntaxException {
        Configuration[] configurations = configAdmin.listConfigurations(null);

        if (configurations != null) {
            Arrays.sort(configurations, new Comparator<Configuration>() {

                @Override
                public int compare(Configuration arg0, Configuration arg1) {
                    if (arg0.getBundleLocation() == null) {
                        return arg1.getBundleLocation() == null ? 0 : 1;
                    }
                    if (arg1.getBundleLocation() == null) {
                        return -1;
                    }
                    return arg0.getBundleLocation().compareTo(arg1.getBundleLocation());
                }

            });

            for (Configuration config : configurations) {
                pw.println("BundleLocation: " + config.getBundleLocation());
                pw.println("PID: " + config.getPid());
                pw.println("FactoryPID: " + config.getFactoryPid());
                Dictionary<String, Object> properties = config.getProperties();
                if (properties != null) {
                    for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
                        String key = e.nextElement();
                        pw.append("  ").append(key).append(": ");

                        Object value = properties.get(key);
                        if (value != null && value.getClass().isArray()) {
                            pw.append("[");
                            for (int i = 0, length = Array.getLength(value); i < length; i++) {
                                if (i > 0) {
                                    //try to avoid really long lines in dump
                                    if (length > 10) {
                                        pw.println(",");
                                        pw.append("      ");
                                    } else {
                                        pw.append(", ");
                                    }
                                }
                                pw.append(getObscuredValue(key, Array.get(value, i)));
                            }
                            pw.append("]");
                        } else {
                            pw.append(getObscuredValue(key, value));
                        }
                        pw.println();
                    }
                }
                pw.println();
            }
        }
    }

}
