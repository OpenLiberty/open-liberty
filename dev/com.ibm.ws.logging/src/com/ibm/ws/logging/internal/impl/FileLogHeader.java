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
package com.ibm.ws.logging.internal.impl;

import java.io.PrintStream;

import com.ibm.websphere.ras.TrConfigurator;

public class FileLogHeader {
    private final String header;
    private final boolean javaLangInstrument;
    private final boolean trace;

    public FileLogHeader(String header, boolean trace, boolean javaLangInstrument) {
        this.header = header;
        this.trace = trace;
        this.javaLangInstrument = javaLangInstrument;
    }

    public void print(PrintStream ps) {
        ps.println(BaseTraceFormatter.banner);

        ps.print(header);

        if (trace) {
            ps.println("trace.specification = " + TrConfigurator.getEffectiveTraceSpec());

            if (!javaLangInstrument) {
                ps.println("java.lang.instrument = " + javaLangInstrument);
            }
        }

        ps.println(BaseTraceFormatter.banner);
    }
}
