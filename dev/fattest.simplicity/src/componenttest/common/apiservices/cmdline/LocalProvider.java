/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.common.apiservices.cmdline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.ibm.websphere.simplicity.AsyncProgramOutput;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

public class LocalProvider {

    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");

    protected static final String EBCDIC_CHARSET_NAME = "IBM1047";

    @SuppressWarnings("unchecked")
    private static final Class c = LocalProvider.class;

    public static boolean rename(RemoteFile oldPath, RemoteFile newPath) throws Exception {
        return new File(oldPath.toString()).renameTo(new File(newPath.getAbsolutePath()));
    }

    public static boolean copy(RemoteFile sourceFile, RemoteFile destFile,
                               boolean binary) throws Exception {
        File src = new File(sourceFile.getAbsolutePath());
        File dest = new File(destFile.getAbsolutePath());
        if (dest.exists() && dest.isDirectory()) {
            dest = new File(destFile.getAbsolutePath() + File.separatorChar
                            + src.getName());
        } else if (!dest.exists())
            dest.getParentFile().mkdirs();
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dest);
        int read;
        try {
            byte[] buffer = new byte[8192];
            while ((read = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
                // let it go
            }
            try {
                fos.close();
            } catch (Exception e) {
                // let it go
            }
        }

        return dest.exists();
    }

    public static boolean delete(RemoteFile file) throws Exception {
        File f = new File(file.getAbsolutePath());
        if (f.isFile())
            return f.delete();
        else {
            // recursive delete
            deleteFolder(f);
            return !f.exists();
        }
    }

    private static void deleteFolder(File folder) throws Exception {
        if (!folder.exists())
            return;

        for (File f : folder.listFiles()) {
            if (f.isFile())
                f.delete();
            else
                deleteFolder(f);
        }
        folder.delete();
    }

    public static ProgramOutput executeCommand(Machine machine, String cmd,
                                               String[] parameters, String workDir, Properties envVars) throws Exception {
        ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bufferErr = new ByteArrayOutputStream();
        int rc = execute(machine, cmd, parameters, envVars, workDir, bufferOut,
                         bufferErr, false);
        ProgramOutput ret = new ProgramOutput(cmd, rc, bufferOut.toString(), bufferErr.toString());
        return ret;
    }

    public static void executeCommandAsync(Machine machine, String cmd, String[] parameters, String workDir, Properties envVars, OutputStream redirect) throws Exception {
        execute(machine, cmd, parameters, envVars, workDir, redirect, null, true);
    }

