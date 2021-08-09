/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory;

/**
 * The ServerDumpPackager encapsulates the logic of creating an archive file containing
 * various server dump data.
 * 
 * The usage pattern of this helper class is as follows:
 * 
 * -- create an instance of the server dump packager using your chosen constructor
 * -- call initializeDumpDirectory() to create the temporary directory that will contain dump information
 * -- if additional information is to be included in the dump (such as a server introspection), the location
 * of the dump directory can be obtained by calling getDumpDir(); any information placed in the dump directory
 * prior to calling packageDump() will be included in the dump
 * -- call packageDump() to created the server dump
 * -- call getDumpFile() to get a File reference to the server dump, if desired
 * -- call cleanupDumpDirectory() to clean up the temporary directory
 */
public class ServerDumpPackager {
    final String serverName;
    final String serverOutputDir;
    final BootstrapConfig bootProps;
    final String dumpTimestamp;
    File dumpDir = null;
    File archive = null;

    public ServerDumpPackager(BootstrapConfig bootProps) {
        this(bootProps, null);
    }

    public ServerDumpPackager(BootstrapConfig bootProps, String packageFileName) {
        this.serverName = bootProps.getProcessName();
        this.bootProps = bootProps;

        serverOutputDir = bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVOUT_DIR);

        // create timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd_HH.mm.ss");
        dumpTimestamp = sdf.format(new Date());

