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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.wsspi.logging.Introspector;

@SuppressWarnings("deprecation")
public class IntrospectionContext {

    private static final TraceComponent tc = Tr.register(IntrospectionContext.class);

    private final BundleContext systemBundleCtx;
    private int unnamedCount;

    IntrospectionContext(BundleContext systemBundleCtx) {
        this.systemBundleCtx = systemBundleCtx;
    }

    public void introspectAll(File outputDir) {
        introspectIntrospectors(outputDir, null);
    }

    public void introspectIntrospectors(File outputDir, List<String> introspectorsToIntrospect) {
        if (!FileUtils.createDir(outputDir)) {
            throw new IllegalStateException("introspections directory could not be created.");
        }

        try {

            for (com.ibm.wsspi.logging.IntrospectableService service : getAllServiceImpls(com.ibm.wsspi.logging.IntrospectableService.class)) {
                if (introspectorsToIntrospect == null || introspectorsToIntrospect.contains(service.getName().toUpperCase())) {
                    introspect(outputDir, service);
                }
            }

            for (Introspector introspector : getAllServiceImpls(Introspector.class)) {
                if (introspectorsToIntrospect == null || introspectorsToIntrospect.contains(introspector.getIntrospectorName().toUpperCase())) {
                    introspect(outputDir, introspector);
                }
            }

        } catch (InvalidSyntaxException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occured when get IntrospectableService refs: {0}", e);
            }
        }
    }

    public void listIntrospectorsToFile(File outputDirectory) {

        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd_HH.mm.ss");
            String timestamp = sdf.format(new Date());
            String fileName = "introspectors_list_" + timestamp;

            OutputStream outputStream = acquireOutputStream(outputDirectory, fileName);
            try (PrintWriter pw = new PrintWriter(outputStream)) {

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
                            Introspector introspectable) {

        introspect(introspectionDir, introspectable.getIntrospectorName(), introspectable.getIntrospectorDescription(), introspectable, null);

    }

    private void introspect(File introspectionDir,
                            com.ibm.wsspi.logging.IntrospectableService introspectable) {

        introspect(introspectionDir, introspectable.getName(), introspectable.getDescription(), null, introspectable);

    }

    private void introspect(File introspectionDir,
                            String introspectionName,
                            String introspectionDesc,
                            Introspector introspector,
                            com.ibm.wsspi.logging.IntrospectableService introspectable) {
        if (introspectionName == null || introspectionName.isEmpty()) {
            introspectionName = Introspector.class.getSimpleName() + '.' + unnamedCount++;
        }

        PrintWriter writerForThrowable = null;
        final OutputStream outputStream = acquireOutputStream(introspectionDir, introspectionName);

        if (outputStream != null) {
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

    public OutputStream acquireOutputStream(File introspectionDir, String introspectionName) {

        File introspectionFile = new File(introspectionDir, introspectionName + ".txt");
        try {
            FileOutputStream out = new FileOutputStream(introspectionFile);
            return out;
        } catch (FileNotFoundException e) {
            e.getCause(); // findbugs
            Tr.error(tc, "error.fileNotFound", introspectionFile);

        }
        return null;
    }

}