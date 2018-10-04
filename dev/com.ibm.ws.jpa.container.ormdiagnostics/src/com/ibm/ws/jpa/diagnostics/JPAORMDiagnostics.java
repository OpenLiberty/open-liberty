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

import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScanner;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerException;
import com.ibm.ws.jpa.diagnostics.puscanner.PersistenceUnitScannerResults;

/**
 *
 */
public class JPAORMDiagnostics {
    public static PersistenceUnitScannerResults performJPAORMDiagnosticsForIntrospector(List<PersistenceUnitInfo> puiList, HashMap<URL, String> pxmlMap, PrintWriter out) {
        if (puiList == null || pxmlMap == null || out == null || puiList.isEmpty() || pxmlMap.isEmpty()) {
            return null;
        }

        try {
            PersistenceUnitScannerResults results = PersistenceUnitScanner.scan(puiList, pxmlMap);
            results.printReport(out);
            results.generateORMDump(out);
            return results;
        } catch (PersistenceUnitScannerException e) {
            FFDCFilter.processException(e, JPAORMDiagnostics.class.getName() + ".performJPAORMDiagnostics", "64");
            return null;
        }
    }

    public static PersistenceUnitScannerResults performJPAORMDiagnosticsForTrace(List<PersistenceUnitInfo> puiList, HashMap<URL, String> pxmlMap) {
        if (puiList == null || pxmlMap == null || puiList.isEmpty() || pxmlMap.isEmpty()) {
            return null;
        }

        try {
            PersistenceUnitScannerResults results = PersistenceUnitScanner.scan(puiList, pxmlMap);
            return results;
        } catch (PersistenceUnitScannerException e) {
            FFDCFilter.processException(e, JPAORMDiagnostics.class.getName() + ".performJPAORMDiagnostics", "64");
            return null;
        }
    }

}
