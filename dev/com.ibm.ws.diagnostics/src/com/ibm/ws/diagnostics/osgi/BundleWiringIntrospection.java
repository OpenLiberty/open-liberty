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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import com.ibm.wsspi.logging.Introspector;

public class BundleWiringIntrospection implements Introspector {
    BundleContext context;

    protected void activate(BundleContext context) {
        this.context = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
    }

    @Override
    public String getIntrospectorName() {
        return "BundleWiringIntrospection";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Introspect all bundles' wiring.";
    }

    @Override
    public void introspect(PrintWriter out) {
        Bundle[] bundles = context.getBundles();
        for (Bundle bundle : bundles) {
            out.println(getBundleInfo(bundle));
            introspectBundleWiringInfo(bundle, out);
            out.println();
        }
    }

    private static void introspectBundleWiringInfo(Bundle bundle, PrintWriter result) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {

            result.println("Required Wires:");
            List<BundleRequirement> reqs = wiring.getRequirements(null);
            Collections.sort(reqs, new Comparator<BundleRequirement>() {

                @Override
                public int compare(BundleRequirement arg0, BundleRequirement arg1) {
                    String attrs0 = new TreeMap<String, Object>(arg0.getAttributes()).toString();
                    String attrs1 = new TreeMap<String, Object>(arg1.getAttributes()).toString();
                    int siga = attrs0.toString().compareTo(attrs1.toString());

                    if (siga != 0) {
                        return siga;
                    }
                    String drs0 = new TreeMap<String, String>(arg0.getDirectives()).toString();
                    String drs1 = new TreeMap<String, String>(arg1.getDirectives()).toString();
                    int sigd = drs0.compareTo(drs1);

                    return sigd;
                }

            });
            if (reqs != null && !reqs.isEmpty()) {
                reqs = new ArrayList<BundleRequirement>(reqs);
                List<BundleWire> reqWires = wiring.getRequiredWires(null);
                if (reqWires != null && !reqWires.isEmpty()) {

                    for (Iterator<BundleRequirement> reqsItr = reqs.iterator(); reqsItr.hasNext();) {
                        BundleRequirement req = reqsItr.next();
                        boolean removed = false;
                        for (BundleWire reqWire : reqWires) {
                            if (reqWire.getRequirement().equals(req)) {

                                if (!removed) {
                                    // print the requirement
                                    result.println("  Requirement:");
                                    introspectBundleRequirementInfo(req, result);
                                    reqsItr.remove();
                                    removed = true;
                                }

                                // print the wired bundle
                                BundleWiring provWiring = reqWire.getProviderWiring();
                                Bundle provBundle = provWiring.getBundle();
                                result.println("    Provided by: " + getBundleInfo(provBundle));

                            }
                        }
                    }

                } else {
                    result.println("  No required wires");
                }

            }

            if (reqs != null && !reqs.isEmpty()) {
                result.println("Not Satisfied Requirements:");
                for (BundleRequirement req : reqs) {
                    result.println("  Requirement:");
                    introspectBundleRequirementInfo(req, result);
                }
            }

            result.println("Provided Wires:");
            List<BundleCapability> caps = wiring.getCapabilities(null);

            if (caps != null && !caps.isEmpty()) {
                List<BundleWire> provWires = wiring.getProvidedWires(null);
                if (provWires != null && !provWires.isEmpty()) {
                    provWires = new ArrayList<BundleWire>(provWires);
                    Collections.sort(provWires, new Comparator<BundleWire>() {

                        @Override
                        public int compare(BundleWire arg0, BundleWire arg1) {
                            return Long.signum(arg0.getRequirerWiring().getBundle().getBundleId() -
                                               arg1.getRequirerWiring().getBundle().getBundleId());
                        }

                    });

                    for (Iterator<BundleCapability> capsItr = caps.iterator(); capsItr.hasNext();) {
                        BundleCapability cap = capsItr.next();
                        boolean removed = false;
                        for (BundleWire provWire : provWires) {
                            if (provWire.getCapability().equals(cap)) {
                                if (!removed) {
                                    // print the capability
                                    result.println("  Capability:");
                                    introspectBundleCapabilityInfo(cap, result);
                                    capsItr.remove();
                                    removed = true;
                                }

                                // print the wired bundle
                                BundleWiring reqrWiring = provWire.getRequirerWiring();
                                Bundle reqrBundle = reqrWiring.getBundle();
                                result.println("    Used by: " + getBundleInfo(reqrBundle));

                            }
                        }
                    }

                } else {
                    result.println("  No provided wires");
                }

            }

            if (caps != null && !caps.isEmpty()) {
                result.println("Not Utilized Capabilities:");
                for (BundleCapability cap : caps) {
                    result.println("  Capability:");
                    introspectBundleCapabilityInfo(cap, result);
                }
            }

        } else {// means it is in the INSTALLED or UNINSTALLED state
            result.println("No wiring");
        }
    }

    private static String getBundleInfo(Bundle bundle) {
        return bundle.getBundleId() + " [" + getBundleState(bundle) + "] " + bundle.getSymbolicName() + " " + bundle.getVersion();
    }

    private static void introspectBundleCapabilityInfo(BundleCapability cap, PrintWriter result) {
        result.append("    Attributes:");
        result.println(cap.getAttributes().toString());

        result.append("    Directives:");
        result.println(cap.getDirectives().toString());
    }

    private static void introspectBundleRequirementInfo(BundleRequirement req, PrintWriter result) {
        result.append("    Attributes:");
        result.println(new TreeMap<String, Object>(req.getAttributes()).toString());

        result.append("    Directives:");
        result.println(new TreeMap<String, String>(req.getDirectives()).toString());
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
