/*******************************************************************************
 * Copyright (c) 2016,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.util;

import static com.ibm.ws.test.image.util.FileUtils.normalize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ibm.ws.test.image.Timeouts;

public class ProcessRunner {
    public static final String CLASS_NAME = ProcessRunner.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }
    
    //
    
    public static class Result {
        public Result(boolean destroyed, int rc,
                List<String> stdout, StreamCopier.TerminationCondition stdoutCondition,
                List<String> stderr, StreamCopier.TerminationCondition stderrCondition) {

            this.destroyed = destroyed;
            this.rc = rc;
            
            this.stdout = stdout;
            this.stdoutCondition = stdoutCondition;
            
            this.stderr = stderr;
            this.stderrCondition = stderrCondition;
        }
        
        private final boolean destroyed;
        private final int rc;

        private final List<String> stdout;
        private final StreamCopier.TerminationCondition stdoutCondition;

        private final List<String> stderr;
        private final StreamCopier.TerminationCondition stderrCondition;

        public boolean getDestroyed() {
            return destroyed;
        }

        public int getRC() {
            return rc;
        }

        public List<String> getStdout() {
            return stdout;
        }
        
        public StreamCopier.TerminationCondition getStdoutCondition() {
            return stdoutCondition;
        }

        public List<String> getStderr() {
            return stderr;
        }

        public StreamCopier.TerminationCondition getStderrCondition() {
            return stderrCondition;
        }

        public void validate(
                int expectedRc,
                StreamCopier.TerminationCondition expectedStdoutCondition,
                StreamCopier.TerminationCondition expectedStderrCondition) throws Exception {

            boolean useDestroyed = getDestroyed();
            int useRc = getRC();
            StreamCopier.TerminationCondition useStdoutCondition = getStdoutCondition();
            StreamCopier.TerminationCondition useStderrCondition = getStderrCondition();

            log("Destroyed [ " + useDestroyed + " ]");
            log("Return code [ " + useRc + " ]");
            log("Stdout condition [ " + useStdoutCondition + " ]");
            log("Stderr condition [ " + useStderrCondition + " ]");

            for ( String stdoutLine : getStdout() ) {
                log("StdOut [ " + stdoutLine + " ]");
            }
            for ( String stderrLine : getStderr() ) {
                log("StdErr [ " + stderrLine + " ]");
            }

            if ( useRc != expectedRc ) {
                // See issue #20249
                if ( useDestroyed ) {
                    log("Ignoring return code [ " + useRc + " ] expecting [ " + expectedRc + " ]");
                } else {
                    throw new Exception("Failed; return code [ " + useRc + " ] expecting [ " + expectedRc + " ]");
                }
            }

            if ( (expectedStdoutCondition != null) &&
                 (useStdoutCondition != expectedStdoutCondition) ) {
                throw new Exception(
                        "Failed;" +
                        " stdout condition [ " + useStdoutCondition + " ]" +
                        " expecting [ " + expectedStdoutCondition + " ]");                
            }

            if ( (expectedStderrCondition != null) &&
                 (useStderrCondition != expectedStderrCondition) ) {
                throw new Exception(
                        "Failed;" +
                        " stderr condition [ " + useStderrCondition + " ]" +
                        " expecting [ " + expectedStderrCondition + " ]");                
            }
        }
    }

    //

    public static final String JVM_VENDOR_PROPERTY_NAME = "java.vendor";
    
    public static final String JVM_VENDOR;
    public static final boolean IS_IBM_JVM;
    
    static {
        String jvmVendor = System.getProperty(JVM_VENDOR_PROPERTY_NAME);
        boolean isIbmJvm = ( (jvmVendor != null) && jvmVendor.toLowerCase().contains("ibm") );
        
        log("JVM vendor [ " + JVM_VENDOR_PROPERTY_NAME + " ]:" +
                " [ " + jvmVendor + " ]:" +
                " IBM [ " + isIbmJvm + " ]");
        
        JVM_VENDOR = jvmVendor;
        IS_IBM_JVM = isIbmJvm;
    }
    
    public static final String JAVA_HOME_PATH;
    public static final String JAVA_HOME_PATH_ABS;
    public static final String JAVA_EXE_PATH_ABS;

    public static String getJavaHomePath() {
        return JAVA_HOME_PATH;
    }
    
    public static String getJavaHomePathAbs() {
        return JAVA_HOME_PATH_ABS;
    }

    static {
        String javaHome = System.getProperty("java.home");
        if ( javaHome == null ) {
            javaHome = ".";
            log("Java home property [ java.home ] is not set; using default.");
        }
        javaHome = normalize(javaHome);
        
        log("Java home [ java.home ]: [ " + javaHome + " ]");

        String javaHomeAbs = normalize( (new File(javaHome)).getAbsolutePath() );
        log("Java home (absolute) [ " + javaHomeAbs + " ]");
        
        String javaExeAbs = javaHomeAbs + "/bin/java";
        log("Java executable (absolute) [ " + javaExeAbs + " ]");
        
        JAVA_HOME_PATH = javaHome; 
        JAVA_HOME_PATH_ABS = javaHomeAbs;
        JAVA_EXE_PATH_ABS = javaExeAbs;
    }

    //
    
    public static void validateJava(String javaPath, String javaArchiveName) throws Exception {
        log("Verifying java [ " + javaPath + " ] [ " + javaArchiveName + " ]");

        String[] expected = javaArchiveName.split("-");
        String expectedJavaVersion = expected[2].split("java")[1];
        String expectedOSName = expected[3].toLowerCase();        
        String expectedOSArch = expected[4].toLowerCase();
        
        log("Expected version [ " + expectedJavaVersion + " ]");
        log("Expected operating system [ " + expectedOSName + " ]");
        log("Expected architecture [ " + expectedOSArch + " ]");

        if ( expectedOSArch.equals("x86_64") ) {
            expectedOSArch = "amd64";
            log("Adjusted expected architecture [ " + expectedOSArch + " ]");
        }
        
        String javaReleasePath = javaPath + "/java/java/release";
        File javaRelease = new File(javaReleasePath);
        if ( !javaRelease.exists() ) {
            throw new Exception("Release [ " + javaReleasePath + " ] does not exist");
        }

        try ( Reader fr = new FileReader(javaRelease);
              BufferedReader br = new BufferedReader(fr) ) {

            String javaVersion = br.readLine();
            log("Java version [ " + javaVersion + " ]");
            if ( javaVersion == null ) {
                throw new Exception("Failed to read java version from release [ " + javaReleasePath + " ]");
            } else if ( !javaVersion.contains("JAVA_VERSION") ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " java version [ " + javaVersion + " ]" +
                        " does not contain [ JAVA_VERSION ]");
            } else if ( !javaVersion.contains(expectedJavaVersion) ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " java version [ " + javaVersion + " ]" +
                        " does not contain [ " + expectedJavaVersion + " ]");
            } else {
                log("Version [ " + javaVersion + " ]");
            }

            String osName = br.readLine();
            if ( osName == null ) {
                throw new Exception("Failed to read OS name from [ " + javaReleasePath + " ]");
            } else if ( !osName.contains("OS_NAME") ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " OS name [ " + osName + " ]" +
                        " does not contain [ OS_NAME ]");
            } else if ( !osName.contains(expectedOSName) ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " OS name [ " + osName + " ]" +
                        " does not contain [ " + expectedOSName + " ]");
            } else {
                log("OS name [ " + osName + " ]");
            }

            String osVersion = br.readLine();
            log("OS version [ " + osVersion + " ]");
            
            String osArch = br.readLine();
            if ( osArch == null ) {
                throw new Exception("Failed to read OS arch from [ " + javaReleasePath + " ]");
            } else if ( !osArch.contains("OS_ARCH") ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " OS Arch [ " + osArch + " ]" +
                        " does not contain [ OS_ARCH ]");
            } else if ( !osArch.contains(expectedOSArch) ) {
                throw new Exception("Release [ " + javaReleasePath + " ]" +
                        " OS arch [ " + osArch + " ]" +
                        " does not contain [ " + expectedOSArch + " ]");
            } else {
                log("OS arch [ " + osArch + " ]");
            }
        }
    }

    //

    public static void runJar(long timeout, String... params) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(JAVA_EXE_PATH_ABS);
        command.add("-jar");
        for ( String param : params ) {
            command.add(param);
        }
        ProcessRunner.validate(command, timeout);
    }

    //

    public static Result validate(String command, long timeout) throws Exception {
        return validate(command, timeout, null);
    }
    
    public static Result validate(String command, long timeout, Consumer<ProcessBuilder> callback)
        throws Exception {

        return validate( Collections.singletonList(command), timeout, callback );
    }    

    public static Result validate(List<String> command, long timeout) throws Exception {
        return validate(command, timeout, null, null, null, null, 0, null, null, null);
    }
    
    public static Result validate(
        List<String> command,
        long timeout,
        Consumer<ProcessBuilder> callback) throws Exception {
        
        return validate(command,
                timeout,
                null, null, null, null,
                0,
                StreamCopier.TerminationCondition.COMPLETED,
                StreamCopier.TerminationCondition.COMPLETED,
                callback);
    }    
    
    public static Result validate(
            List<String> command,
            long timeout,
            Function<String, Boolean> stdoutSuccess, Function<String, Boolean> stdoutFailure,
            Function<String, Boolean> stderrSuccess, Function<String, Boolean> stderrFailure,
            int expectedRc,
            StreamCopier.TerminationCondition expectedStdoutCondition,
            StreamCopier.TerminationCondition expectedStderrCondition,
            Consumer<ProcessBuilder> callback) throws Exception {

        log("Command:");
        for ( String commandArg : command ) {
            log("[ " + commandArg + " ]");
        }

        log("Timeout: [ " + timeout + " (ns) ]");

        Result result = run(command,
                timeout,
                stdoutSuccess, stdoutFailure, stderrSuccess, stderrFailure,
                callback);
        
        result.validate(expectedRc, expectedStdoutCondition, expectedStderrCondition);

        return result;
    }

    //

    public static Result run(String command, long timeout) throws Exception {
        return run(command, timeout, null);
    }
    
    public static Result run(String command, long timeout, Consumer<ProcessBuilder> callback) throws Exception {
        return run( Collections.singletonList(command), timeout, callback );
    }    

    public static Result run(List<String> command, long timeout) throws Exception {
        return run(command, timeout, null, null, null, null, null);
    }

    public static Result run(
            List<String> command,
            long timeout,
            Consumer<ProcessBuilder> callback) throws Exception {

        return run(command, timeout, null, null, null, null, callback);
    }
    
    //

    public static Result run(
            List<String> command,
            long timeout,
            Function<String, Boolean> stdoutSuccess, Function<String, Boolean> stdoutFailure,
            Function<String, Boolean> stderrSuccess, Function<String, Boolean> stderrFailure,
            Consumer<ProcessBuilder> callback) throws Exception {

        ProcessBuilder pb = new ProcessBuilder();

        pb.command(command);

        if ( callback != null ) {
            callback.accept(pb);
        }

        List<String> stdoutCapture = new ArrayList<String>();
        List<String> stderrCapture = new ArrayList<String>();

        Process p = pb.start();

        StreamCopier osc = new StreamCopier(
                p.getInputStream(), stdoutCapture,
                timeout, stdoutSuccess, stdoutFailure );
        Thread stdoutCopier = new Thread(osc, "CaptureStdout");
        stdoutCopier.start();

        StreamCopier esc = new StreamCopier(
                p.getErrorStream(), stderrCapture,
                timeout, stderrSuccess, stderrFailure );
        Thread stderrCopier = new Thread(esc, "CaptureStderr");
        stderrCopier.start();

        while ( p.isAlive() && stdoutCopier.isAlive() && stderrCopier.isAlive() ) { 
            Thread.sleep(Timeouts.PROCESS_INTERVAL_MS);
        }

        boolean destroyed;
        if ( destroyed = p.isAlive() ) {
            log("Forcibly destroying process");
            p.destroy();
            p.waitFor(Timeouts.PROCESS_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
        if ( stdoutCopier.isAlive() ) {
            log("Interrupting capture of stdout");
            osc.recordHalted();
            stdoutCopier.interrupt();
        }
        if ( stderrCopier.isAlive() ) {
            log("Interrupting capture of stderr");
            esc.recordHalted();
            stderrCopier.interrupt();
        }

        return new Result(
                destroyed, p.exitValue(),
                stdoutCapture, osc.getTerminationCondition(),
                stderrCapture, esc.getTerminationCondition() );
    }
    
    public static class StreamCopier implements Runnable {
        public StreamCopier(InputStream input, List<String> capture, long timeout) {
            this(input, capture, timeout, null, null);
        }

        public StreamCopier(
                InputStream input, List<String> capture,
                long timeout,
                Function<String, Boolean> successCondition,
                Function<String, Boolean> failureCondition) {
            
            this.input = input;
            this.capture = capture;

            this.timeout = timeout;

            this.successCondition = successCondition;
            this.failureCondition = failureCondition;

            this.termination = TerminationCondition.RUNNING;
        }

        private final InputStream input;

        public InputStream getInput() {
            return input;
        }

        private final List<String> capture;

        protected void capture(String line) {
            if ( !line.isEmpty() ) {
                capture.add(line);
                System.out.println("Captured [ " + line + " ]");
            }
        }

        public List<String> getCapture() {
            return capture;
        }

        //

        public static enum TerminationCondition {
            RUNNING,
            COMPLETED,
            TIMED_OUT,
            IO_EXCEPTION,
            SUCCEEDED,
            FAILED,
            HALTED
        };

        private TerminationCondition termination;
        
        protected void recordTermination(TerminationCondition termination) {
            System.out.println("Recording termination [ " + termination + " ]");
            this.termination = termination;
        }
        
        public TerminationCondition getTerminationCondition() {
            return termination;
        }
        
        public boolean getTerminated() {
            return ( getTerminationCondition() != TerminationCondition.RUNNING );
        }

        protected void recordStreamEnded() {
            recordTermination(TerminationCondition.COMPLETED);
        }
        
        protected void recordIOException() {
            recordTermination(TerminationCondition.IO_EXCEPTION);
        }

        protected void recordSuccess() {
            recordTermination(TerminationCondition.SUCCEEDED);
        }

        protected void recordFailure() {
            recordTermination(TerminationCondition.FAILED);
        }

        protected void recordTimedOut() {
            recordTermination(TerminationCondition.TIMED_OUT);
        }
        
        public void recordHalted() {
            recordTermination(TerminationCondition.HALTED);
        }

        //

        private final Function<String, Boolean> successCondition;

        public Function<String, Boolean> getSuccessCondition() {
            return successCondition;
        }

        private final Function<String, Boolean> failureCondition;

        public Function<String, Boolean> getFailureCondition() {
            return failureCondition;
        }        
        
        public boolean terminate(String line) {
            Function<String, Boolean> useSuccessCondition = getSuccessCondition();
            if ( (useSuccessCondition != null) && useSuccessCondition.apply(line) ) {
                recordSuccess();
                return true;
            }
            
            Function<String, Boolean> useFailureCondition = getFailureCondition();
            if ( (useFailureCondition != null) && useFailureCondition.apply(line) ) {
                recordFailure();
                return true;
            }            

            return false;
        }

        //

        private final long timeout;
        
        public long getTimeout() {
            return timeout;
        }
        
        public boolean terminate(long durationNs) {
            return ( durationNs > getTimeout() );
        }

        //

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader( new InputStreamReader( getInput() ) );

                long startTimeNs = System.nanoTime();
                String line;
                boolean terminate = false;
                
                while ( !terminate ) {
                    if ( (line = reader.readLine()) == null ) {
                        recordStreamEnded();
                        terminate = true;
                    } else {
                        capture(line);
                        if ( terminate(line) ) {
                            terminate = true;
                        } else {
                            long endTimeNs = System.nanoTime();
                            if ( terminate(endTimeNs - startTimeNs) ) {
                                recordTimedOut();
                                terminate = true;
                            }
                        }
                    }
                }

            } catch ( IOException ex ) {
                recordIOException();
                throw new Error(ex);
            }
        }
    }
}
