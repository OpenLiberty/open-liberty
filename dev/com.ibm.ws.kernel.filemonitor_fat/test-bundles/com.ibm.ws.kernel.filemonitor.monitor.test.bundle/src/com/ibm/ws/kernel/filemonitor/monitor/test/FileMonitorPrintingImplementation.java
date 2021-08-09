/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package com.ibm.ws.kernel.filemonitor.monitor.test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

public class FileMonitorPrintingImplementation implements FileMonitor {
    // Not thread-safe, so keep as an instance variable
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    private static final String EOR = "<eor>";
    private final String eyecatcher;

    public FileMonitorPrintingImplementation(String eyecatcher) {
        this.eyecatcher = eyecatcher;
    }

    @Override
    public void onBaseline(Collection<File> baseline) {
        System.out.println(timestamp() + "onBaseline" + toString(baseline) + EOR);

    }

    @Override
    public void onChange(Collection<File> createdFiles,
                         Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        System.out.println(timestamp() + "onChange" + toString(createdFiles) + toString(modifiedFiles) + toString(deletedFiles) + EOR);
    }

    private String toString(Collection<File> files) {
        return Arrays.toString(files.toArray());
    }

    /**
     * @return
     */
    private String timestamp() {
        return DATE_FORMAT.format(System.currentTimeMillis()) + eyecatcher;
    }

}
