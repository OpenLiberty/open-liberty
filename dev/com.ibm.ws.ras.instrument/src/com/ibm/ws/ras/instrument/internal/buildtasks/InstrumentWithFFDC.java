/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

import com.ibm.ws.ras.instrument.internal.main.AbstractInstrumentation;
import com.ibm.ws.ras.instrument.internal.main.StaticTraceInstrumentation;

@SuppressWarnings("restriction")
public class InstrumentWithFFDC extends AbstractInstrumentationTask {

    protected boolean ffdc = true;

    @Override
    protected Commandline getCommandline() {
        Commandline cmdl = super.getCommandline();
        if (ffdc) {
            cmdl.createArgument().setValue("--ffdc");
        }
        cmdl.createArgument().setValue("--none");
        return cmdl;
    }

    /**
     * Indicate whether or not the task should instrument classes with FFDC.
     * FFDC is enabled by default.
     */
    public void setFfdc(boolean ffdc) {
        this.ffdc = ffdc;
    }

    @Override
    protected AbstractInstrumentation createInstrumentation() {
        return new StaticTraceInstrumentation();
    }

}
