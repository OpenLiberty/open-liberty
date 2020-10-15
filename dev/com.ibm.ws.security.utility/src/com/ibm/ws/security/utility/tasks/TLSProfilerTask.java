/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.tasks;

import java.io.PrintStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import com.ibm.ws.security.utility.SecurityUtilityReturnCodes;
import com.ibm.ws.security.utility.utils.ConsoleWrapper;

/**
 * Main class for password encryption utility.
 * Not bundled with the core runtime jars by design.
 */
public class TLSProfilerTask extends BaseCommandTask {
    private static final String ARG_HOST = "--host";
    private static final String ARG_PORT = "--port";
    private static final String ARG_V = "--v";
    private static final String ARG_VERBOSE = "--verbose";
    private static final List<String> ARG_TABLE = Arrays.asList(ARG_HOST, ARG_PORT, ARG_V, ARG_VERBOSE);
    private static List<String> resultLabel = new ArrayList<String>();

    public TLSProfilerTask(String scriptName) {
        super(scriptName);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "tlsProfiler";
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("tlsProfiler.desc", "tlsProfiler.usage.options",
                           "tlsProfiler.required-key.", "tlsProfiler.required-desc.",
                           "tlsProfiler.option-key.", "tlsProfiler.option-desc.",
                           null, null,
                           scriptName);
    }