        // create archive file for server dump
        if (packageFileName == null || packageFileName.isEmpty()) {
            packageFileName = serverName + ".dump-" + dumpTimestamp + ".zip";
            archive = new File(serverOutputDir, packageFileName);
        } else {
            archive = new File(packageFileName);
            if (!archive.isAbsolute()) {
                archive = new File(serverOutputDir, packageFileName);
            }
        }
    }

    public void initializeDumpDirectory() {
        if (dumpDir == null) {
            // create dump dir based on timestamp
            dumpDir = new File(serverOutputDir, BootstrapConstants.SERVER_DUMP_FOLDER_PREFIX + dumpTimestamp);
            if (!FileUtils.createDir(dumpDir))
                throw new IllegalStateException("Dump directory could not be created.");
        }
    }

    public void cleanupDumpDirectory() {
        if (dumpDir != null) {
            FileUtils.recursiveClean(dumpDir);
            dumpDir = null;
        }
    }

    public File getDumpFile() {
        return archive;
    }

    public File getDumpDir() {
        return dumpDir;
    }

    public String getDumpTimestamp() {
        return dumpTimestamp;
    }

    /**
     * Package the server dump and optionally record error data.
     * 
     * @return
     */
    public ReturnCode packageDump(boolean javaDumpsRequested) {
        // Add xml marker file for server dump tool
        File autoPdZip = new File(serverOutputDir, "autopdzip");
        File autoPd = new File(autoPdZip, "autopd/autopd-collection-environment-v2.xml");
        if (autoPd.getParentFile().mkdirs()) {
            try {
                FileUtils.createFile(autoPd, getClass().getResourceAsStream("/OSGI-OPT/websphere/autopd-collection-environment-v2.xml"));
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }

        captureEnvData(dumpDir, bootProps.getInstallRoot());

        // we also want the dump zip contains the lib inventory, so generate one. 
        File libInventory = new File(dumpDir, BootstrapConstants.SERVER_LIB_INVENTORY_FILE_NAME + ".txt");
        if (!new FolderStructureGenerator().generate(bootProps.getInstallRoot(), libInventory)) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.LibInventoryGenerationException"), serverName));
        }

        List<String> javaDumps = new ArrayList<String>();
        if (javaDumpsRequested) {
            // Find java dumps locations
            File javaDumpLocationFile = new File(dumpDir + File.separator + BootstrapConstants.SERVER_DUMPED_FILE_LOCATIONS);
            Map<JavaDumpAction, String> javaDumpLocations = ProcessControlHelper.readJavaDumpLocations(javaDumpLocationFile);

            if (javaDumpLocations == null) {
                // the most likely reason for this being null is a file not found
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.fileNotFound"), javaDumpLocationFile.getAbsolutePath()));
            } else {
                javaDumps.addAll(javaDumpLocations.values());
                // Remove any blank entries
                javaDumps.remove("");

                // If we're on z/OS, convert the javacore (if present) from EBCDIC before packaging.
                if (ServerDumpUtil.isZos() && Charset.isSupported("IBM-1047")) {
                    String javacoreName = javaDumpLocations.get(JavaDumpAction.THREAD);
                    if ((javacoreName != null) && !(javacoreName.equals(""))) {
                        try {
                            File javacoreEbcdic = new File(javacoreName);
                            File javacoreAscii = new File(javacoreEbcdic.getAbsolutePath() + ".tmp");
                            javacoreAscii.createNewFile();
                            javacoreAscii.setWritable(true);

                            InputStreamReader reader = new InputStreamReader(new FileInputStream(javacoreEbcdic), Charset.forName("IBM-1047"));
                            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(javacoreAscii), Charset.forName("US-ASCII"));
                            int readInt;
                            while ((readInt = reader.read()) != -1) {
                                writer.write(readInt);
                            }

                            reader.close();
                            writer.close();
                            if (javacoreAscii.exists()) {
                                javacoreEbcdic.delete();
                                javacoreAscii.renameTo(javacoreEbcdic);
                            }
                        } catch (IOException e) {
                            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.cannotConvertEbcdicToAscii"), javacoreName));
                            Debug.printStackTrace(e);
                        }
                    }
                }
            }
        }

        ReturnCode rc = packageServerDumps(archive, javaDumps);

        // clean up javaDumps
        if (!!!javaDumps.isEmpty()) {
            for (String f : javaDumps) {
                File file = new File(f);
                file.delete();
            }
        }

        // clean up autoPdZip
        FileUtils.recursiveClean(autoPdZip);

        return rc;
    }

    /**
     * Copy relevant data (like service data, shared config, etc) to the dump dir to be zipped up.
     * 
     * @param dumpDir
     * @param installDir
     */
    private void captureEnvData(File dumpDir, File installDir) {
        File versionsSource = new File(installDir, "lib/versions");
        File fixesSource = new File(installDir, "lib/fixes");
        String sharedConfigDir = AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getProperty("shared.config.dir");
            }

        });
        File sharedConfigSource = sharedConfigDir != null ? new File(sharedConfigDir) : new File(installDir, "usr/shared/config");

        File versionsTarget = new File(dumpDir, "service/versions");
        File fixesTarget = new File(dumpDir, "service/fixes");
        File sharedConfigTarget = new File(dumpDir, "usr/shared/config");

        if (versionsTarget.mkdirs()) {
            try {
                FileUtils.copyDir(versionsSource, versionsTarget);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
        if (fixesTarget.mkdirs()) {
            try {
                FileUtils.copyDir(fixesSource, fixesTarget);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
        if (sharedConfigTarget.mkdirs()) {
            try {
                FileUtils.copyDir(sharedConfigSource, sharedConfigTarget);
            } catch (IOException e) {
                Debug.printStackTrace(e);
            }
        }
    }

    static class FolderStructureGenerator {

        protected static MessageDigest newMD5MessageDigest() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (Exception ex) {
                return null;
            }
        }

        private static final int NUM_MD5_BYTES = 16;

        private final MessageDigest md5 = newMD5MessageDigest();
        private final byte[] md5Bytes = new byte[0x4000];
        private final char[] md5Chars = new char[NUM_MD5_BYTES * 2];

        private boolean generate(File folder, File output) {
            if (folder == null || output == null) {
                return false;
            }
            if (!folder.exists() || !folder.isDirectory()) {
                return false;
            }

            BufferedWriter writer = null;
            try {
                FileOutputStream outputStream = TextFileOutputStreamFactory.createOutputStream(output);
                writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                Formatter formatter = new Formatter(writer);
                printFileList(folder, formatter, folder.getAbsolutePath().length() + 1);
            } catch (IOException e) {
                return false;
            } finally {
                Utils.tryToClose(writer);
            }

            return true;
        }

        void printFileList(File targetDir, Formatter writer, int leadingPathLength) throws IOException {

            File[] contents = targetDir.listFiles();

            if (contents != null) {
                List<File> contentList = Arrays.asList(contents);
                Collections.sort(contentList);

                for (File file : contentList) {
                    String fileName = file.getAbsolutePath().substring(leadingPathLength);

                    if (file.isDirectory()) {
                        writer.format("d  %1$10s  %2$tF %2$tT  %3$32s  %4$s%n", "", file.lastModified(), "", fileName + File.separator);
                        // Stop recursing the listing when we reach the usr dir (if there is one)
                        // we're only trying to list the install contents
                        if (!BootstrapConstants.LOC_AREA_NAME_USR.equals(file.getName()))
                            printFileList(file, writer, leadingPathLength);
                    } else {
                        writer.format("f  %1$10d  %2$tF %2$tT  %3$32s  %4$s%n", file.length(), file.lastModified(), md5(file), fileName);
                    }
                }
            }
        }

        protected String md5(File file) {
            if (md5 == null) {
                return "MD5 unavailable";
            }

            InputStream in = null;
            try {
                in = new FileInputStream(file);
                in = new BufferedInputStream(in);

                for (int read; (read = in.read(md5Bytes)) != -1;) {
                    md5.update(md5Bytes, 0, read);
                }
            } catch (IOException ex) {
                return ex.toString();
            } finally {
                Utils.tryToClose(in);
            }

            byte[] bytes = md5.digest();
            int j = 0;
            for (int i = 0; i < NUM_MD5_BYTES; i++) {
                byte b = bytes[i];
                md5Chars[j++] = Character.forDigit((b >> 4) & 0xf, 16);
                md5Chars[j++] = Character.forDigit(b & 0xf, 16);
            }

            return new String(md5Chars);
        }
    }

    /**
     * Creates an archive containing the server dumps, server configurations.
     * 
     * @param packageFile
     * @return
     */
    private ReturnCode packageServerDumps(File packageFile, List<String> javaDumps) {
        DumpProcessor processor = new DumpProcessor(serverName, packageFile, bootProps, javaDumps);
        return processor.execute();
    }
}
