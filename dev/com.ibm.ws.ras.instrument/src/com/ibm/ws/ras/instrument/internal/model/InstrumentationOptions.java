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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InstrumentationOptions {

    private List<Pattern> packagesInclude = new ArrayList<Pattern>();
    private List<Pattern> packagesExclude = new ArrayList<Pattern>();
    private boolean addFFDC = false;
    private TraceType traceType = TraceType.TR;

    public InstrumentationOptions() {}

    public void addPackagesInclude(String regex) {
        Pattern pattern = Pattern.compile(regex);
        packagesInclude.add(pattern);
    }

    public void addPackagesExclude(String regex) {
        Pattern pattern = Pattern.compile(regex);
        packagesExclude.add(pattern);
    }

    public boolean isPackageIncluded(String internalPackageName) {
        if (packagesInclude.isEmpty() && packagesExclude.isEmpty()) {
            return !internalPackageName.startsWith("/java/lang");
        }
        String packageName = internalPackageName.replaceAll("/", "\\.");
        for (Pattern pi : packagesInclude) {
            if (pi.matcher(packageName).matches()) {
                for (Pattern pe : packagesExclude) {
                    if (pe.matcher(packageName).matches()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean getAddFFDC() {
        return this.addFFDC;
    }

    public void setAddFFDC(boolean addFFDC) {
        this.addFFDC = addFFDC;
    }

    public void setTraceType(String traceType) {
        traceType = traceType == null ? "" : traceType;
        if (traceType.equalsIgnoreCase("jsr47") ||
                traceType.equalsIgnoreCase("java.util.logging") || traceType.equalsIgnoreCase("java_logging")) {
            this.traceType = TraceType.JAVA_LOGGING;
        } else if (traceType.equalsIgnoreCase("tr") || traceType.equalsIgnoreCase("websphere")) {
            this.traceType = TraceType.TR;
        } else if (traceType.equalsIgnoreCase("none")) {
            this.traceType = TraceType.NONE;
        } else {
            this.traceType = TraceType.JAVA_LOGGING;
        }
    }

    public TraceType getTraceType() {
        return this.traceType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";packagesInclude=").append(packagesInclude);
        sb.append(",packagesExclude=").append(packagesExclude);
        sb.append(",addFFDC=").append(addFFDC);
        sb.append(",traceType=").append(traceType);
        return sb.toString();
    }
}
