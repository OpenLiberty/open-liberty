/*******************************************************************************
 * Copyright (c) 2010, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.wsspi.logging.Introspector;

@SuppressWarnings("deprecation")
public class IntrospectionContext {

    private static final TraceComponent tc = Tr.register(IntrospectionContext.class);

    private final BundleContext systemBundleCtx;
    private final File dumpDir;
    private int unnamedCount;

    IntrospectionContext(BundleContext systemBundleCtx, File dumpDir) {
        this.systemBundleCtx = systemBundleCtx;
        this.dumpDir = dumpDir;
    }

    public void introspectAll(OutputTarget outputTarget) {
        introspectAll(outputTarget, null);
    }

    public void introspectAll(OutputTarget outputTarget, List<String> filter) {
        // create introspection dir in the dump dir which was created in the server's output directory
        File introspectionDir = null;

        if (outputTarget == OutputTarget.file) {
            introspectionDir = new File(dumpDir, BootstrapConstants.SERVER_INTROSPECTION_FOLDER_NAME);
            if (!FileUtils.createDir(introspectionDir)) {
                throw new IllegalStateException("introspections directory could not be created.");
            }
        }

        try {
            introspectIntrospectors(introspectionDir, outputTarget, filter);
            introspectIntrospectableServices(introspectionDir, outputTarget, filter);
        } catch (InvalidSyntaxException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occured when get IntrospectableService refs: {0}", e);
            }
        }
    }

    public void listIntrospectorsToConsole() {

        try {
            OutputStream outputStream = acquireOutputStream(OutputTarget.console);
            try (PrintWriter pw = new PrintWriter(outputStream)) {

                pw.write("please select one or more of the following introspectors in a space delimited list, or select none to output all of them");

                Collection<ServiceReference<Introspector>> refs = this.systemBundleCtx.getServiceReferences(Introspector.class, null);
                if (refs != null && !refs.isEmpty()) {
                    for (ServiceReference<Introspector> ref : refs) {
                        Introspector introspector = this.systemBundleCtx.getService(ref);
                        if (introspector != null) {
                            try {
                                String name = introspector.getIntrospectorName();
                                String desc = introspector.getIntrospectorDescription();
                                pw.write(name + " : " + desc);

                            } finally {
                                this.systemBundleCtx.ungetService(ref);
                            }
                        }
                    }
                }

                Collection<ServiceReference<com.ibm.wsspi.logging.IntrospectableService>> legacyRefs = systemBundleCtx.getServiceReferences(com.ibm.wsspi.logging.IntrospectableService.class,
                                                                                                                                            null);

                if (legacyRefs != null && !legacyRefs.isEmpty()) {
                    for (ServiceReference<com.ibm.wsspi.logging.IntrospectableService> ref : legacyRefs) {
                        com.ibm.wsspi.logging.IntrospectableService serv = systemBundleCtx.getService(ref);
                        if (serv != null) {
                            try {
                                String name = serv.getName();
                                String desc = serv.getDescription();
                                pw.write(name + " : " + desc);
                            } finally {
                                systemBundleCtx.ungetService(ref);
                            }
                        }
                    }
                }

                pw.flush();
            }
        } catch (InvalidSyntaxException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occured when get IntrospectableService refs: {0}", e);
            }
        }
    }

    private void introspectIntrospectors(File introspectionDir, OutputTarget outputTarget, List<String> filter) throws InvalidSyntaxException {
        Collection<ServiceReference<Introspector>> refs = this.systemBundleCtx.getServiceReferences(Introspector.class, null);
        if (refs != null && !refs.isEmpty()) {
            for (ServiceReference<Introspector> ref : refs) {
                Introspector introspector = this.systemBundleCtx.getService(ref);
                if (introspector != null) {
                    try {
                        String name = introspector.getIntrospectorName();
                        String desc = introspector.getIntrospectorDescription();
                        if (filter == null || !filter.contains(name.toUpperCase())) {
                            introspect(introspectionDir, name, desc, introspector, null,
                                       outputTarget);
                        }
                    } finally {
                        this.systemBundleCtx.ungetService(ref);
                    }
                }
            }
        }
    }

    private void introspectIntrospectableServices(File introspectionDir, OutputTarget outputTarget, List<String> filter) throws InvalidSyntaxException {
        Collection<ServiceReference<com.ibm.wsspi.logging.IntrospectableService>> legacyRefs = systemBundleCtx.getServiceReferences(com.ibm.wsspi.logging.IntrospectableService.class,
                                                                                                                                    null);

        if (legacyRefs != null && !legacyRefs.isEmpty()) {
            for (ServiceReference<com.ibm.wsspi.logging.IntrospectableService> ref : legacyRefs) {
                com.ibm.wsspi.logging.IntrospectableService serv = systemBundleCtx.getService(ref);
                if (serv != null) {
                    try {
                        String name = serv.getName();
                        String desc = serv.getDescription();
                        if (filter == null || !filter.contains(name.toUpperCase())) {
                            introspect(introspectionDir, name, desc, null, serv,
                                       outputTarget);
                        }
                    } finally {
                        systemBundleCtx.ungetService(ref);
                    }
                }
            }
        }
    }

    private void introspect(File introspectionDir,
                            String introspectionName,
                            String introspectionDesc,
                            Introspector introspector,
                            com.ibm.wsspi.logging.IntrospectableService introspectable,
                            OutputTarget outputTarget) {
        if (introspectionName == null || introspectionName.isEmpty()) {
            introspectionName = Introspector.class.getSimpleName() + '.' + unnamedCount++;
        }

        PrintWriter writerForThrowable = null;
        final OutputStream outputStream = acquireOutputStream(outputTarget, introspectionDir, introspectionName);

        try (PrintWriter pw = new PrintWriter(outputStream)) {
            writerForThrowable = pw;

            // write header
            if (introspectionDesc != null && !introspectionDesc.isEmpty()) {
                pw.println("The description of this introspector:");
                pw.println(introspectionDesc);
                pw.println();
                pw.flush();
            }

            // write body
            if (introspectable != null) {
                introspectable.introspect(outputStream);
            } else {
                introspector.introspect(pw);
            }
        } catch (Throwable t) {
            Object introspectionFile = null;
            Tr.warning(tc, "warn.unableWriteFile", introspectionFile, t.getMessage());
            if (writerForThrowable != null) {
                t.printStackTrace(writerForThrowable);
            }
        }
    }

    public enum OutputTarget {
        file,
        console;
    }

    public OutputStream acquireOutputStream(OutputTarget target) {
        return acquireOutputStream(target, null, null);
    }

    public OutputStream acquireOutputStream(OutputTarget outputTarget, File introspectionDir, String introspectionName) {
        switch (outputTarget) {
            case file:
                return System.out;
            case console:
                File introspectionFile = new File(introspectionDir, introspectionName + ".txt");
                try {
                    FileOutputStream out = new FileOutputStream(introspectionFile);
                    return out;
                } catch (FileNotFoundException e) {
                    e.getCause(); // findbugs
                    Tr.error(tc, "error.fileNotFound", introspectionFile);
                }
            default:
                throw new IllegalArgumentException("A destination for introspection output is required");
        }
    }
}