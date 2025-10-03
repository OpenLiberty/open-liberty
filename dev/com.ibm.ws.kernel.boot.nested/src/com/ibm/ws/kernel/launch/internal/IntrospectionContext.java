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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
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
    private final PrintWriter consolePrintWriter = null;
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

            for (com.ibm.wsspi.logging.IntrospectableService service : getAllServiceImpls(com.ibm.wsspi.logging.IntrospectableService.class)) {
                if (filter == null || filter.contains(service.getName().toUpperCase())) {
                    introspect(introspectionDir, service, outputTarget);
                }
            }

            for (Introspector introspector : getAllServiceImpls(Introspector.class)) {
                if (filter == null || filter.contains(introspector.getIntrospectorName().toUpperCase())) {
                    introspect(introspectionDir, introspector, outputTarget);
                }
            }

        } catch (InvalidSyntaxException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occured when get IntrospectableService refs: {0}", e);
            }
        }
    }

    public void listIntrospectorsToConsole() {

        try {
            OutputStream outputStream = acquireConsoleOutputStream();
            try (PrintWriter pw = new PrintWriter(outputStream)) {

                pw.write("please select one or more of the following introspectors in a space delimited list, or select none to output all of them");
                pw.write(System.lineSeparator());

                for (com.ibm.wsspi.logging.IntrospectableService service : getAllServiceImpls(com.ibm.wsspi.logging.IntrospectableService.class)) {
                    if (service != null) {
                        String name = service.getName();
                        String desc = service.getDescription();
                        pw.write(name + " : " + desc);
                        pw.write(System.lineSeparator());
                    }
                }

                for (Introspector introspector : getAllServiceImpls(Introspector.class)) {
                    if (introspector != null) {
                        String name = introspector.getIntrospectorName();
                        String desc = introspector.getIntrospectorDescription();
                        pw.write(name + " : " + desc);
                        pw.write(System.lineSeparator());
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

    private void introspect(File introspectionDir,
                            Introspector introspectable,
                            OutputTarget outputTarget) {

        introspect(introspectionDir, introspectable.getIntrospectorName(), introspectable.getIntrospectorDescription(), introspectable, null, outputTarget);

    }

    private void introspect(File introspectionDir,
                            com.ibm.wsspi.logging.IntrospectableService introspectable,
                            OutputTarget outputTarget) {

        introspect(introspectionDir, introspectable.getName(), introspectable.getDescription(), null, introspectable, outputTarget);

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

    private <SERVICE_CLASS> List<SERVICE_CLASS> getAllServiceImpls(Class<SERVICE_CLASS> serviceClazz) throws InvalidSyntaxException {

        List<SERVICE_CLASS> serviceObjects = new ArrayList<SERVICE_CLASS>();
        Collection<ServiceReference<SERVICE_CLASS>> refs = this.systemBundleCtx.getServiceReferences(serviceClazz, null);
        if (refs != null && !refs.isEmpty()) {
            for (ServiceReference<SERVICE_CLASS> ref : refs) {
                try {
                    SERVICE_CLASS serviceObject = this.systemBundleCtx.getService(ref);
                    if (serviceObject != null) {
                        serviceObjects.add(serviceObject);
                    }
                } finally {
                    this.systemBundleCtx.ungetService(ref);
                }
            }
        }
        return serviceObjects;
    }

    public enum OutputTarget {
        file,
        console;
    }

    public OutputStream acquireConsoleOutputStream() {
        return acquireOutputStream(OutputTarget.console, null, null);
    }

    public OutputStream acquireOutputStream(OutputTarget outputTarget, File introspectionDir, String introspectionName) {
        switch (outputTarget) {
            case console:
                return new UncloseableWrapper(System.out);
            case file:
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

    private class UncloseableWrapper extends OutputStream {

        private final OutputStream deligate;

        public UncloseableWrapper(OutputStream deligate) {
            this.deligate = deligate;
        }

        @Override
        public void write(int b) throws IOException {
            deligate.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            deligate.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            deligate.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            deligate.flush();
        }

        @Override
        public void close() throws IOException {
            // No-Op. When we close the PrintWriter using it, we don't want to actually close
            // Liberty's connection to console.log!
        }

    }
}