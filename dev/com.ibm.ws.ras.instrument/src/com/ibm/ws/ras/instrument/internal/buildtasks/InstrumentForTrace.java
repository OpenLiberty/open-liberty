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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

import com.ibm.ws.ras.instrument.internal.main.AbstractInstrumentation;
import com.ibm.ws.ras.instrument.internal.main.LibertyTracePreprocessInstrumentation;

@SuppressWarnings("restriction")
public class InstrumentForTrace extends AbstractInstrumentationTask {

    /**
     * Should FFDC be injected as well
     */
    boolean ffdc = false;

    /**
     * Should trace be injected by the task rather than just pre-processed.
     */
    boolean taskInjection = false;

    /**
     * The type of trace to inject.
     */
    String api = "liberty";

    /**
     * Indicate whether or not the task should instrument classes with FFDC.
     * FFDC is enabled by default.
     */
    public void setFfdc(boolean ffdc) {
        this.ffdc = ffdc;
    }

    /**
     * Indicate whether or not the task should perform static trace injection
     * at task execution rather than at runtime.
     */
    public void setTaskInjection(boolean taskInjection) {
        this.taskInjection = taskInjection;
    }

    /**
     * Set the type of trace API to use.
     * 
     * @param api
     *            the type of trace interface to use
     */
    public void setApi(String api) {
        api = api.trim();
        if ("liberty".equalsIgnoreCase(api)) {
            this.api = "liberty";
        } else if ("websphere".equalsIgnoreCase(api)) {
            this.api = "tr";
        } else if ("tr".equalsIgnoreCase(api)) {
            this.api = "tr";
        } else if ("jsr47".equalsIgnoreCase(api)) {
            this.api = "java-logging";
        } else if ("java".equalsIgnoreCase(api)) {
            this.api = "java-logging";
        } else if ("java.logging".equalsIgnoreCase(api)) {
            this.api = "java-logging";
        } else if ("java-logging".equalsIgnoreCase(api)) {
            this.api = "java-logging";
        } else {
            log("Invalid trace type " + api, Project.MSG_ERR);
        }
    }

    @Override
    protected Commandline getCommandline() {
        Commandline cmdl = super.getCommandline();
        if (ffdc) {
            cmdl.createArgument().setValue("--ffdc");
        }
        if (taskInjection) {
            cmdl.createArgument().setValue("--static");
        }
        if (api != null) {
            cmdl.createArgument().setValue("--" + api);
        }
        return cmdl;
    }

    @Override
    protected AbstractInstrumentation createInstrumentation() {
        return new LibertyTracePreprocessInstrumentation();
    }

}
