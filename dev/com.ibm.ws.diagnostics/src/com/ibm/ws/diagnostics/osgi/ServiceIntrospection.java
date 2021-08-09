/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.logging.Introspector;

public class ServiceIntrospection implements Introspector {
    BundleContext context;

    protected void activate(BundleContext context) {
        this.context = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
    }

    @Override
    public String getIntrospectorName() {
        return "ServiceIntrospection";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspect all services' state.";
    }

    @Override
    public void introspect(PrintWriter out) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            out.append(Long.toString(bundle.getBundleId())).append(" [").append(getBundleState(bundle)).append("] ")
                            .append(bundle.getSymbolicName()).append(" ").append(String.valueOf(bundle.getVersion())).println();
            out.println("Registered Services:");
            writeServiceReferences(out, bundle.getRegisteredServices(), true);
            out.println("Services in Use:");
            writeServiceReferences(out, bundle.getServicesInUse(), false);
            out.println();
        }
    }

    private void writeServiceReferences(PrintWriter out, ServiceReference<?>[] services, boolean showUses) {
        if (services != null) {
            for (ServiceReference<?> sr : services) {
                String[] objectClasses = (String[]) sr.getProperty(Constants.OBJECTCLASS);
                out.append("  {");
                String separator = "";
                for (String objectClass : objectClasses) {
                    out.append(separator).append(objectClass);
                    separator = ", ";
                }
                separator = "";
                out.append("} = {");
                for (String key : sr.getPropertyKeys()) {
                    if (!Constants.OBJECTCLASS.equals(key)) {
                        out.append(separator).append(key).append("=");
                        Object property = sr.getProperty(key);
                        if (property == null || !property.getClass().isArray()) {
                            out.append(String.valueOf(property));
                        } else {
                            out.append("[");
                            for (int i = 0, length = Array.getLength(property); i < length; i++) {
                                if (i > 0) {
                                    //try to avoid really long lines in dump
                                    if (length > 10) {
                                        out.println(",");
                                        out.append("      ");
                                    } else {
                                        out.append(", ");
                                    }
                                }
                                out.append(String.valueOf(Array.get(property, i)));
                            }
                            out.append("]");
                        }
                        separator = ", ";
                    }
                }
                out.println("}");

                if (showUses) {
                    out.println("    Used by:");
                    Bundle[] usingBundles = sr.getUsingBundles();
                    if (usingBundles != null) {
                        Arrays.sort(usingBundles, new Comparator<Bundle>() {
                            @Override
                            public int compare(Bundle arg0, Bundle arg1) {
                                return Long.signum(arg0.getBundleId() - arg1.getBundleId());
                            }
                        });

                        for (Bundle b : usingBundles) {
                            out.append("      ").append(Long.toString(b.getBundleId()))
                                            .append(" ").append(b.getSymbolicName())
                                            .append(" ").append(String.valueOf(b.getVersion()))
                                            .println();
                        }
                    }
                }
            }
        }
    }

    private static String getBundleState(Bundle bundle) {
        int state = bundle.getState();
        switch (state) {
            case Bundle.UNINSTALLED:
                return "Uninstalled";
            case Bundle.INSTALLED:
                return "Installed";
            case Bundle.RESOLVED:
                return "Resolved";
            case Bundle.STOPPING:
                return "Stopping";
            case Bundle.STARTING:
                return "Starting";
            case Bundle.ACTIVE:
                return "Active";
            default: //should not happen
                return "Unknown";
        }
    }
}
