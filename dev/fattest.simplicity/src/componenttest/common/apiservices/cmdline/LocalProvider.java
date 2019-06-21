/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    private static final Class<? extends LocalProvider> CLASS = LocalProvider.class;

    // System helpers ...

    public static Date getDate(Machine machine) throws Exception {
        return new Date( System.currentTimeMillis() );
    }

    public static String getOSName(Machine machine) throws Exception {
        return System.getProperty("os.name");
    }

    public static String getTempDir() throws Exception {
        return System.getProperty("java.io.tmpdir");
    }

    // Remote connection status ...

    /**
     * Tell if this provider is connected.  Always answer true, since this
     * is a local provider and is always connected.
     *
     * @return True or false telling if this provider is connected.  This
     *     implementation always answers true.
     *
     * @throws Exception Thrown if an error occurs testing the connection.
     */
    public static boolean isConnected() throws Exception {
        return true;
    }

    // Simple file operations ...

    public static boolean mkdir(RemoteFile remoteFile) throws Exception {
        return remoteFile.asFile().mkdir();
    }

    public static boolean mkdirs(RemoteFile remoteFile) throws Exception {
        return remoteFile.asFile().mkdirs();
    }

    public static InputStream openFileForReading(RemoteFile remoteFile) throws Exception {
        return new FileInputStream( remoteFile.asFile() );
    }

    public static OutputStream openFileForWriting(RemoteFile remoteFile, boolean append) throws Exception {
        return new FileOutputStream( remoteFile.asFile(), append );
    }

    public static boolean exists(RemoteFile remoteFile) throws Exception {
        return remoteFile.asFile().exists();
    }

    public static boolean isDirectory(RemoteFile remoteFile) throws Exception {
        return remoteFile.asFile().isDirectory();
    }

    public static boolean isFile(RemoteFile remoteFile) throws Exception {
        return remoteFile.asFile().isFile();
    }

    public static long lastModified(RemoteFile remoteFile) {
    	return remoteFile.asFile().lastModified();
    }

    public static long length(RemoteFile remoteFile) {
    	return remoteFile.asFile().length();
    }

    // Possible orphan ...

    /**
     * Tell if a machine / host is local: That is, if it has a null host name,
     * or if the host is the current host.
     *
     * See {@link Machine#getHostname()} and {@link Machine#isLocal()}.
     *
     * @param host The machine which is to be tested.
     *
     * @return True or false telling if the machine is local.
     */
    public static boolean isLocal(Machine host) {
        return ( (host.getHostname() == null) || host.isLocal() );
    }

    /**
     * This operation does not seem to be in use, and has limited effect:
     * The implementation verifies that the host and file are both local.
     * If either is non-local, an exception is thrown.  If both are local,
     * the file is returned.
     *
     * The test seems to ensure that the t
     * 
     * @param targetHost The host that is the target of an operation.
     * @param targetFile The file that is the target of an operation.
     *
     * @return The target file.
     *
     * @throws Exception Thrown if either the host or the file is non-local.
     */
    public static RemoteFile ensureFileIsOnMachine(Machine targetHost, RemoteFile targetFile) throws Exception {
        if ( !targetHost.isLocal() || !targetFile.getMachine().isLocal() ) {
            throw new Exception("A remote provider is required to transfer files between physical machines.");
        }
        return targetFile;
    }

    // Move and rename

    /**
     * Attempt to move a file.  Answer true or false telling if the move was successful.
     *
     * Expected exceptions from the move operation are captured and false is returned.
     *
     * The move is performed as an atomic operation.  Time attributes of the file are preserved.
     * See {@link Files#move}. and {@link StandardCopyOption#ATOMIC_MOVE}.
     *
     * @param src The source file.
     * @param dest The destination file.
     *
     * @return True or false telling if the move was successful.
     *
     * @throws Exception Thrown if the move failed unexpectedly.
     */
    public static boolean move(RemoteFile src, RemoteFile dest) throws Exception {
        String methodName = "move";
        try {
            return ( Files.move( src.asPath(), dest.asPath(), StandardCopyOption.ATOMIC_MOVE) != null );
        } catch ( IOException e ) {
            Log.error(CLASS,  methodName, e, "Failed to move " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            return false;
        }
    }

    /**
     * Attempt to rename a file.  Answer true or false telling if the rename was successful.
     *
     * Expected exceptions from the rename operation are captured and false is returned.
     *
     * The operation is not necessarily atomic.  See {@link File#renameTo}.
     *
     * @param src The source file.
     * @param dest The destination file.
     *
     * @return True or false telling if the rename was successful.
     *
     * @throws Exception Thrown if the rename failed unexpectedly.
     */
    public static boolean rename(RemoteFile src, RemoteFile dest) {
        return src.asFile().renameTo(dest.asFile());
    }

    /**
     * Attempt to copy a file.  Answer true or false telling if the copy was successful.
     *
     * If the copy destination is a directory, copy the source file as an immediate child
     * of the destination.  The copy destination may be a simple file, in which case the
     * copy destination will be overwritten.
     *
     * Expected exceptions from the copy operation are captured and false is returned.
     *
     * The operation not generally atomic.
	 *
     * @param src The source file.
     * @param dest The destination file.
     * @param binary Control parameter: If true, perform a binary copy.  If
     *     false perform a text copy.  Currently ignored.  Always perform a
     *     binary copy.
     * @return True or false telling if the copy was successful.
     *
     * @throws Exception Thrown if the copy failed unexpectedly.
     */
    public static boolean copy(RemoteFile src, RemoteFile dest, boolean binary) throws Exception {
        String methodName = "copy";
        try {
            long bytesWritten = basicCopy(src, dest);
            Log.info(CLASS, methodName, "Copied " + bytesWritten + " bytes from " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            return true;
        } catch ( IOException e ) {
            Log.error(CLASS,  methodName, e, "Failed to copy from " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
            return false;
        }
    }

    private static long basicCopy(RemoteFile src, RemoteFile dest) throws IOException {
        File srcFile = src.asFile();
        File destFile = dest.asFile();

        if ( destFile.isDirectory()) {
            destFile = new File(destFile, srcFile.getName());        
        }

        long totalRead = 0L;

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = new FileInputStream(srcFile); // throws IOException
            outputStream = new FileOutputStream(destFile); // throws IOException

            int read;
            byte[] buffer = new byte[16 * 1024];
            while ((read = inputStream.read(buffer)) != -1) { // throws IOException
                totalRead += read;
                outputStream.write(buffer, 0, read); // throws IOException
            }

        } finally {
            if ( inputStream != null ) {
                try {
                    inputStream.close();
                } catch ( IOException e ) {
                    Log.warning(CLASS, "Failed to close " + src.getAbsolutePath() + ": " + e.getMessage());
                }
            }
            if ( outputStream != null ) {
                try {
                    outputStream.close();
                } catch ( IOException e ) {
                    Log.warning(CLASS, "Failed to close " + dest.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        return totalRead;
    }

    //

    /**
     * Delete the local file which is indicated by the absolute path
     * of a remote file.
     *
     * A failure to delete any nested child file will cause the entire
     * deletion to fail.  However, the delete operation will attempt to
     * delete all nested children despite any failures on particular
     * children.
     *
     * @param remoteFile The remote file which is to be deleted.
     *
     * @return True or false telling if the file was deleted.
     *     True if the file does not exist.
     *
     * @throws Exception Thrown if the delete unexpectedly failed.
     */
    public static boolean delete(RemoteFile remoteFile) throws Exception {
    	return delete( remoteFile.asFile() );
    }

    /**
     * Recursively delete a file.
     *
     * Report failure, but continue deleting other children when a
     * particular child delete fails.
     *
     * @param file The file which is to be deleted.
     *
     * @return True or false telling if the file was deleted.
     *
     * @throws Exception Thrown if the deleted failed for an unexpected
     *     reason.
     */
    private static boolean delete(File file) throws Exception {
    	String methodName = "delete";

    	if ( !file.exists() ) {
    		return false;
    	}

    	Path path = file.toPath();

    	if ( file.isDirectory() ) {
        	File failedChild = null;

        	for ( File child : file.listFiles() ) {
    			if ( !delete(child) ) {
    				if ( failedChild == null ) {
    					failedChild = child;
    				}
    			}
    		}

    		if ( failedChild != null ) {
        		Log.warning(CLASS, methodName + ": Cannot delete " + path + ": Failed to delete child " + failedChild.toPath());
    		}
    		return false;
    	}

    	try {
    		Files.delete(path);
    		return true;
    	} catch ( IOException e ) {
    		Log.warning(CLASS, methodName + ": Failed to delete " + path);
    		return false;
    	}

    }

    // File listing ...

    /**
     * List the files beneath a specified file.  Conditionally, recurse.  Answer
     * the collected absolute paths of the child files.
     *
     * Answer an empty array if the target file is not a directory.  Do not collect
     * the path to the target file.
     *
     * @param remoteFile The file which is to be listed.
     * @param recurse Control parameter: Tells whether to list children recursively.
     *
     * @return The collect absolute paths of the listed files.
     *
     * @throws Exception Thrown if an error occurs obtaining the listing.
     */
    public static String[] list(RemoteFile remoteFile, boolean recurse) throws Exception {
        List<String> collectedPaths = new ArrayList<String>();
        collectPaths( remoteFile.asFile(), recurse, collectedPaths );
        return collectedPaths.toArray( new String[ collectedPaths.size() ] );
    }

    /**
     * List the files beneath a specified file.  Do not recurse.  Answer
     * the names of the child files.
     *
     * Answer null if the remote file is not a directory.
     *
     * @param remoteFile The file which is to be listed.
     *
     * @return The names of child files of the remote file.
     *
     * @throws Exception Thrown if an error occurs obtaining the listing.
     */
    public static String[] list(RemoteFile remoteFile) throws Exception {
		return remoteFile.asFile().list();
    }

    /**
     * Collect the absolute paths of the children of a target file.  Place no paths
     * if the target file is not a directory.
     *
     * Do not collect the path to the target file.
     *
     * @param file The file for which to collect absolute paths.
     * @param recurse Control parameter: If true, recurse when collecting paths.
     * @param collectedPaths The collected absolute paths.
     *
     * @throws Exception Thrown if an error occurs obtaining child paths.
     */
    public static void collectPaths(File file, boolean recurse, List<String> collectedPaths) throws Exception {
        File[] children = file.listFiles();
        if ( children == null ) {
            Log.warning(CLASS,  "Request to list non-directory " + file.getAbsolutePath());
            return;
        }

        for ( File child : children ) {
            collectedPaths.add( child.getAbsolutePath() );
            if ( recurse && child.isDirectory() ) {
                collectPaths(child, recurse, collectedPaths);
            }
        }
    }

    // Process execution utility ...

    public static ProgramOutput executeCommand(
        Machine host,
        String cmd, String[] parmArray, String workPath, Properties envp) throws Exception {

        ByteArrayOutputStream stdOutStream = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErrStream = new ByteArrayOutputStream();
        int rc = execute(host, cmd, parmArray, envp, workPath, stdOutStream, stdErrStream, IS_NOT_ASYNC);
        return new ProgramOutput(cmd, rc, stdOutStream.toString(), stdErrStream.toString());
    }

    public static void executeCommandAsync(
        Machine host,
        String cmd, String[] parmArray, String workPath, Properties envp,
        OutputStream stdOutStream) throws Exception {

        execute(host, cmd, parmArray, envp, workPath, stdOutStream, null, IS_ASYNC);
        // null stdErr stream
    }

    private static final String WLP_CYGWIN_HOME = System.getenv("WLP_CYGWIN_HOME");
    protected static final String EBCDIC_CHARSET_NAME = "IBM1047";

    private static final boolean IS_ASYNC = true;
    private static final boolean IS_NOT_ASYNC = false;

    private static final int execute(
        Machine host,
        String cmd, String[] parmArray, Properties envp, String workPath,
        OutputStream stdOutStream, OutputStream stdErrStream,
        boolean async) throws Exception {

        String methodName = "execute";
        Log.entering(CLASS, methodName, "Async " + async);

        if ( cmd == null ) {
            throw new IllegalArgumentException("Null command");
        }

        String[] cmdArray = null;
        if ( parmArray != null ) {
            cmdArray = new String[parmArray.length + 1];
            cmdArray[0] = cmd;
            for ( int parmNo = 0; parmNo < parmArray.length; parmNo++ ) {
                cmdArray[parmNo + 1] = parmArray[parmNo];
            }
        } else {
            cmdArray = new String[] { cmd };
        }

        // Windows does not permit execution of batch files directly through
        // Runtime.exec. We have to wrap the call to the batch file with a call
        // to "cmd /c".

        if ( (host.getOperatingSystem() == OperatingSystem.WINDOWS) && (WLP_CYGWIN_HOME == null) ) {
            if ( !cmdArray[0].startsWith("cmd /c") ) {
                String[] adjustedCmdArray = new String[cmdArray.length + 2];
                adjustedCmdArray[0] = "cmd";
                adjustedCmdArray[1] = "/c";
                for ( int elementNo = 0; elementNo < cmdArray.length; elementNo++ ) {
                    adjustedCmdArray[elementNo + 2] = cmdArray[elementNo];
                }
                cmdArray = adjustedCmdArray;
            }
        } else {
            if ( !cmdArray[0].startsWith("sh -c") ) {
                cmdArray = new String[] {
                    ((WLP_CYGWIN_HOME == null) ? "sh" : (WLP_CYGWIN_HOME + "/bin/sh")),
                    "-c",
                    collapse(cmdArray) };
            }
        }

        File workDir;
        if ( workPath != null ) {
            workDir = new File(workPath);
            if ( !workDir.isDirectory() ) {
                workDir = null;
            }
        } else {
            workDir = null; // Inherit
        }

        // Make sure SystemRoot is defined.
        // On WindowsXP this disappears, causing socket issues.

        if ( (envp != null) && (host.getOperatingSystem() == OperatingSystem.WINDOWS) ) {
            boolean systemRootFound = false;
            for ( Object envKey : envp.keySet() ) {
                Object envValue = envp.get(envKey);
                if ( (envValue != null) && ((String) envValue).startsWith("SystemRoot") ) {
                    systemRootFound = true;
                    break;
                }
            }

            if ( !systemRootFound ) {
                String systemRoot = System.getenv("SystemRoot");
                envp.setProperty("SystemRoot", systemRoot);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder();

        if ( envp != null ) {
            Map<String, String> pbEnv = processBuilder.environment();
            for ( Map.Entry<Object, Object> envpEntry : envp.entrySet() ) {
                pbEnv.put( (String) envpEntry.getKey(), (String) envpEntry.getValue() );
            }
        }

        processBuilder.command(cmdArray);

        if ( workDir != null ) {
            processBuilder.directory(workDir);
        }

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        // Runtime.getRuntime().exec(cmd, envVars, dir);

        StreamGobbler outputGobbler = null;

        // TODO: Is this a good place to encode the assumption that
        // on z/OS the console.log is going to be produced in EBCDIC
        // even if the default character set is ASCII?

        // TODO: Why is the character set conditioned on 'async'?

        InputStream processInput = process.getInputStream();
        if ( async && (host.getOperatingSystem() == OperatingSystem.ZOS) ) {
            outputGobbler = new StreamGobbler(processInput, stdOutStream, true, Charset.forName(EBCDIC_CHARSET_NAME));
        } else {
            outputGobbler = new StreamGobbler(processInput, stdOutStream, async);
        }
        outputGobbler.start();

        if ( async ) {
            Log.exiting(CLASS, methodName);
            return -1; // Async launch, so don't have the actual return value.
        }

        process.waitFor();
        outputGobbler.doJoin();

        int exitValue = process.exitValue();

        Log.exiting(CLASS, methodName);
        return exitValue;
    }

    private static String collapse(String[] cmd) {
        StringBuilder cmdText = new StringBuilder();
        for ( int elementNo = 0; elementNo < cmd.length; elementNo++ ) {
            if ( elementNo > 0 ) {
                cmdText.append(' ');
            }
            cmdText.append( cmd[elementNo] );
        }
        return cmdText.toString();
    }

    public static synchronized AsyncProgramOutput executeCommandAsync(
        Machine machine,
        String cmd, String[] parmArray, String workPath, Properties envp)
        throws Exception {

        String[] cmdArray = new String[parmArray.length + 1];
        cmdArray[0] = cmd;
        for  ( int elementNo = 0; elementNo < parmArray.length; elementNo++ ) {
            cmdArray[elementNo + 1] = parmArray[elementNo];
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmdArray);

        // What we're aiming for here is a command which doesn't block,
        // but we don't really care about its life continuing past the life
        // of this process (with nohup). We also don't think wsscript is
        // a great idea, since it pops up firewall questions, which isn't
        // really viable on an unattended machine.
        // ProcessBuilder.start() should be sufficient for our needs.

        Process process = processBuilder.start();
        return new AsyncProgramOutput(cmd, parmArray, process);

    }

    public static ProgramOutput killProcess(Machine machine, int processId) throws Exception {
        String methodName = "killProcess";
        Log.entering(CLASS, methodName, new Object[] { processId });

        // Defect 42663: For some reason passing the kill command with the parameters does
        // not seem to work.
        //
        // I do not understand why, but, giving the whole already built command does work.
        //
        // This may need investigation later but for the time being this appears to fix
        //the non-killing problem.        

        String cmd;
        String[] parmArray;

        if ( machine.getOperatingSystem() != OperatingSystem.WINDOWS ) {
            cmd = "kill" + " -9 " + processId;
            parmArray = null;
            // parmArray = new String[] { "-9", "" + processId };
        } else {
            cmd = "taskkill";
            parmArray = new String[] { "/F", "/PID", "" + processId };
        }

        Log.finer(CLASS, methodName, cmd, parmArray);
        ProgramOutput output = executeCommand(machine, cmd, parmArray, null, null);

        Log.exiting(CLASS, methodName);
        return output;
    }
}

/* InputCopier appears to be obsolete. */

/**
 * This Runnable basically just copies the input from one stream and copies it
 * to the output of another. It is designed to copy the output from the Orca
 * server process to a file, although it is more general than this.
 *
 * It is critical that it reads from an InputStream, and the InputStream from
 * the Orca Process, because the other stream types perform buffering this can
 * cause all kinds of problems like, causing hangs in the child process because
 * we did not read all the content, or only part of the output being captured to
 * file.
 */
class InputCopier implements Runnable {
    private final InputStream input;
    private final OutputStream output;

    public InputCopier(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        byte[] transferBuffer = new byte[16 * 1024];

        try {
            try {
                int bytesRead;
                while ( (bytesRead = input.read(transferBuffer)) != -1 ) {
                    output.write(transferBuffer, 0, bytesRead);
                    output.flush();
                }
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } finally {
            try {
                output.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
            try {
                input.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
}
