/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.kernel.provisioning.packages.PackageIndex;

/**
 * Avoid clutter in TrConfigurator!!! Be kind and do the creation
 * of the package index in this factory.
 */
public class SafeTraceLevelIndexFactory {

    /**
     * Create the package index from the contents of the resource.
     * 
     * @param resourceName The resource name to load the trace list from.
     */
    public static PackageIndex<Integer> createPackageIndex(String resourceName) {
        PackageIndex<Integer> packageIndex = new PackageIndex<Integer>();

        BufferedReader br = null;
        try {
            br = getLibertyTraceListReader(resourceName);
            addFiltersAndValuesToIndex(br, packageIndex);
        } catch (IOException e) {
            System.err.println("Unable to load " + resourceName);
        } finally {
            tryToCloseReader(br);
        }

        packageIndex.compact();
        return packageIndex;
    }

    /*
     * Get the reader for the liberty.ras.rawtracelist.properties file.
     * Attempt to get resource as stream from the class loader,
     * otherwise try to load from file. This will also assist during testing,
     * where the resource name can be specified as a file name.
     */
    private static BufferedReader getLibertyTraceListReader(String resourceName) throws FileNotFoundException {
        InputStream traceListStream = TrConfigurator.class.getClassLoader().getResourceAsStream(resourceName);
        if (traceListStream == null) {
            traceListStream = new FileInputStream(resourceName);
        }
        return new BufferedReader(new InputStreamReader(traceListStream));
    }

    /*
     * Add the filters and values to the index.
     * The values are inserted as Integers for easy comparison in TraceComponent
     * and to avoid having to recompute each time the spec is checked.
     */
    private static void addFiltersAndValuesToIndex(BufferedReader br, PackageIndex<Integer> packageIndex) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int pos = line.indexOf('=');
            if (pos > 0) {
                String filter = line.substring(0, pos).trim();
                String value = line.substring(pos + 1).trim();
                packageIndex.add(filter, getMinLevelIndex(value));
            }
        }
    }

    /*
     * Get the minimum level as an Integer.
     */
    private static Integer getMinLevelIndex(String value) {
        Integer minLevelIndex = 0;
        if ("info".equalsIgnoreCase(value)) {
            minLevelIndex = 6;
        } else {
            Level minLevel = Level.parse(value.toUpperCase());
            for (int i = 0; i < TrLevelConstants.levels.length; i++) {
                if (minLevel.equals(TrLevelConstants.levels[i])) {
                    minLevelIndex = i;
                    break;
                }
            }
        }
        return minLevelIndex;
    }

    private static void tryToCloseReader(BufferedReader br) {
        if (br == null) {
            return;
        }
        try {
            br.close();
        } catch (IOException ioe) {
        }
    }

}
