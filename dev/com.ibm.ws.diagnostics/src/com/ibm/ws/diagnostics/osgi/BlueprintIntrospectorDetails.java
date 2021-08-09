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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class BlueprintIntrospectorDetails implements BlueprintListener {

    private enum BlueprintState {
        unknown,
        creating,
        created,
        destroying,
        destroyed,
        failure,
        gracePeriod,
        waiting
    };

    private enum AutoExport {
        unknown,
        Disabled,
        Interfaces,
        ClassHierarchy,
        AllClasses
    }

    private final Map<Long, BlueprintState> states = new ConcurrentHashMap<Long, BlueprintState>();

    private final ServiceRegistration<BlueprintListener> reg;
    private final BundleContext bc;

    BlueprintIntrospectorDetails(BundleContext bc) {
        this.bc = bc;
        this.reg = bc.registerService(BlueprintListener.class, this, new Hashtable<String, Object>());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.blueprint.container.BlueprintListener#blueprintEvent(org.osgi.service.blueprint.container.BlueprintEvent)
     */
    @Override
    public void blueprintEvent(BlueprintEvent event) {
        states.put(event.getBundle().getBundleId(), BlueprintState.values()[event.getType()]);
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ InvalidSyntaxException.class, IllegalStateException.class })
    public void dump(PrintWriter b) {
        try {
            ServiceReference<BlueprintContainer>[] list = null;
            try {
                list = (ServiceReference<BlueprintContainer>[]) bc.getAllServiceReferences("org.osgi.service.blueprint.container.BlueprintContainer",
                                                                                           null);
            } catch (InvalidSyntaxException e) {
                // null filter. This will never happen
                b.println("Unable to dump blueprint container details: bad service filter");
                return;
            } catch (IllegalStateException ex) {
                // This could happen, and would be normal
                b.println("Unable to dump blueprint container details: bundle context is invalid (bundle has been stopped).");
                return;
            }

            List<ServiceReference<BlueprintContainer>> containers = Arrays.asList(list);
            Collections.sort(containers, new Comparator<ServiceReference<BlueprintContainer>>() {
                @Override
                public int compare(ServiceReference<BlueprintContainer> arg0, ServiceReference<BlueprintContainer> arg1) {
                    // TODO Auto-generated method stub
                    return Long.signum(arg0.getBundle().getBundleId() - arg1.getBundle().getBundleId());
                }
            });

            // Iterate through blueprint containers...
            for (ServiceReference<BlueprintContainer> containerRef : containers) {

                b.println();
                b.append("Bundle: ").append(containerRef.getBundle().getSymbolicName())
                                .append("(").append(Long.toString(containerRef.getBundle().getBundleId()))
                                .append(") Blueprint State: ").append(String.valueOf(states.get(containerRef.getBundle().getBundleId()))).println();

                // Get the container itself
                BlueprintContainer container = bc.getService(containerRef);

                if (container != null) {
                    for (String id : container.getComponentIds()) {
                        ComponentMetadata md = container.getComponentMetadata(id);
                        if (md instanceof BeanMetadata) {
                            BeanMetadata bmd = (BeanMetadata) md;
                            b.append("  Bean: ").append(id)
                                            .append(md.getActivation() == 1 ? " (Eager)" : " (Lazy)")
                                            .append(" (").append(bmd.getScope() == null ? "singleton" : bmd.getScope()).append(")")
                                            .append(" class: ").append(bmd.getClassName()).println();
                            if (bmd.getInitMethod() != null) {
                                b.append("    Init method: ").append(bmd.getInitMethod()).println();
                            }
                            if (bmd.getDestroyMethod() != null) {
                                b.append("    Destroy method: ").append(bmd.getDestroyMethod()).println();
                            }
                            if (bmd.getFactoryMethod() != null) {
                                b.append("    Factory method: ").append(bmd.getFactoryMethod()).println();
                            }
                            if (bmd.getFactoryComponent() != null) {
                                b.append("    Factory component: ").append(String.valueOf(bmd.getFactoryComponent())).println();
                            }
                            if (!bmd.getArguments().isEmpty()) {
                                b.println("    Arguments:");
                                for (BeanArgument m : bmd.getArguments()) {
                                    b.append("      ").append(String.valueOf(m)).println();
                                }
                            }
                            if (!bmd.getProperties().isEmpty()) {
                                b.println("    Properties:");
                                for (BeanProperty m : bmd.getProperties()) {
                                    b.append("      ").append(String.valueOf(m)).println();
                                }
                            }

                        } else if (md instanceof ReferenceListMetadata) {
                            ReferenceListMetadata rlmd = (ReferenceListMetadata) md;
                            b.append("  Reference List: ").append(id).append(", ").append(rlmd.getComponentName()).println();

                        } else if (md instanceof ReferenceMetadata) {
                            ReferenceMetadata rmd = (ReferenceMetadata) md;
                            b.append("  Reference: ").append(id).append(rmd.getComponentName()).println();

                        } else if (md instanceof ServiceMetadata) {
                            ServiceMetadata smd = (ServiceMetadata) md;
                            b.append("  Service: ").append(id)
                                            .append(md.getActivation() == 1 ? " (Eager)" : " (Lazy)")
                                            .append(" Ranking: ").append(Integer.toString(smd.getRanking()))
                                            .append(" Auto-Export: ").append(String.valueOf(AutoExport.values()[smd.getAutoExport()]))
                                            .append(" Interfaces: ").append(String.valueOf(smd.getInterfaces())).println();
                            if (!smd.getServiceProperties().isEmpty()) {
                                b.println("    Service Properties:");
                                for (MapEntry entry : smd.getServiceProperties()) {
                                    b.append("      key=").append(String.valueOf(entry.getKey()))
                                                    .append(", value=").append(String.valueOf(entry.getValue())).println();
                                }
                            }

                        } else {
                            b.append("  Component: ").append(id).println();
                        }

                        if (md instanceof ServiceReferenceMetadata) {
                            ServiceReferenceMetadata srmd = (ServiceReferenceMetadata) md;
                            b.append(srmd.getAvailability() == 2 ? " (Optional)" : " (Mandatory)")
                                            .append(md.getActivation() == 1 ? " (Eager)" : " (Lazy)").println();
                            b.append("   Interface: ").append(srmd.getInterface()).println();
                            if (srmd.getComponentName() != null) {
                                b.append("    Component Name: ").append(srmd.getComponentName()).println();
                            }
                            if (srmd.getFilter() != null) {
                                b.append("    Filter: ").append(srmd.getFilter()).println();
                            }
                        }
                        List<String> deps = md.getDependsOn();
                        if (!deps.isEmpty()) {
                            b.println("    Dependencies:");
                            for (String dep : deps) {
                                b.append("      ").append(dep).println();
                            }
                        }

                    }
                } else {
                    b.println("  null container");
                }
            }
        } finally {
            // Remove registration for this blueprint listener
            reg.unregister();
        }
    }
}