    private static final int execute(Machine machine, final String command,
                                     final String[] parameterArray, Properties envp,
                                     final String workDir, final OutputStream stdOutStream,
                                     final OutputStream stdErrStream, boolean async) throws Exception {
        final String method = "execute";
        Log.entering(c, method, "async is " + async);

        if (command == null) {
            throw new IllegalArgumentException("command cannot be null.");
        }
        String[] cmd = null;
        if (parameterArray != null) {
            cmd = new String[parameterArray.length + 1];
            cmd[0] = command;
            for (int i = 0; i < parameterArray.length; i++) {
                cmd[i + 1] = parameterArray[i];
            }
        } else {
            cmd = new String[] { command };
        }

        /*
         * Windows does not permit execution of batch files directly through
         * Runtime.exec. We have to wrap the call to the batch file with a call
         * to "cmd /c".
         */
        if (machine.getOperatingSystem() == OperatingSystem.WINDOWS && WLP_CYGWIN_HOME == null) {
            if (!cmd[0].startsWith("cmd /c")) {
                String[] tmp = new String[cmd.length + 2];
                tmp[0] = "cmd";
                tmp[1] = "/c";
                for (int i = 0; i < cmd.length; i++)
                    tmp[i + 2] = cmd[i];
                cmd = tmp;
            }
        } else {
            if (!cmd[0].startsWith("sh -c")) {
                String[] tmp = new String[3];
                String parsedCommand = shArrayTransform(cmd);
                tmp[0] = WLP_CYGWIN_HOME == null ? "sh" : WLP_CYGWIN_HOME + "/bin/sh";
                tmp[1] = "-c";
                tmp[2] = parsedCommand;
                cmd = tmp;
            }
        }

        // By default, the subprocess should inherit the working directory of
        // the current process
        File dir = null;
        if (workDir != null) {
            dir = new File(workDir);
            if (!dir.isDirectory()) {
                dir = null;
            }
        }

        /*
         * make sure SystemRoot is defined. For some reason on WindowsXP this
         * disappears when passing in evn variables causing socket issues
         */
        if (envp != null
            && machine.getOperatingSystem() == OperatingSystem.WINDOWS) {
            boolean systemRootFound = false;
            for (Object p : envp.keySet()) {
                // first check if the user defined it. most likely not
                if (envp.get(p) != null
                    && ((String) envp.get(p)).startsWith("SystemRoot")) {
                    systemRootFound = true;
                    break;
                }
            }
            if (!systemRootFound) {
                // not user defined, so lets make sure it stays set. most likely
                // scenario
                String systemRoot = System.getenv("SystemRoot");
                envp.setProperty("SystemRoot", systemRoot);
            }
        }

        //Create the ProcessBuilder object here as we need the environment property map
        ProcessBuilder pb = new ProcessBuilder();

        //Inject the the environment properties
        if (envp != null) {
            Map<String, String> tmp = pb.environment(); //This will already have the system environment
            for (Map.Entry<Object, Object> p : envp.entrySet()) {
                tmp.put((String) p.getKey(), (String) p.getValue());
            }
        }

        // execute the command
        pb.command(cmd);
        if (dir != null)
            pb.directory(dir);
        pb.redirectErrorStream(true);
        Process proc = pb.start(); // Runtime.getRuntime().exec(cmd, envVars,
        // dir);

        StreamGobbler outputGobbler = null;

        // Is this a good place to encode the assumption that on z/OS the console.log is going to be
        // produced in EBCDIC even if the default charset is an ASCII one?
        if (async && machine.getOperatingSystem() == OperatingSystem.ZOS) {
            outputGobbler = new StreamGobbler(proc.getInputStream(), stdOutStream, true, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            outputGobbler = new StreamGobbler(proc.getInputStream(), stdOutStream, async);
        }

        // listen to subprocess output
        outputGobbler.start();

        if (async) {
            Log.exiting(c, method);
            return -1;
        }

        // wait till completion
        proc.waitFor();
        // let the streams catch up (critical step)
        outputGobbler.doJoin();
        Log.exiting(c, method);
        return proc.exitValue();
    }

    /**
     * This method takes the parameter array and turns it into one long string for
     * use by the sh -c part of running the command locally on linux
     *
     * @param cmd
     * @return
     */
    private static String shArrayTransform(String[] cmd) {
        String returned = "";
        for (int i = 0; i < cmd.length; i++) {
            returned += cmd[i] + " ";
        }
        returned = returned.substring(0, (returned.length() - 1)); //should remove the space at the end;
        return returned;
    }

    public static synchronized AsyncProgramOutput executeCommandAsync(Machine machine,
                                                                      String cmd, String[] parameters, String workDir,
                                                                      Properties envVars) throws Exception {
        String[] tmp = new String[parameters.length + 1];
        tmp[0] = cmd;
        for (int i = 0; i < parameters.length; i++) {
            tmp[i + 1] = parameters[i];
        }
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(tmp);

        /*
         * What we're aiming for here is a command which doesn't block,
         * but we don't really care about its life continuing past the life
         * of this process (with nohup). We also don't think wsscript is
         * a great idea, since it pops up firewall questions, which isn't
         * really viable on an unattended machine.
         * ProcessBuilder.start() should be sufficient for our needs.
         */
        Process process = builder.start();
        return new AsyncProgramOutput(cmd, parameters, process);

    }

    public static boolean exists(RemoteFile file) throws Exception {
        File f = new File(file.getAbsolutePath());
        return f.exists();
    }

    public static String getOSName(Machine machine) throws Exception {
        return System.getProperty("os.name");
    }

    public static boolean isDirectory(RemoteFile dir) throws Exception {
        File f = new File(dir.getAbsolutePath());
        return f.isDirectory();
    }

    public static boolean isFile(RemoteFile file) throws Exception {
        File f = new File(file.getAbsolutePath());
        return f.isFile();
    }

    public static String[] list(RemoteFile file, boolean recursive) throws Exception {
        File f = new File(file.getAbsolutePath());
        List<String> list = new ArrayList<String>();
        String[] children = listDirectory(f);
        for (int i = 0; i < children.length; i++) {
            File child = new File(f, children[i]);
            list.add(child.getAbsolutePath());
            if (recursive && child.isDirectory()) {
                RemoteFile childKey = new RemoteFile(file.getMachine(), child.getAbsolutePath());
                String[] grandchildren = list(childKey, recursive);
                for (int k = 0; k < grandchildren.length; ++k) {
                    list.add(grandchildren[k]);
                }
            }
        }
        return list.toArray(new String[0]);
    }

    private static final String[] listDirectory(File directory) {
        File[] files = directory.listFiles();
        String[] result = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            result[i] = files[i].getName();
        }
        return result;
    }

