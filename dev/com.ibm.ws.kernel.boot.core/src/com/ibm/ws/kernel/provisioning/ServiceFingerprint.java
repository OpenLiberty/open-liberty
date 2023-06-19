/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 *
 */
public class ServiceFingerprint {
    private static volatile File fingerPrintFile;
    private static Map<String, String> props = new HashMap<>();

    public static boolean hasServiceBeenApplied(File installDir, File workarea) {
        boolean result = true; // default to true so we do a clean start if we can't work out if service is applied

        fingerPrintFile = new File(workarea, "platform/service.fingerprint");
        if (fingerPrintFile.exists()) {
            props = new HashMap<>();

            try (DataInputStream in = new DataInputStream(new FileInputStream(fingerPrintFile))) {
                MessageDigest digest = null;
                byte[] buffer = new byte[4096];
                boolean doClean = false;
                boolean foundCore = false;
                int size = in.readInt();

                for (int i = 0; i < size; i++) {
                    String entryName = in.readUTF();
                    String value = in.readUTF();
                    props.put(entryName, value);

                    if (entryName.indexOf(File.separatorChar) != -1) {
                        File file = new File(entryName);
                        if (file.exists()) {
                            long len = file.length();
                            if (len <= 256) {
                                String content = readBytes(file, (int) len);
                                if (!!!content.equals(value)) {
                                    doClean = true;
                                }
                            } else {
                                if (digest == null) {
                                    digest = MessageDigest.getInstance("SHA-256");
                                }
                                if (!!!calculateFileHash(digest, buffer, file).equals(value)) {
                                    doClean = true;
                                }
                            }
                        } else if (!!!"".equals(value)) {
                            doClean = true;
                        }
                    } else if ("core".equals(entryName)) {
                        if (!!!installDir.getCanonicalPath().equals(value)) {
                            doClean = true;
                        }
                        foundCore = true;
                    }
                    if (doClean) {
                        break;
                    }
                }

                result = doClean || !!!foundCore;
            } catch (IOException e) {
            } catch (NoSuchAlgorithmException e) {
            }
        } else {
            result = false;
        }
        return result;
    }

    public static void clear() {
        props.clear();
    }

    private static String calculateFileHash(MessageDigest digest, byte[] buffer, File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream fIn = new FileInputStream(file)) {
            int len;
            while ((len = fIn.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            byte[] digestBytes = digest.digest();
            for (byte b : digestBytes) {
                builder.append(toHexString(b));
            }
        }
        return builder.toString();
    }

    private static String toHexString(byte b) {
        String hex = Integer.toHexString(b);

        if (hex.length() == 0) {
            hex = "00";
        } else if (hex.length() == 1) {
            hex = "0" + hex;
        } else if (hex.length() > 2) {
            int len = hex.length();
            hex = hex.substring(len - 2);
        }

        return hex;
    }

    private static void internalPut(File file) {
        try {
            String hash = "";
            if (file.exists()) {
                long len = file.length();
                // if the file is less than 256 characters then just copy it in
                // this avoids the need to do an expensive hash.
                if (len <= 256) {
                    hash = readBytes(file, (int) len);
                } else {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] buffer = new byte[4096];
                    hash = calculateFileHash(digest, buffer, file);
                }
            }
            props.put(file.getCanonicalPath(), hash);
        } catch (NoSuchAlgorithmException e) {
        } catch (IOException e) {
        }
    }

    /**
     * @param file
     * @param len
     * @return
     * @throws IOException
     */
    private static String readBytes(File file, int bytesToRead) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (FileInputStream fIn = new FileInputStream(file)) {
            byte[] buffer = new byte[bytesToRead];
            int len;
            int offset = 0;
            while (bytesToRead > 0 && (len = fIn.read(buffer, offset, bytesToRead)) != -1) {
                offset += len;
                bytesToRead -= len;
            }
            for (byte b : buffer) {
                builder.append(toHexString(b));
            }
        }
        return builder.toString();
    }

    public static void put(File file) {
        internalPut(file);
        flush();
    }

    private static void flush() {
        if (fingerPrintFile != null) {
            File fingerPrintParent = fingerPrintFile.getParentFile();
            if (fingerPrintParent != null && !!!fingerPrintParent.exists()) {
                //make the necessary directories, if the make fails issue a message
                if (!fingerPrintParent.mkdirs()) {
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warn.fingerprintUnableToMkDirs"),
                                                            fingerPrintParent.getAbsolutePath(), fingerPrintFile.getAbsolutePath()));
                }
            }
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(fingerPrintFile))) {
                out.writeInt(props.size());
                for (Map.Entry<String, String> entries : props.entrySet()) {
                    out.writeUTF(entries.getKey());
                    out.writeUTF(entries.getValue());
                }
            } catch (IOException e) {
            }
        }
    }

    public static void putInstallDir(String name, File installDir) {
        if (name == null) {
            name = "core";
        }

        if (!props.containsKey(name)) {
            internalPut(new File(installDir, "lib/versions/service.fingerprint"));
            try {
                props.put(name, installDir.getCanonicalPath());
            } catch (IOException ioe) {
                // ignore this. If it happens for core we will do an auto clean next time, otherwise we don't really care.
            }

            flush();
        }
    }

}