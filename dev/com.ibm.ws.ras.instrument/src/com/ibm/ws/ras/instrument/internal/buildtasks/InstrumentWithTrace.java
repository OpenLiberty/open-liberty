/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.buildtasks;

import org.apache.tools.ant.types.Commandline;

/**
 * Build task to instrument classes and jars with trace.
 */
@SuppressWarnings("restriction")
public class InstrumentWithTrace extends InstrumentWithFFDC {

    protected String traceType = null;

    /**
     * InstrumentWithTrace task constructor.
     */
    public InstrumentWithTrace() {}

    /**
     * Set the style of trace to use when instrumenting.
     */
    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }

    protected Commandline getCommandline() {
        Commandline cmdl = super.getCommandline();
        if (traceType != null) {
            cmdl.createArgument().setValue("--" + traceType);
        }
        return cmdl;
    }
}
