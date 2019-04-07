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
    private final boolean isJSON;

    public FileLogHeader(String header, boolean trace, boolean javaLangInstrument, boolean isJSON) {
        this.header = header;
        this.trace = trace;
        this.javaLangInstrument = javaLangInstrument;
        this.isJSON = isJSON;
    }

    private void process(Processor processor) {
        if (!isJSON) {
            processor.println(BaseTraceFormatter.banner);
        }
        processor.print(header);

        if (trace) {
            processor.println("trace.specification = " + TrConfigurator.getEffectiveTraceSpec());

            if (!javaLangInstrument) {
                processor.println("java.lang.instrument = " + javaLangInstrument);
            }
        }

        if (!isJSON) {
            processor.println(BaseTraceFormatter.banner);
        }
    }

    public void print(PrintStream ps) {
        process(new PrintProcessor(ps));
    }

    public long length() {
        LengthProcessor lengthProcessor = new LengthProcessor();
        process(lengthProcessor);
        return lengthProcessor.getLength();
    }

    interface Processor {
        void print(String str);

        void println(String str);
    }

    class PrintProcessor implements Processor {

        final PrintStream ps;

        public PrintProcessor(final PrintStream ps) {
            this.ps = ps;
        }

        @Override
        public void println(String str) {
            ps.println(str);
        }

        @Override
        public void print(String str) {
            ps.print(str);
        }
    }

    class LengthProcessor implements Processor {
        long length;

        @Override
        public void print(String str) {
            length += str.getBytes().length;
        }

        @Override
        public void println(String str) {
            length += str.getBytes().length + LoggingConstants.nlen;
        }

        long getLength() {
            return length;
        }
    }
}