    public static boolean mkdir(RemoteFile dir) throws Exception {
        File f = new File(dir.getAbsolutePath());
        return f.mkdir();
    }

    public static boolean mkdirs(RemoteFile dir) throws Exception {
        File f = new File(dir.getAbsolutePath());
        return f.mkdirs();
    }

    public static boolean isConnected() throws Exception {
        return true;
    }

    public static String getTempDir() throws Exception {
        return System.getProperty("java.io.tmpdir");
    }

    public static InputStream openFileForReading(RemoteFile file) throws Exception {
        return new FileInputStream(file.getAbsolutePath());
    }

    public static OutputStream openFileForWriting(RemoteFile file,
                                                  boolean append) throws Exception {
        return new FileOutputStream(file.getAbsolutePath(), append);
    }

    public static ProgramOutput killProcess(Machine machine, int processId) throws Exception {
        final String method = "killProcess";
        Log.entering(c, method, new Object[] { processId });
        String cmd = null;
        String[] parameters = null;
        if (machine.getOperatingSystem() != OperatingSystem.WINDOWS) {
            cmd = "kill" + " -9 " + processId;
            // Defect 42663: For some reason passing the kill command with the parameters does not seem to work.
            // I do not understand why but giving the whole already built command does work...
            // This may need investigation later but for the time being this appears to fix the non-killing problem
            //  parameters = new String[] { "-9", "" + processId };
        } else {
            cmd = "taskkill";
            parameters = new String[] { "/F", "/PID", "" + processId };
        }
        Log.finer(c, method, cmd, parameters);
        return executeCommand(machine, cmd, parameters, null, null);
    }

    public static RemoteFile ensureFileIsOnMachine(Machine target,
                                                   RemoteFile file) throws Exception {
        if ((target.getHostname() != null && !target.isLocal())
            || (file.getMachine().getHostname() != null && !file.getMachine().isLocal()))
            throw new Exception("A remote provider is required to transfer files between physical machines.");
        // If we're here, the file already exists on the local machine
        return file;
    }

    public static Date getDate(Machine machine) throws Exception {
        System.out.println(System.currentTimeMillis());
        return new Date(System.currentTimeMillis());
    }

}

/**
 * This Runnable basically just copies the input from one stream and copies it
 * to the output of another. It is designed to copy the output from the Orca
 * server process to a file, although it is more general than this.
 *
 * <p>
 * It is critical that it reads from an InputStream, and the InputStream from
 * the Orca Process, because the other stream types perform buffering this can
 * cause all kinds of problems like, causing hangs in the child process because
 * we did not read all the content, or only part of the output being captured to
 * file.
 * </p>
 */
class InputCopier implements Runnable {

    /** The input stream to read from */
    private final InputStream input;
    /** The output stream to write to */
    private final OutputStream output;

    /**
     * @param serverOut
     *            the output from the Orca process, ownership is passed and this
     *            stream is closed at the end of the thread
     * @param writer
     *            the file to write to, ownership is passed and this stream is
     *            closed at the end of the thread
     */
    public InputCopier(InputStream serverOut, OutputStream writer) {
        input = serverOut;
        output = writer;
    }

    @Override
    public void run() {
        int len;

        // create a byte buffer
        byte[] buffer = new byte[1024];

        try {
            // read in some data into the buffer, keep doing this until
            // we get -1 back which generally indicates that stream is
            // closed.
            while ((len = input.read(buffer)) != -1) {
                // and write and flush
                output.write(buffer, 0, len);
                output.flush();
            }

            // close the streams
            output.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
