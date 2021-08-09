/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.model;

public class PackageInfo {

    private String packageName;
    private String internalPackageName;
    private boolean trivial;
    private TraceOptionsData traceOptionsData = new TraceOptionsData();

    public PackageInfo() {}

    public PackageInfo(String packageName, boolean trivial, TraceOptionsData traceOptionsData) {
        setPackageName(packageName);
        this.trivial = trivial;
        if (traceOptionsData != null) {
            this.traceOptionsData = traceOptionsData;
        }
    }

    public boolean isTrivial() {
        return trivial;
    }

    public void setTrivial(boolean trivial) {
        this.trivial = trivial;
    }

    public String getInternalPackageName() {
        return internalPackageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName.replaceAll("/", "\\.");
        this.internalPackageName = packageName.replaceAll("\\.", "/");
    }

    public TraceOptionsData getTraceOptionsData() {
        return traceOptionsData;
    }

    public void setTraceOptionsData(TraceOptionsData traceOptionsData) {
        this.traceOptionsData = traceOptionsData;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";packageName=").append(packageName);
        sb.append(",trivial=").append(trivial);
        sb.append(",traceOptionsData=").append(traceOptionsData);
        return sb.toString();
    }
}
