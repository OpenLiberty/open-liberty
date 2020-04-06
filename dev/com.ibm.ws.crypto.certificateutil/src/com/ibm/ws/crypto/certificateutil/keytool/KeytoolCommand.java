/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil.keytool;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;

/**
 * Provides the logic necessary to build and execute keytool commands.
 */
class KeytoolCommand {

    private static final Class<?> CLASS_NAME = KeytoolCommand.class;
    private static final Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName());
    private static final int BUFFER_SIZE = 4096;
    private final ArrayList<String> cmd;
    private String toString = null;

    /**
     * KeytoolCommand represents a keytool command-line invocation.
     * <p>
     * No validation is done on the arguments, as the caller should have done
     * so already.
     *
     * @param filePath
     * @param password
     * @param validity
     * @param subjectDN
     * @param keySize
     * @param keyType
     * @param sigAlg
     * @param ksType
     * @param extInfo
     */
    KeytoolCommand(String filePath, String password, int validity, String subjectDN, int keySize, String keyType, String sigAlg, String ksType, List<String> extInfo) {

        cmd = new ArrayList<String>();
        cmd.add(getAbsoluteKeytoolPath());
        cmd.add("-genkey");
        cmd.add("-keystore");
        cmd.add(filePath);
        cmd.add("-storepass");
        cmd.add(password);
        cmd.add("-keypass");
        cmd.add(password);
        cmd.add("-validity");
        cmd.add(Integer.toString(validity));
        cmd.add("-dname");
        cmd.add(subjectDN);
        cmd.add("-alias");
        cmd.add(DefaultSSLCertificateCreator.ALIAS);
        cmd.add("-sigalg");
        cmd.add(sigAlg);
        cmd.add("-keyalg");
        cmd.add(keyType);
        cmd.add("-keySize");
        cmd.add(Integer.toString(keySize));
        cmd.add("-storetype");
        cmd.add(ksType);

        if (extInfo != null && !extInfo.isEmpty()) {
            for (Object extVal : extInfo) {
                cmd.add("-ext");
                cmd.add((String) (extVal));
            }
        }
    }

    /**
     * Construct the absolute path to the keytool binary based on the
     * java.home system property. We use the absolute path to prevent
     * any malicious binaries sitting in PATH from being used.
     * <p>
     * If this JVM can be trusted, then we should be able to trust its
     * keytool.
     *
     * @return
     */
    String getAbsoluteKeytoolPath() {
        String javaHome = System.getProperty("java.home");
        String keytool = javaHome + "/bin/keytool";
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            keytool = keytool + ".exe";
        }
        return keytool;
    }

    /**
     * Return the command created.
     *
     * @return
     */
    List<String> getCommandArgs() {
        return cmd;
    }

    /**
     * Executes the keytool command. If there are any problems executing
     * the command, a {@code CertificateException} will be thrown.
     * <p>
     * Observed return codes:<br>
     * 0 - success<br>
     * 1 - error
     *
     * @throws CertificateException if the certificate could not be created
     */
    void executeCommand() throws CertificateException {
        String exceptionMessage;
        Throwable cause = null;

        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Executing keytool command: " + this.toString());
            }
            Process proc = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Waiting for keytool command process to complete...");
            }
            int exitCode = proc.waitFor();
            if (exitCode == 0) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("keytool command process completed successfully");
                }
                return;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("keytool command process failed to complete successfully (exit code: " + exitCode + ")");
                }
                exceptionMessage = getProcErrorOutput(proc);
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("keytool command process execution resulted in IOException: " + e.getMessage());
            }
            exceptionMessage = e.getLocalizedMessage();
            cause = e;
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("InterruptedException while waiting for keytool command process result: " + e.getMessage());
            }
            exceptionMessage = e.getLocalizedMessage();
            cause = e;
        }

        if (cause != null) {
            throw new CertificateException(exceptionMessage, cause);
        } else {
            throw new CertificateException(exceptionMessage);
        }
    }

    /**
     * Grabs the error message generated by executing the process.
     *
     * @param proc
     * @throws IOException
     */
    String getProcErrorOutput(Process proc) throws IOException {
        StringBuffer output = new StringBuffer();

        InputStream procIn = proc.getInputStream();
        int read;
        // Dump the data printed by the process
        do {
            byte[] buffer = new byte[BUFFER_SIZE];
            read = procIn.read(buffer);
            String s = new String(buffer);
            output.append(s);
        } while (read == BUFFER_SIZE);

        return output.toString();
    }

    /**
     * Construct the command String with the passwords masked.
     *
     * @return String representing the keytool command (including keytool path)
     *         with the passwords masked.
     */
    @Override
    public String toString() {
        // If we have not yet computed the toString value, do so now and save
        // it as the value can not change
        if (toString == null) {
            StringBuffer buf = new StringBuffer();
            Iterator<String> itr = cmd.iterator();
            while (itr.hasNext()) {
                String str = itr.next();
                buf.append(str);
                // If we're on the last element, don't need a trailing space
                if (itr.hasNext()) {
                    buf.append(" ");
                }
                // If this is a password argument, mask the next entry
                if (str.equals("-storepass") || str.equals("-keypass")) {
                    buf.append("*** ");
                    itr.next(); // Does not require a guard as we know the order from construction
                }
            }
            toString = buf.toString();
        }
        return toString;
    }
}
