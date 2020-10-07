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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;

import com.ibm.ws.ras.instrument.internal.bci.InstrumentationException;
import com.ibm.ws.ras.instrument.internal.main.AbstractInstrumentation;

@SuppressWarnings("restriction")
public abstract class AbstractInstrumentationTask extends Task {

    protected File file = null;
    protected Vector<FileSet> filesets = new Vector<FileSet>();

    protected boolean failOnError = true;
    protected int verbosity = Project.MSG_VERBOSE;
    protected boolean debug = false;

    protected File configFile = null;

    /**
     * Issue a message for each class file that is processed.
     * 
     * @param verbose
     *            issues messages for each processed file
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            verbosity = Project.MSG_INFO;
        } else {
            verbosity = Project.MSG_VERBOSE;
        }
    }

    /**
     * Set a single file to be instrumented with trace. The specified
     * file may be a zip, jar, or class file.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Add a set of files to be instrumented with trace.
     */
    public void addFileset(FileSet set) {
        filesets.add(set);
    }

    /**
     * Indicate whether or not an error should fail the build.
     */
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Indicate whether or not the task should dump the results of class
     * instrumentation after execution.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set the configuration file to use for instrumentation options.
     */
    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    /**
     * Get the command line configuration arguments for the
     * StaticTraceInstrumentation invocations.
     * 
     * @return the ant Commandline that holds the command line arguments
     */
    protected Commandline getCommandline() {
        Commandline cmdl = new Commandline();
        if (configFile != null) {
            cmdl.createArgument().setValue("--config");
            cmdl.createArgument().setFile(configFile);
        }
        if (debug) {
            cmdl.createArgument().setValue("--debug");
        }
        return cmdl;
    }

    protected abstract AbstractInstrumentation createInstrumentation();

    /**
     * Execute the build task.
     */
    @Override
    public void execute() {
        List<File> flist = new ArrayList<File>();

        if (file != null) {
            flist.add(file);
        }

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            File dir = fs.getDir(getProject());

            String[] includedFiles = ds.getIncludedFiles();

            for (String s : Arrays.asList(includedFiles)) {
                flist.add(new File(dir, s));
            }
        }

        Commandline cmdl = getCommandline();

        for (File f : flist) {
            cmdl.createArgument().setFile(f);
        }

        AbstractInstrumentation inst;
        try {
            inst = createInstrumentation();
            if (cmdl.size() == 0) {
                return;
            }
            inst.processArguments(cmdl.getArguments());
            inst.processPackageInfo();
        } catch (Exception t) {
            getProject().log(this, "Invalid class files or jars specified " + t, Project.MSG_ERR);
            if (failOnError) {
                getProject().log("Instrumentation Failed", t, Project.MSG_ERR);
                throw new BuildException("InstrumentationFailed", t);
            } else {
                return;
            }
        }

        List<File> classFiles = inst.getClassFiles();
        List<File> jarFiles = inst.getJarFiles();

        // I don't remember why I did this
        // inst.setClassFiles(null);
        // inst.setJarFiles(null);

        boolean instrumentationErrors = false;

        for (File f : classFiles) {
            try {
                log("Instrumenting class " + f, verbosity);
                inst.instrumentClassFile(f);
            } catch (Throwable t) {
                boolean instrumentationError = t instanceof InstrumentationException;
                if (failOnError && !instrumentationError) {
                    throw new BuildException("Instrumentation of class " + f + " failed", t);
                } else {
                    String reason = "";
                    if (instrumentationError) {
                        reason = ": " + t.getMessage();
                        instrumentationErrors = true;
                    }
                    getProject().log(this, "Unable to instrument class " + f + reason, Project.MSG_WARN);
                }
            }
        }

        for (File f : jarFiles) {
            try {
                log("Instrumenting archive file " + f, verbosity);
                inst.instrumentZipFile(f);
            } catch (Throwable t) {
                if (failOnError) {
                    throw new BuildException("InstrumentationFailed", t);
                } else {
                    getProject().log(this, "Unable to instrument archive " + f, Project.MSG_WARN);
                }
            }
        }

        if (instrumentationErrors) {
            throw new BuildException("Instrumentation of classes failed");
        }
    }

}
