/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package componenttest.topology.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.FileUtils;

public class Logstash implements LogMonitorClient {

    protected static final Class<?> c = Logstash.class;
    protected static Logger LOG = Logger.getLogger(c.getName());

    protected Machine machine = null;
    protected LogMonitor logMonitor = null;
    protected static final int LOG_SEARCH_TIMEOUT = 120 * 1000;
    protected static final int LOG_SEARCH_EXTENDED_TIMEOUT = 240 * 1000;

    //Used for keeping track of offset positions of log files
    protected final HashMap<String, Long> logOffsets = new HashMap<String, Long>();

    public static final String JAVA_SECURITY_OVERWRITE_FILE = "java.security";
    static final String AUTOFVT_DIR = System.getProperty("user.dir");
    public static String CONFIG_FILENAME = "logstash.conf";
    static final String CONFIG_TAG_FILENAME = "logstash_tag.conf";
    public static String OUTPUT_FILENAME = "logstash_output.txt";
    static String CONSOLE_FILENAME = "logstash-plain.log";
    private static final String JAVA9_ARGS = "--add-opens=java.base/java.lang=ALL-UNNAMED" +
                                             " --add-opens=java.base/java.lang.reflect=ALL-UNNAMED" +
                                             " --add-opens=java.base/java.util.regex=ALL-UNNAMED" +
                                             " --add-opens=java.base/java.net=ALL-UNNAMED";

    static final String WIN_PUBLISH_FILES_DIR = "\\lib\\LibertyFATTestFiles";
    static final String WIN_LOGSTASH_DIR = WIN_PUBLISH_FILES_DIR + "\\logstash-5.5.0";
    static final String WIN_LOGSTASH_LOGS_DIR = WIN_LOGSTASH_DIR + "\\logs";
    static final String winKillLogstash[] = { "cmd.exe", "/c", "WMIC", "PROCESS", "WHERE", "\"CommandLine Like '%logstash-5.5.0%'\"", "Call", "Terminate" };

    static final String UNIX_PUBLISH_FILES_DIR = "/lib/LibertyFATTestFiles";
    static final String UNIX_LOGSTASH_DIR = UNIX_PUBLISH_FILES_DIR + "/logstash-5.5.0";
    static final String UNIX_LOGSTASH_LOGS_DIR = UNIX_LOGSTASH_DIR + "/logs";
    static final String UNIX_LOGSTASH_CMD = "/bin/logstash";
    static final String UNIX_JRUBY_CMD = "/vendor/jruby/bin/jruby";

    private OutputReader reader;
    private final StringBuffer sb = new StringBuffer();
    private Process process;
    public static final int MAX_VALUE = 0x7fffffff;

    public Logstash(Machine machine) {
        //Initialize LogMonitor
        logMonitor = new LogMonitor(this);
        this.machine = machine;
    }

    public boolean enableTag() throws Exception {
        File configFile = new File(AUTOFVT_DIR + "/lib/LibertyFATTestFiles/logstash-5.5.0/" + CONFIG_FILENAME);
        File modifiedConfig = new File(AUTOFVT_DIR + "/lib/LibertyFATTestFiles/logstash-5.5.0/" + CONFIG_TAG_FILENAME);
        if (!configFile.exists()) {
            Log.info(c, " enable tag", "-->cannot find config file " + configFile.getAbsolutePath());
            return false;
        }
        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(modifiedConfig));

        String line;
        String lineToRemove = "add_tag => " + '"' + "unrecognized_type" + '"';
        boolean found = false;
        while ((line = reader.readLine()) != null) {
            if (line.contains(lineToRemove)) {
                Log.info(c, " found disabled tag section to remove ", line);
                found = true;
                continue;
            }
            writer.write(line + "\n");
        }
        reader.close();
        writer.close();

        if (!found) {
            Log.info(c, " cannot find the disable tag line to remove", ", need to debug");
            return found;
        }

