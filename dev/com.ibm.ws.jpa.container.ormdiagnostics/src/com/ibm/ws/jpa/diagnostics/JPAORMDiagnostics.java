/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.class_scanner.ano.EntityMappingsScannerResults;
import com.ibm.ws.jpa.diagnostics.ormparser.EntityMappingsDefinition;
import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntityMappings;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScanner;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedData;
import com.ibm.ws.jpa.diagnostics.utils.encapsulation.EncapsulatedDataGroup;

/**
 *
 */
public class JPAORMDiagnostics {
    private static final TraceComponent tc = Tr.register(JPAORMDiagnostics.class,
                                                         "JPAORM",
                                                         "com.ibm.ws.jpa.jpa");

    // Check if including JPA ORM in the Liberty Dump has been enabled.
    private static boolean jpaDumpEnabled = AccessController.doPrivileged(
                                                                          new PrivilegedAction<Boolean>() {
                                                                              @Override
                                                                              public Boolean run() {
                                                                                  return Boolean.getBoolean("com.ibm.websphere.persistence.enablejpadump");
                                                                              }
                                                                          });

    public static void writeJPAORMDiagnostics(PersistenceUnitInfo pui, InputStream pxmlIS, PrintWriter out) {
        if (jpaDumpEnabled == false || pui == null || out == null) {
            return;
        }

        generateJPAORMDiagnostics(pui, pxmlIS, out);
    }

    public static void writeJPAORMDiagnostics(PersistenceUnitInfo pui, InputStream pxmlIS) {
        if (pui == null || !(tc.isAnyTracingEnabled() && tc.isDebugEnabled())) {
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter pw = new PrintWriter(baos);

        pw.println("##### BEGIN JPA ORM Diagnostics");
        generateJPAORMDiagnostics(pui, pxmlIS, pw);
        pw.println("##### END JPA ORM Diagnostics");

        Tr.debug(tc, "JPAORMDiagnostics Dump", baos.toString());
    }

    private static void generateJPAORMDiagnostics(PersistenceUnitInfo pui, InputStream pxmlIS, PrintWriter out) {
        try {
            if (pui == null) { //  || !(tc.isAnyTracingEnabled() && tc.isDebugEnabled())) {
                return;
            }

            final PersistenceUnitScannerResults pusr = PersistenceUnitScanner.scanPersistenceUnit(pui);
            final String puName = pusr.getPersistenceUnitName();
            final List<EntityMappingsScannerResults> clsScanResultsList = pusr.getClassScannerResults();
            final List<EntityMappingsDefinition> entityMappingDefList = pusr.getEntityMappingsDefinitionsList();

            int totalClassesScanned = 0;
            if (clsScanResultsList != null && clsScanResultsList.size() > 0) {
                for (EntityMappingsScannerResults emsr : clsScanResultsList) {
                    if (emsr.getCit() != null) {
                        totalClassesScanned += emsr.getCit().getClassInfo().size();
                    }
                }
            }
            out.println("Total Classes Included in Analysis: " + totalClassesScanned);
            out.print("Entity Mappings Found ");
            if (entityMappingDefList != null && entityMappingDefList.size() > 0) {
                out.print("(" + entityMappingDefList.size() + ") :");
                boolean first = true;
                for (EntityMappingsDefinition emd : entityMappingDefList) {
                    if (first) {
                        first = false;
                    } else {
                        out.print(", ");
                    }
                    out.print(emd.getSource());
                }
            } else {
                out.print(": (none)");
            }
            out.println();

            // Generate ORM Dump
            EncapsulatedDataGroup edg = EncapsulatedDataGroup.createEncapsulatedDataGroup("ORMDiagnostics", "ORMDiagnostics");
            int id = 0;

            // Set Properties
            edg.setProperty("execution.environment", "WebSphere Liberty");
            edg.setProperty("Persistence Unit Name", puName);
            edg.setProperty("Persistence Unit Root", pui.getPersistenceUnitRootUrl().toString());
            edg.setProperty("Persistence Schema Version", pui.getPersistenceXMLSchemaVersion());

            List<URL> jpaFileURLList = pui.getJarFileUrls();
            int jarUrlCount = 0;
            if (jpaFileURLList != null && jpaFileURLList.size() > 0) {
                for (URL url : jpaFileURLList) {
                    edg.setProperty("jar_file_" + ++jarUrlCount, url.toString());
                }
            }

            edg.putDataItem(EncapsulatedData.createEncapsulatedData("persistence.xml", Integer.toString(id++), readInputStream(pxmlIS)));

            EncapsulatedDataGroup edgClassScanner = EncapsulatedDataGroup.createEncapsulatedDataGroup("ClassScanner", "ClassScanner");
            edg.putDataSubGroup(edgClassScanner);
            for (EntityMappingsScannerResults emsr : clsScanResultsList) {
                byte[] data = emsr.produceXML();
                String name = emsr.getTargetArchive().toString();
                String idStr = Integer.toString(id++);
                EncapsulatedData ed = EncapsulatedData.createEncapsulatedData(name, idStr, data);
                edgClassScanner.putDataItem(ed);
            }

            EncapsulatedDataGroup edgEntityMappings = EncapsulatedDataGroup.createEncapsulatedDataGroup("EntityMappings", "EntityMappings");
            edg.putDataSubGroup(edgEntityMappings);
            for (EntityMappingsDefinition emd : entityMappingDefList) {
                IEntityMappings em = emd.getEntityMappings();
                String name = emd.getSource().toString();
                String idStr = Integer.toString(id++);
                byte[] ormXmlData = readInputStream(emd.getSource().openStream());
                EncapsulatedData ed = EncapsulatedData.createEncapsulatedData(name, idStr, ormXmlData);
                edgEntityMappings.putDataItem(ed);
            }

            // Analysis complete, generate the report
            out.println();
            out.println("### Persistence Unit, ORM Mapping File, JPA Entity Class Signature Dump ###");
            edg.write(out);
        } catch (Throwable t) {
            // An Exception thrown by the diagnostic tool must not be permitted to interrupt application start.
            // So log the Exception in FFDC and return.
            FFDCFilter.processException(t, JPAORMDiagnostics.class.getName() + ".generateJPAORMDiagnostics", "120");
        }
    }

    private static byte[] readInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = -1;

        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        is.close();

        return baos.toByteArray();
    }
}
