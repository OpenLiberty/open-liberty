/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan;

public class TestOptions {

    public TestOptions(
        String title, String description,
        boolean useJandex, int scanThreads,
        boolean ignoreMissingPackages, boolean ignoreMissingInterfaces) {

        this.title = title;
        this.description = description;

        this.scanOptions = new TestOptions_Scan(useJandex, scanThreads);
        this.cacheOptions = null;

        this.ignoreMissingPackages = ignoreMissingPackages;
        this.ignoreMissingInterfaces = ignoreMissingInterfaces;
    }

    public TestOptions(
        String title, String description,
        boolean useJandex, int scanThreads,
        String storageSuffix, boolean cleanStorage, int writeThreads,
        boolean useJandexFormat, boolean useBinaryFormat,
        boolean ignoreMissingPackages, boolean ignoreMissingInterfaces,
        boolean readOnly, boolean alwaysValid) {

        this.title = title;
        this.description = description;

        this.scanOptions = new TestOptions_Scan(useJandex, scanThreads);
        this.cacheOptions = new TestOptions_Cache(
            storageSuffix, cleanStorage,
            readOnly, alwaysValid,
            writeThreads,
            useJandexFormat, useBinaryFormat);

        this.ignoreMissingPackages = ignoreMissingPackages;
        this.ignoreMissingInterfaces = ignoreMissingInterfaces;
    }

    public TestOptions(
        String title, String description,
        boolean useJandex, int scanThreads,
        String storageSuffix, boolean cleanStorage,
        boolean readOnly, boolean alwaysValid, int writeThreads,
        boolean useJandexFormat, boolean useBinaryFormat,
        boolean ignoreMissingPackages, boolean ignoreMissingInterfaces) {

        this.title = title;
        this.description = description;

        this.scanOptions = new TestOptions_Scan(useJandex, scanThreads);
        this.cacheOptions = new TestOptions_Cache(
           storageSuffix, cleanStorage,
           readOnly, alwaysValid, writeThreads,
           useJandexFormat, useBinaryFormat);

        this.ignoreMissingPackages = ignoreMissingPackages;
        this.ignoreMissingInterfaces = ignoreMissingInterfaces;
    }

    //

    public final String title;
    
    public String getTitle() {
        return title;
    }

    public final String description;

    public String getDescription() {
        return description;
    }

    //

    public final TestOptions_Scan scanOptions;

    public TestOptions_Scan getScanOptions() {
        return scanOptions;
    }

    //

    public final TestOptions_Cache cacheOptions;

    public TestOptions_Cache getCacheOptions() {
        return cacheOptions;
    }

    //

    public final boolean ignoreMissingPackages;
    
    public boolean getIgnoreMissingPackages() {
        return ignoreMissingPackages;
    }

    public final boolean ignoreMissingInterfaces;

    public boolean getIgnoreMissingInterfaces() {
        return ignoreMissingInterfaces;
    }
}