        return true;

    }

    public void start() throws Exception {
        final String method = "start";
        Log.entering(c, method);
        String winCommands[] = { "cmd.exe", "/c", "bin\\logstash.bat", "-f", CONFIG_FILENAME, "--verbose",
                                 "-l", ".\\logs" };

        String unixCommands[] = { "./bin/logstash", "-f", CONFIG_FILENAME, "--verbose", "-l", "./logs" };

        ProcessBuilder pb = null;
        File logsDir = null;
        if (isWindows()) {
            pb = new ProcessBuilder(winCommands);
            pb.directory(new File(AUTOFVT_DIR + WIN_LOGSTASH_DIR));
            pb.redirectErrorStream(true);
            logsDir = new File(AUTOFVT_DIR + WIN_LOGSTASH_LOGS_DIR);
        } else {
            boolean isSuccess;
            isSuccess = setExecutiblePermission(AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_LOGSTASH_CMD);
            if (!isSuccess) {
                Log.warning(c, "Setting execution bit for " + AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_LOGSTASH_CMD + " failed");
            }
            isSuccess = setExecutiblePermission(AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_JRUBY_CMD);
            if (!isSuccess) {
                Log.warning(c, "Setting execution bit for " + AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_JRUBY_CMD + " failed");
            }
            pb = new ProcessBuilder(unixCommands);
            pb.directory(new File(AUTOFVT_DIR + UNIX_LOGSTASH_DIR));
            pb.redirectErrorStream(true);
            logsDir = new File(AUTOFVT_DIR + UNIX_LOGSTASH_LOGS_DIR);
        }

        Log.info(c, method, "Logstash log is written to " + logsDir);
        // Clean logsDir
        FileUtils.recursiveDelete(logsDir);

        //Set the System variable to overwrite the Java Security for starting logstash
        Map<String, String> env = pb.environment();
        String ls_java_opts_value = "-Djava.security.properties=" + this.getJavaSecuritySettingFilePath();
        env.put("LS_JAVA_OPTS", ls_java_opts_value);
        Log.info(c, method, "set env LS_JAVA_OPTS=" + ls_java_opts_value);
        Boolean found = new File(this.getJavaSecuritySettingFilePath()).exists();
        Log.info(c, method, this.getJavaSecuritySettingFilePath() + " is " + (found ? "found" : "NOT found"));
        if (JavaInfo.JAVA_VERSION >= 9) {
            pb.environment().put("JAVA_OPTS", JAVA9_ARGS);
            Log.info(c, method, "set env JAVA_OPTS=" + JAVA9_ARGS);
        }
        process = pb.start();
        reader = new OutputReader(process.getInputStream());
        reader.start();

        String line = null;
        try {
            line = waitForStringInConsole("Pipeline main started");
        } catch (FileNotFoundException e) {
            Log.error(c, method, e);
        }
        if (line == null) {
            Log.info(c, method, "Logstash could not be started.");
            Log.info(c, method, "os.name = " + System.getProperty("os.name"));
            Log.info(c, method, "os.arch = " + System.getProperty("os.arch"));
            Log.info(c, method, "Try to save logstash console file");
            Log.info(c, method, "Please look under publish/logstash-plain.log for further details");

            File console = new File(getConsoleFilename());
            File console_saved = new File(AUTOFVT_DIR + "/output/servers/" + System.currentTimeMillis() + "_" + CONSOLE_FILENAME);
            if (!new File(AUTOFVT_DIR + "/output/servers").exists()) {
                boolean isCreated = new File(AUTOFVT_DIR + "/output/servers").mkdirs();
                if (!isCreated) {
                    Log.info(c, method, "Failed to create directory " + AUTOFVT_DIR + "/output/servers");
                }
            }
            if (console.exists()) {
                FileUtils.copyFile(console, console_saved);
            }

            throw new IOException("Pipeline started not found.  Logstash could not be started.  Check FAT log for details.");
        }

        Log.exiting(c, method);
    }

    public void startWithJsonOutput() throws Exception {
        final String method = "start";
        Log.entering(c, method);
        String winCommandsJson[] = { "cmd.exe", "/c", "bin\\logstash.bat", "-f", CONFIG_FILENAME, "--verbose",
                                     "-l", ".\\logs" };
        String unixCommandsJson[] = { "./bin/logstash", "-f", CONFIG_FILENAME, "--verbose", "-l",
                                      "./logs" };
        if (isWindows()) {
            ProcessBuilder pb = new ProcessBuilder(winCommandsJson);
            pb.directory(new File(AUTOFVT_DIR + WIN_LOGSTASH_DIR));
            pb.redirectErrorStream(true);
            if (JavaInfo.JAVA_VERSION >= 9)
                pb.environment().put("LS_JAVA_OPTS", JAVA9_ARGS);
            process = pb.start();
            reader = new OutputReader(process.getInputStream());
            reader.start();
        } else {
            boolean isSuccess;
            isSuccess = setExecutiblePermission(AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_LOGSTASH_CMD);
            if (!isSuccess) {
                Log.warning(c, "Setting execution bit for " + AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_LOGSTASH_CMD + " failed");
            }
            isSuccess = setExecutiblePermission(AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_JRUBY_CMD);
            if (!isSuccess) {
                Log.warning(c, "Setting execution bit for " + AUTOFVT_DIR + UNIX_LOGSTASH_DIR + UNIX_JRUBY_CMD + " failed");
            }
            ProcessBuilder pb = new ProcessBuilder(unixCommandsJson);
            pb.directory(new File(AUTOFVT_DIR + UNIX_LOGSTASH_DIR));
            pb.redirectErrorStream(true);
            if (JavaInfo.JAVA_VERSION >= 9)
                pb.environment().put("LS_JAVA_OPTS", JAVA9_ARGS);
            process = pb.start();
            reader = new OutputReader(process.getInputStream());
            reader.start();
        }
        String line = waitForStringInConsole("Pipeline started");
        if (line == null) {
            Log.info(c, method, "Logstash could not be started.");
            Log.info(c, method, "OS=" + machine.getOperatingSystem());
            Log.info(c, method, "Version=" + machine.getOSVersion());
            Log.info(c, method, "RawOS=" + machine.getRawOSName());

            throw new IOException("Pipeline started not found.  Logstash could not be started.  Check FAT log for details.");
        }

        Log.exiting(c, method);
    }

    public void stop() throws Exception {
        final String method = "stop";
        Log.entering(c, method);

        if (isWindows()) {
            //Process.destroy() does not work on Windows
            new ProcessBuilder(winKillLogstash).start();
            process.waitFor();
            Log.info(c, method, "process killed");
        } else {
            if (process != null) {
                process.destroy();
                process.waitFor();
                Log.info(c, method, "process destroyed");
            }
        }
        reader.join();
        Log.info(c, method, "Logstash stdout and stderr: " + sb.toString());

        File console = new File(getConsoleFilename());
        File console_saved = new File(AUTOFVT_DIR + "/output/servers/" + System.currentTimeMillis() + "_" + CONSOLE_FILENAME);
        if (!new File(AUTOFVT_DIR + "/output/servers").exists()) {
            boolean isCreated = new File(AUTOFVT_DIR + "/output/servers").mkdirs();
            if (!isCreated) {
                Log.info(c, method, "Failed to create directory " + AUTOFVT_DIR + "/output/servers");
            }
        }
        if (console.exists()) {
            FileUtils.copyFile(console, console_saved);
        }

        Log.exiting(c, method);
    }

    public String getLogFilename() throws Exception {
        if (isWindows()) {
            return AUTOFVT_DIR + WIN_LOGSTASH_DIR + "\\" + OUTPUT_FILENAME;
        } else {
            return AUTOFVT_DIR + UNIX_LOGSTASH_DIR + "/" + OUTPUT_FILENAME;
        }
    }

    public String getConsoleFilename() throws Exception {
        if (isWindows()) {
            return AUTOFVT_DIR + WIN_LOGSTASH_LOGS_DIR + "\\" + CONSOLE_FILENAME;
        } else {
            return AUTOFVT_DIR + UNIX_LOGSTASH_LOGS_DIR + "/" + CONSOLE_FILENAME;
        }
    }

    public String getJavaSecuritySettingFilePath() throws Exception {
        if (isWindows()) {
            return AUTOFVT_DIR + WIN_PUBLISH_FILES_DIR + "\\" + JAVA_SECURITY_OVERWRITE_FILE;
        } else {
            return AUTOFVT_DIR + UNIX_PUBLISH_FILES_DIR + "/" + JAVA_SECURITY_OVERWRITE_FILE;
        }
    }

    private boolean isWindows() throws Exception {
        return machine.getOperatingSystem() == OperatingSystem.WINDOWS;
    }

    private static boolean setExecutiblePermission(String filename) throws IOException {
        final String method = "setExecutiblePermission";
        File file = new File(filename);
        boolean result = false;
        if (file.exists()) {
            Log.info(c, method, "Is " + filename + " executable before setExecutable:" + file.canExecute());
            result = file.setExecutable(true, true);
            Log.info(c, method, "Is " + filename + " executable after setExecutable:" + file.canExecute());
        } else {
            throw new IOException(filename + " does not exist");
        }
        return result;
    }

    class OutputReader extends Thread {

        InputStream is;

        public OutputReader(InputStream is) {
            this.is = is;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final String LF = System.getProperty("line.separator");
            try {
                for (String line; (line = reader.readLine()) != null;) {
                    sb.append(line + LF);
                }
            } catch (IOException ex) {
                throw new Error(ex);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public RemoteFile lmcGetDefaultLogFile() throws Exception {
        RemoteFile file = LibertyFileManager.getLibertyFile(machine, getLogFilename());
        if (file == null) {
            throw new IllegalStateException("Unable to find default log file, path=" + getLogFilename());
        }
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void lmcClearLogOffsets() {
        logOffsets.clear();
    }

    /** {@inheritDoc} */
    @Override
    public void lmcUpdateLogOffset(String logFile, Long newLogOffset) {
        Long oldLogOffset = logOffsets.put(logFile, newLogOffset);
        Log.info(c, "updateLogOffset", "old log offset=" + oldLogOffset + ", new log offset=" + newLogOffset);
    }

    public String waitForStringInLogUsingMark(String regexp) throws Exception {
        return waitForStringInLogUsingMark(regexp, LOG_SEARCH_TIMEOUT);
    }

    public String waitForStringInLogUsingMark(String regexp, long timeout) throws Exception {
        String outputFilename = getLogFilename();

        if (outputFilename == null) {
            Log.info(c, "waitForStringInLogUsingMark", "outputFilename is null");
            return null;
        }

        RemoteFile f = getRemoteFile(outputFilename);

        if (f != null) {
            return logMonitor.waitForStringInLogUsingMark(regexp, timeout);
        } else {
            return null;
        }
    }

    public String waitForStringInConsole(String regexp) throws FileNotFoundException {
        String consoleFilename = null;
        try {
            consoleFilename = getConsoleFilename();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (consoleFilename == null) {
            Log.info(c, "waitForStringInConsole", "consoleFilename is null");
            return null;
        }

        RemoteFile f = getRemoteFile(consoleFilename);

        if (f != null) {
            return logMonitor.waitForStringInLogUsingMark(regexp, LOG_SEARCH_TIMEOUT, LOG_SEARCH_EXTENDED_TIMEOUT, f);
        } else {
            return null;
        }
    }

    public void setMarkToEndOfLog() throws Exception {
        logMonitor.setMarkToEndOfLog(getRemoteFile(getLogFilename()));
    }

    public Long getMarkOffset() throws Exception {
        return logMonitor.getMarkOffset(getRemoteFile(getLogFilename()).getAbsolutePath());
    }

    public RemoteFile getRemoteFile(String filename) throws FileNotFoundException {
        RemoteFile f = null;

        Log.info(c, "waitForFileExistence", filename + " started");
        for (int i = 1; (i <= 40) && (f == null); i++) {
            try {
                f = LibertyFileManager.getLibertyFile(machine, filename);
            } catch (Exception e) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                }
            }
        }
        if (f == null) {
            Log.info(c, "waitForFileExistence", filename + " was not found after 60 seconds");
            throw new FileNotFoundException(filename);
        } else {
            Log.info(c, "waitForFileExistence", filename + " found");
        }
        return f;
    }

    public List<JSONObject> parseOutputFile() throws Exception {
        return parseOutputFile(1);
    }

    public List<JSONObject> parseOutputFile(int startLine) throws Exception {
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        RemoteFile outputFile = getRemoteFile(getLogFilename());
        InputStreamReader isr = null;
        BufferedReader br = null;
        int lineNumber = 0;
        try {
            isr = new InputStreamReader(outputFile.openForReading());
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber >= startLine) {
                    JSONObject jobj = new JSONObject(line);
                    list.add(jobj);
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
        return list;
    }

    public boolean isSupportedPlatform() {
        final String method = "isSupportedPlatform";
        String os = System.getProperty("os.name").toLowerCase();
        Log.info(c, method, "os.name = " + os);

        String arch = System.getProperty("os.arch").toLowerCase();
        Log.info(c, method, "os.arch = " + arch);

        return !os.contains("zos") && !os.contains("z/os") && !os.contains("os/390") && !os.contains("os390")
               && !os.contains("mac")
               && !(os.contains("linux") && ((arch.contains("ppc") || arch.contains("s390x"))) && !os.contains("sunos"))
               && !os.contains("aix");
    }

    /** {@inheritDoc} */
    @Override
    public void lmcResetLogOffsets() {
    }

    /** {@inheritDoc} */
    @Override
    public void lmcSetOriginLogOffsets() {
    }
}
