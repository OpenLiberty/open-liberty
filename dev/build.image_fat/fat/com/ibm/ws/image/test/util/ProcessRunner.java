/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test.util;

import static com.ibm.ws.image.test.util.FileUtils.normalize;
import static org.junit.Assert.fail;

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
import java.util.function.Consumer;

public class ProcessRunner {
    public static final String CLASS_NAME = ProcessRunner.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }
    
    //
    
    public static class Result {
        public Result(int rc, List<String> stdout, List<String> stderr) {
            this.rc = rc;
            this.stdout = stdout;
            this.stderr = stderr;
        }
        
        private final int rc;
        private final List<String> stdout;
        private final List<String> stderr;

        public int getRC() {
            return rc;
        }

        public List<String> getStdout() {
            return stdout;
        }

        public List<String> getStderr() {
            return stderr;
        }

        public void validate() {
            int useRc = getRC();

            log("Command return code [ " + useRc + " ]");

            for ( String stdoutLine : getStdout() ) {
                log("StdOut [ " + stdoutLine + " ]");
            }
            for ( String stderrLine : getStderr() ) {
                log("StdErr [ " + stderrLine + " ]");
            }            

            if ( useRc != 0 ) {
                fail("Command failed with return code [ " + useRc + " ]");
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
        
        log("JVM vendor [ " + JVM_VENDOR_PROPERTY_NAME + " ]: [ " + jvmVendor + " ]");
        log("Is IBM JVM [ " + isIbmJvm + " ]");
        
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
            fail("Java home property [ java.home ] is not set");
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
            fail("Release [ " + javaReleasePath + " ] does not exist");
            return;
        }

        try ( Reader fr = new FileReader(javaRelease);
              BufferedReader br = new BufferedReader(fr) ) {

            String javaVersion = br.readLine();
            log("Java version [ " + javaVersion + " ]");
            if ( javaVersion == null ) {
                fail("Failed to read java version from release [ " + javaReleasePath + " ]");
                return;
            } else if ( !javaVersion.contains("JAVA_VERSION") ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " java version [ " + javaVersion + " ]" +
                        " does not contain [ JAVA_VERSION ]");
                return;
            } else if ( !javaVersion.contains(expectedJavaVersion) ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " java version [ " + javaVersion + " ]" +
                        " does not contain [ " + expectedJavaVersion + " ]");
                return;
            } else {
                log("Version [ " + javaVersion + " ]");
            }

            String osName = br.readLine();
            if ( osName == null ) {
                fail("Failed to read OS name from [ " + javaReleasePath + " ]");
                return;
            } else if ( !osName.contains("OS_NAME") ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " OS name [ " + osName + " ]" +
                        " does not contain [ OS_NAME ]");
                return;
            } else if ( !osName.contains(expectedOSName) ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " OS name [ " + osName + " ]" +
                        " does not contain [ " + expectedOSName + " ]");
            } else {
                log("OS name [ " + osName + " ]");
            }

            String osVersion = br.readLine();
            log("OS version [ " + osVersion + " ]");
            
            String osArch = br.readLine();
            if ( osArch == null ) {
                fail("Failed to read OS arch from [ " + javaReleasePath + " ]");
                return;
            } else if ( !osArch.contains("OS_ARCH") ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " OS Arch [ " + osArch + " ]" +
                        " does not contain [ OS_ARCH ]");
                return;
            } else if ( !osArch.contains(expectedOSArch) ) {
                fail("Release [ " + javaReleasePath + " ]" +
                        " OS arch [ " + osArch + " ]" +
                        " does not contain [ " + expectedOSArch + " ]");
                return;
            } else {
                log("OS arch [ " + osArch + " ]");
            }
        }
    }

    //

    public static void runJar(String... params) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(JAVA_EXE_PATH_ABS);
        command.add("-jar");
        for ( String param : params ) {
            command.add(param);
        }
        ProcessRunner.validate(command);
    }

    //

    public static Result validate(String command) throws IOException, InterruptedException {
        return validate(command, null);
    }
    
    public static Result validate(String command, Consumer<ProcessBuilder> callback)
        throws IOException, InterruptedException {

        return validate( Collections.singletonList(command), callback );
    }    

    public static Result validate(List<String> command) throws IOException, InterruptedException {
        return validate(command, null);
    }
    
    public static Result validate(List<String> command, Consumer<ProcessBuilder> callback)
        throws IOException, InterruptedException {

        log("Command:");
        for ( String commandArg : command ) {
            log("[ " + commandArg + " ]");
        }

        Result result = run(command, callback);
        result.validate();
        return result;
    }

    //

    public static Result run(String command) throws IOException, InterruptedException {
        return run(command, null);
    }
    
    public static Result run(String command, Consumer<ProcessBuilder> callback) throws IOException, InterruptedException {
        return run( Collections.singletonList(command), callback );
    }    

    public static Result run(List<String> command) throws IOException, InterruptedException {
        return run(command, null);
    }
    
    public static Result run(List<String> command, Consumer<ProcessBuilder> callback) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();

        pb.command(command);

        if ( callback != null ) {
            callback.accept(pb);
        }

        List<String> stdoutCapture = new ArrayList<String>();
        List<String> stderrCapture = new ArrayList<String>();
        
        Process p = basicRun(pb, stdoutCapture, stderrCapture);

        return new Result( p.exitValue(), stdoutCapture, stderrCapture );
    }
    
    private static Process basicRun(ProcessBuilder pb, List<String> stdoutCapture, List<String> stderrCapture) throws IOException, InterruptedException {
        Process p = pb.start();

        StreamCopier osc = new StreamCopier(p.getInputStream(), stdoutCapture);
        Thread stdoutCopier = new Thread(osc);        
        stdoutCopier.start();
        
        StreamCopier esc = new StreamCopier(p.getErrorStream(), stderrCapture);
        Thread stderrCopier = new Thread(esc);
        stderrCopier.start();

        p.waitFor(); // TODO: Need a timeout for this

        stdoutCopier.join(); // TODO: Need a timeout for this
        stderrCopier.join(); // TODO: Need a timeout for this

        return p;
    }
    
    private static class StreamCopier implements Runnable {
        public StreamCopier(InputStream input, List<String> capture) {
            this.input = input;
            this.capture = capture;
        }

        private final InputStream input;

        public InputStream getInput() {
            return input;
        }

        private final List<String> capture;

        protected void capture(String line) {
            if ( !line.isEmpty() ) {
                capture.add(line);
            }
        }

        @SuppressWarnings("unused")
        public List<String> getCapture() {
            return capture;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader( new InputStreamReader( getInput() ) );

                String line;
                while ( (line = reader.readLine()) != null ) {
                    capture(line);
                }

            } catch (IOException ex) {
                throw new Error(ex);
            }
        }
    }
}
