/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * The ShutdownHook class provides cleanup logic for the server in the extracted jar file.
 * The ShutdownHook stops the server and deletes the extraction directory.
 */
public class ShutdownHook implements Runnable {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle(SelfExtract.class.getName() + "Messages");

    int platformType;
    String dir;
    String serverName;
    Thread out;
    Thread err;
    boolean extractDirPredefined = false;

    // no parm ctor not public
    private ShutdownHook() {

    }

    /**
     * The only constructor.
     *
     * @param platformType - platform type: unix(1), windows(2), cygwin(3)
     * @param dir - extraction directory
     * @param serverName - name of server from jar (in extraction directory)
     * @param out - output stream reader thread of parent process.
     * @param err - error stream reader thread of parent process.
     * @param extractDirPredefined - flag which indicates if WLP_JAR_EXTRACT_DIR was predefined by user
     */
    public ShutdownHook(int platformType,
                        String dir,
                        String serverName,
                        StreamReader out,
                        StreamReader err,
                        boolean extractDirPredefined) {

        this();
        this.serverName = serverName;
        this.out = out;
        this.err = err;
        this.dir = dir;
        this.platformType = platformType;
        this.extractDirPredefined = extractDirPredefined;
    }

    /**
     * Return PID from server directory for cygwin environment only.
     *
     * @return PID string or null if not cygwin environment or exception occurs
     * @throws IOException, FileNotFoundException if anything goes wrong
     */
    private String getPID(String dir, String serverName) {
        String pid = null;
        if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {
            String pidFile = dir + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + ".pid" + File.separator
                             + serverName + ".pid";
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile), "UTF-8"));
                try {
                    return br.readLine();
                } finally {
                    br.close();
                }
            } catch (IOException e) {
                pid = null;
            }

            if (pid == null) {
                Object[] substitution = { dir };
                System.out.println(MessageFormat.format(resourceBundle.getString("UNABLE_TO_FIND_PID"), substitution));
            }

        }
        return pid;
    }

    /**
     * Run server stop command
     *
     * @throws IOException if exec fails
     */
    private void stopServer() throws IOException {

        // build stop command for Unix platforms
        String cmd = dir + File.separator + "wlp" + File.separator + "bin" + File.separator + "server stop " + serverName;

        if (platformType == SelfExtractUtils.PlatformType_UNIX) {
            // use command as-is
        } else if (platformType == SelfExtractUtils.PlatformType_WINDOWS) {
            cmd = "cmd /k " + cmd;
        } else if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {
            cmd = "bash -c  " + '"' + cmd.replace('\\', '/') + '"';
        }

        Runtime.getRuntime().exec(cmd, SelfExtractUtils.runEnv(dir), null); // stop server

    }

    /**
     * Start async deletion using background script
     *
     * @throws IOException
     */
    private void startAsyncDelete() throws IOException {

        Runtime rt = Runtime.getRuntime();
        File scriptFile = null;
        if (platformType == SelfExtractUtils.PlatformType_UNIX) {
            scriptFile = writeCleanupFile(SelfExtractUtils.PlatformType_UNIX);
            rt.exec("chmod 750 " + scriptFile.getAbsolutePath());
            rt.exec("sh -c " + scriptFile.getAbsolutePath() + " &");
        } else if (platformType == SelfExtractUtils.PlatformType_WINDOWS) {
            scriptFile = writeCleanupFile(SelfExtractUtils.PlatformType_WINDOWS);
            // Note: must redirect output in order for script to run on windows.
            // This is a quirk validated by testing. Redirect to NUL is fine since we're
            // not trying to trap this output anyway.
            rt.exec("cmd /k start /B " + scriptFile.getAbsolutePath() + " >/NUL 2>/NUL");
        } else if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {
            scriptFile = writeCleanupFile(SelfExtractUtils.PlatformType_CYGWIN);
            // convert to Unix type path and run under bash
            rt.exec("bash -c " + scriptFile.getAbsolutePath().replace('\\', '/') + " &");
        }
    }

    /**
     * Write logic for windows cleanup script
     *
     * @param file - script File object
     * @param bw - bufferedWriter to write into script file
     * @throws IOException
     */
    private void writeWindowsCleanup(File file, BufferedWriter bw) throws IOException {

        String logDir = dir + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + File.separator + "logs";
        File tempDir = Files.createTempDirectory("logs").toFile();

        bw.write("set max=30\n");
        bw.write("set cnt=0\n");
        bw.write("set dir=" + dir + "\n");
        bw.write("set tempDir=" + tempDir.getAbsolutePath() + "\n");
        bw.write("set logDir=" + logDir + "\n");
        bw.write("echo delete %dir%\n");
        bw.write("sleep 5\n");
        bw.write(":while\n");
        bw.write("   if exist %dir% (\n");
        bw.write("      xcopy /E/H/C/I %logDir% %tempDir% \n");
        bw.write("      rmdir /s /q %dir%\\wlp\n");
        bw.write("      mkdir %logDir%\n");
        bw.write("      xcopy /E/H/C/I %tempDir% %logDir%\n");
        bw.write("      timeout 1\n");
        bw.write("      set /a cnt+=1\n");
        bw.write("      if %cnt% leq %max% (\n");
        bw.write("         goto :while \n");
        bw.write("      )\n");
        bw.write("   )\n ");
        bw.write("erase " + file.getAbsoluteFile() + "\n");
        bw.write("erase " + tempDir.getAbsolutePath() + "\n");

    }

    /**
     * Write logic for Unix cleanup script
     *
     * @param file - script File object
     * @param bw - bufferedWriter to write into script file
     * @throws IOException
     */
    private void writeUnixCleanup(File file, BufferedWriter bw) throws IOException {

        String logDir = dir + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + File.separator + "logs";
        String serverDir = dir + File.separator + "wlp" + File.separator + "usr" + File.separator + "servers" + File.separator + serverName + File.separator;

        File tempDir = Files.createTempDirectory("logs").toFile();

        bw.write("echo begin delete" + "\n");
        bw.write("n=0" + "\n");
        bw.write("while [ $n -ne 1 ]; do" + "\n");
        bw.write("  if [ -e " + dir.replace('\\', '/') + "/wlp ]; then" + "\n");
        bw.write("    cp -r " + logDir.replace('\\', '/') + " " + tempDir.getAbsolutePath().replace('\\', '/') + "\n");
        bw.write("    rm -rf " + dir.replace('\\', '/') + "/wlp/ \n");
        bw.write("  else" + "\n");
        bw.write("    echo file not found - n=$n" + "\n");
        bw.write("    n=1" + "\n");
        bw.write("  fi" + "\n");
        bw.write("done" + "\n");
        bw.write("mkdir -p " + logDir.replace('\\', '/') + "\n");
        bw.write("cp -r " + tempDir.getAbsolutePath().replace('\\', '/') + "/logs/ " + serverDir.replace('\\', '/') + "\n");
        bw.write("chmod -R 755 " + dir.replace('\\', '/') + "\n");
        bw.write("rm -rf " + file.getAbsolutePath().replace('\\', '/') + "\n");
        bw.write("rm -rf " + tempDir.getAbsolutePath().replace('\\', '/') + "\n");
        bw.write("echo end delete" + "\n");
    }

    /**
     * Write logic for Cygwin cleanup script
     *
     * @param file - script File object
     * @param bw - bufferedWriter to write into script file
     * @throws IOException
     */
    private void writeCygwinCleanup(File file, BufferedWriter bw) throws IOException {
        // Under cygwin, must explicitly kill the process that runs
        // the server. It simply does not die on its own. And it's
        // JVM holds file locks which will prevent cleanup of extraction
        // directory. So kill it.
        String pid = getPID(dir, serverName);
        if (pid != null)
            bw.write("kill " + pid + "\n");
        writeUnixCleanup(file, bw);
    }

    /**
     * Write script file to clean up extraction directory.
     * The reason a script is required, rather than a Java
     * file delete method or a simple Runtime.exec of an OS
     * delete command is because neither work reliably. Even
     * after the 'server run process' has ended, Java methods
     * and single OS commands have failed to reliably delete
     * the extraction directory. This has been observed on
     * both Windows and Unix (Ubuntu) platforms.
     * It appears there is a lock release latency of some kind
     * preventing one-shot deletion from working. So a script
     * with a loop is the approach used here. Additionally,
     * launching a background script offers the added value
     * of doing the delete in the background, even after the
     * foreground process has terminated.
     *
     * @param type is the platform type: unix(1), windows(2), or cygwin(3)
     * @return a script File object
     * @throws IOException
     */
    private File writeCleanupFile(int platformType) throws IOException {

        String fileSuffix = ".sh";

        if (platformType == SelfExtractUtils.PlatformType_WINDOWS) {
            fileSuffix = ".bat";
        }

        File file = File.createTempFile("wlpDelete", fileSuffix);
        if (!file.exists()) {
            boolean success = file.createNewFile();
            if (!success) {
                throw new IOException("Failed to create file " + file.getName());
            }
        }

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsoluteFile()), "UTF-8"));

        if (platformType == SelfExtractUtils.PlatformType_UNIX) {
            writeUnixCleanup(file, bw);
        } else if (platformType == SelfExtractUtils.PlatformType_WINDOWS) {
            writeWindowsCleanup(file, bw);
        } else if (platformType == SelfExtractUtils.PlatformType_CYGWIN) {
            writeCygwinCleanup(file, bw);
        }

        bw.close();

        return file;
    }

    /**
     * Main method for shutdown hook. Job of this hook
     * is to stop server and delete extraction directory.
     */
    @Override
    public void run() {
        try {

            stopServer(); // first, stop server

            // wait on error/output stream threads to complete
            // note on Windows the streams never close, so wait with brief timeout
            if (out != null && err != null) {
                if (!System.getProperty("os.name").startsWith("Win")) {
                    out.join();
                    err.join();
                } else { // windows, so use timeout
                    out.join(500);
                    err.join(500);
                }
            }

            // When the server is launched with java -jar, delete the server on exit minus
            // the /logs folder, unless WLP_JAR_EXTRACT_DIR is set at which point don't delete
            // anything.

            if (extractDirPredefined != true) {
                startAsyncDelete(); // now launch async process to cleanup extraction directory
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Shutdown hook failed with exception " + e.getMessage());
        }

    }

}