    @Override
    public String getTaskDescription() {
        return getOption("tlsProfiler.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {

        validateArgumentList(args, Arrays.asList(new String[] { ARG_V, ARG_VERBOSE }));
        boolean verbose = checkVerboseArgs(args);
        String host = getArgumentValue(ARG_HOST, args, null);
        int port = Integer.parseInt(getArgumentValue(ARG_PORT, args, null));

        List<String> disabledList = new ArrayList<String>();

        if (Security.getProperty("jdk.tls.disabledAlgorithms") != null) {
            disabledList = Arrays.asList((Security.getProperty("jdk.tls.disabledAlgorithms")).split(","));
        }

        Security.setProperty("jdk.tls.disabledAlgorithms", "");
        Security.setProperty("jdk.certpath.disabledAlgorithms", "");

        List<TlsProfilerResult> results = new ArrayList<TlsProfilerResult>();
        List<String> safeProtocols = new ArrayList<String>();
        List<String> unsafeProtocols = new ArrayList<String>();

        //grab the protocols and then remove SSLv2 stuff, because on Oracle it's included but doesn't actually work
        List<String> protocols = new ArrayList<String>();

        for (String protocol : Arrays.asList(SSLContext.getDefault().getDefaultSSLParameters().getProtocols())) {
            if (protocol.contains(("SSLv3")) || protocol.contains("TLS"))
                protocols.add(protocol);
        }

        //loop through each of the protocols
        for (String protocol : protocols) {

            SSLContext sc = SSLContext.getInstance(protocol);
            sc.init(null, null, null);
            String[] ciphersuites = sc.getSupportedSSLParameters().getCipherSuites();
            //loop through each of the ciphersuites available for the given protocol to the runtime.
            for (String ciphersuite : ciphersuites) {
                TlsProfilerResult result = new TlsProfilerResult();
                for (String disabledString : disabledList) {
                    if (protocol.contains(disabledString)) {
                        result.setVulnerable(true);
                    }
                }
                if (result.isVulnerable() && !unsafeProtocols.contains(protocol)) {
                    unsafeProtocols.add(protocol);
                } else if (!result.isVulnerable() && !safeProtocols.contains(protocol)) {
                    safeProtocols.add(protocol);
                }
                result.setProtocol(protocol);
                result.setCiphersuite(ciphersuite);
                for (String disabledString : disabledList) {
                    if (ciphersuite.contains(disabledString)) {
                        result.setVulnerable(true);
                    }
                }
                SSLParameters sp = sc.getDefaultSSLParameters();
                sp.setCipherSuites(new String[] { ciphersuite });
                SSLSocket socket = (SSLSocket) sc.getSocketFactory().createSocket(host, port);
                socket.setSSLParameters(sp);
                try {
                    socket.startHandshake();
                    if (protocol.equals(socket.getSession().getProtocol()) && ciphersuite.equals(socket.getSession().getCipherSuite())) {
                        result.setSuccessful(true);
                        results.add(result);
                    } else {
                        results.add(result);
                    }
                } catch (Exception e) {
                    results.add(result);
                    //e.printStackTrace();
                }
                socket.close();
            }
        }

        boolean headerPrinted = false;

        for (String protocol : safeProtocols) {
            String resultString = new String();
            for (TlsProfilerResult result : results) {
                if (result.getProtocol().equals(protocol) && result.isSuccessful()) {
                    resultString += result.getCiphersuite() + " ";
                }
            }
            if (!resultString.isEmpty()) {
                if (!headerPrinted)
                    resultLabel.add("Successful handshakes to the target host and port were made with the following recommended protocol and cipher suites:\n");
                headerPrinted = true;
                resultLabel.add(protocol + ": " + resultString + "\n");
            }
        }

        if (verbose) {
            headerPrinted = false;

            for (String protocol : safeProtocols) {
                String resultString = new String();
                for (TlsProfilerResult result : results) {
                    if (result.getProtocol().equals(protocol) && !result.isSuccessful()) {
                        resultString += result.getCiphersuite() + " ";
                    }
                }
                if (!resultString.isEmpty()) {
                    if (!headerPrinted)
                        resultLabel.add("Unsuccessful handshakes to the target host and port were made with the following protocol and cipher suites:\n");
                    headerPrinted = true;
                    resultLabel.add(protocol + ": " + resultString + "\n");
                }
            }

            headerPrinted = false;

            for (String protocol : unsafeProtocols) {
                String resultString = new String();
                for (TlsProfilerResult result : results) {
                    if (result.getProtocol().equals(protocol) && result.isSuccessful()) {
                        resultString += result.getCiphersuite() + " ";
                    }
                }
                if (!resultString.isEmpty()) {
                    if (!headerPrinted)
                        resultLabel.add("\nWARNING: The following output was generated using protocols and cipher suites which have known security vulnerabilities.\n"
                                        + "This diagnostic tool's runtime does not expose any data, and this result does not indicate a problem.\n"
                                        + "The target host and port supports the following protocols and cipher suites with known security vulnerabilities.\n"
                                        + "Enabling any of the following protocols and cipher suites to connect to the target host and port can result in a breach of security.\n");
                    headerPrinted = true;
                    resultLabel.add(protocol + ": " + resultString + "\n");
                }
            }

            headerPrinted = false;
            for (String protocol : unsafeProtocols) {
                String resultString = new String();
                for (TlsProfilerResult result : results) {
                    if (result.getProtocol().equals(protocol) && !result.isSuccessful()) {
                        resultString += result.getCiphersuite() + " ";
                    }
                }
                if (!resultString.isEmpty()) {
                    if (!headerPrinted) {
                        resultLabel.add("\nWARNING: The following output was generated using protocols and/or cipher suites which have known security vulnerabilities.\n"
                                        + "This diagnostic tool's runtime does not expose any data, and this result does not indicate a problem.\n"
                                        + "However, it should be noted that the target host and port does not support the following protocols and cipher suites which have known security vulnerabilites.\n"
                                        + "Enabling any of the following protocols and cipher suites in order to connect to the target host and port could result in a breach of security.\n");
                    }
                    headerPrinted = true;
                    resultLabel.add(protocol + ": " + resultString + "\n");
                }
            }
        }
        for (String line : resultLabel) {
            stdout.println(line);
        }
        return SecurityUtilityReturnCodes.OK;
    }

    /**
     * @param args The command arguments
     * @return True if (v)erbose was input
     */
    private boolean checkVerboseArgs(String[] args) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(ARG_V) || arg.equalsIgnoreCase(ARG_VERBOSE)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        boolean value = false;
        if (arg != null) {
            value = ARG_TABLE.contains(arg);
        }
        return value;
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least two arguments and the task name
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        boolean portFound = false;
        boolean hostFound = false;
        for (String arg : args) {
            if (arg.startsWith(ARG_PORT)) {
                portFound = true;
            }
            if (arg.startsWith(ARG_HOST)) {
                hostFound = true;
            }
        }
        if (!hostFound) {
            message += " " + getMessage("missingArg", ARG_HOST);
        }
        if (!portFound) {
            message += " " + getMessage("missingArg", ARG_PORT);
        }
        if (!message.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    protected String getArgumentValue(String arg, String[] args,
                                      String defalt) {
        for (int i = 1; i < args.length; i++) {
            String key = args[i].split("=")[0];
            if (key.equals(arg)) {
                return getValue(args[i]);
            }
        }
        return defalt;
    }

    private class TlsProfilerResult {
        private String protocol;
        private String ciphersuite;
        private boolean isVulnerable;
        private boolean isSuccessful;

        public TlsProfilerResult() {
            setProtocol(new String());
            setCiphersuite(new String());
            setVulnerable(false);
            setSuccessful(false);
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getCiphersuite() {
            return ciphersuite;
        }

        public void setCiphersuite(String ciphersuite) {
            this.ciphersuite = ciphersuite;
        }

        public boolean isVulnerable() {
            return isVulnerable;
        }

        public void setVulnerable(boolean isVulnerable) {
            this.isVulnerable = isVulnerable;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public void setSuccessful(boolean isSuccessful) {
            this.isSuccessful = isSuccessful;
        }
    }
}
