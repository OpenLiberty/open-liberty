/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.RemoteFile.Operation;
import com.ibm.websphere.simplicity.log.Log;
//open-liberty uses 'LocalProvider', where-as WS-CD-Open uses 'RXAProvider'.
import componenttest.common.apiservices.cmdline.LocalProvider;

/**
 * Represents a file or directory on a {@link Machine}. This file or directory
 * may or may not actually exist on the {@link Machine}. When an instance of
 * RemoteFile is created the path name is modified to be of the form
 * /dir/dir2/file.
 */
public class RemoteFile {
    @SuppressWarnings("rawtypes")
    private static final Class c = RemoteFile.class;

    // Retry utility ...

    /**
     * A more accurate version of {@link Thread#sleep}: Use nano-second units,
     * and do not rely on the actual sleep duration being at least the requested
     * duration.
     *
     * Thread.sleep() is repeated until System.nanoTime() shows that the thread
     * has waited at least the specified time in milliseconds.
     *
     * @param requestedNs The requested sleep time, in nano-seconds.
     *
     * @return The time slept, in nano-seconds.
     *
     * @throws InterruptedException Thrown if if the sleep is
     *     interrupted.
     */
    public static long sleep(long requestedNs) throws InterruptedException {
        long startNs = System.nanoTime();

        long elapsedNs = 0L;
        long remainingNs = requestedNs;

        while ( remainingNs > 0 ) {
            TimeUnit.NANOSECONDS.sleep(remainingNs); // throws InterruptedException

            elapsedNs = System.nanoTime() - startNs;
            remainingNs = requestedNs - elapsedNs;
       }

        return elapsedNs;
    }

    /**
     * The standard retry interval for delete and rename operations.
     *
     * This is based on the artifact file system minimum
     * retry interval of 200 milliseconds, per
     * <code>
     *   open-liberty/dev/com.ibm.ws.artifact.zip/src/
     *   com/ibm/ws/artifact/zip/cache/
     *   ZipCachingProperties.java
     * </code>
     *
     * The default largest pending close time is specified
     * by property <code>zip.reaper.slow.pend.max</code>.
     * 
     * The current largest pend time is 200 milliseconds.  For
     * extra safety, the retry interval is set to twice this value.
     * 
     * Note: This does not handle when the server is prevented
     * from running.  In a typical case, where application files
     * are updated after accessing the server files, if the server
     * is prevented from running while an application file has a
     * pending close, the usual pend time may be exceeded without
     * the file being closed.
     */
    public static final long STANDARD_RETRY_INTERVAL_NS =
        TimeUnit.MILLISECONDS.toNanos(200 * 2);

    // Increase this to 0.1s instead of 0.05s.  The sleep granularity
    // on windows is too small to reliably handle 0.05s. 

    public static final long STANDARD_RETRY_PARTIAL_INTERVAL_NS =
        TimeUnit.MILLISECONDS.toNanos(100); // 0.1s

    public interface Operation {
        public boolean act() throws Exception;
    }

    /**
     * Attempt an operation.  Retry the operation, possibly several times,
     * across a specified retry interval.
     *
     * @param op The operation which is being performed.
     * @param fullRetryNs The full retry duration.
     *
     * @return True or false telling if the operation was successful.
     *
     * @throws Exception
     */
    private boolean retry(Operation op, long fullRetryNs) throws Exception {
        // First try is immediate. 
        if ( op.act() ) {
            return true;
        }

        // Don't retry unless a retry interval was specified.
        if ( fullRetryNs == 0 ) {
            return false;
        }

        // Make sure the retry interval is usable.
        if ( fullRetryNs < 0 ) {
            throw new IllegalArgumentException("Full retry interval [ " + fullRetryNs + " ] for [ " + getAbsolutePath() + " ] is less then 0"); 
        }

        // Delay and retry, in increments of the partial retry interval, until
        // at least the full retry interval has elapsed.

        // Time accounting here has ambiguity: Should operation time be considered a part of
        // the time slept?
        //
        // For the intended cases, file deletion and file renaming, the operation time can be
        // considerable.

        long finalNs = System.nanoTime() + fullRetryNs;
        do {
            sleep(STANDARD_RETRY_PARTIAL_INTERVAL_NS); // throws InterruptedException
            if ( op.act() ) {
                return true;
            }
        } while ( System.nanoTime() < finalNs );

        return false;
    }

    //

    private final Machine host;
    private String filePath;
    private final String parentPath;
    private String name;
    private File localFile; // non-null only if Machine.isLocal() returns true
    private final Charset encoding;

    /**
     * Construct an instance based on the fully qualified path of the remote
     * file.
     * 
     * @param host
     *            The {@link Machine} where this file is physically located
     * @param filePath
     *            The fully qualified (absolute) path of the file
     */
    public RemoteFile(Machine host, String filePath) {
        this(host, filePath, Charset.defaultCharset());
    }

    /**
     * Construct an instance based on the fully qualified path of the remote
     * file.
     * 
     * @param host
     *            The {@link Machine} where this file is physically located
     * @param filePath
     *            The fully qualified (absolute) path of the file
     * @param encoding
     *            The character set the file is encoded in
     */
    public RemoteFile(Machine host, String filePath, Charset encoding) {
        this.host = host;
        this.filePath = convertPath(filePath);
        this.parentPath = convertPath(getParentPath(filePath));
        this.encoding = encoding;
        init();
    }

    /**
     * Construct an instance based on the parent of the remote file.
     * 
     * @param host
     *            The {@link Machine} where this file is physically located
     * @param parent
     *            The remote file's parent directory
     * @param name
     *            The name of the file
     */
    public RemoteFile(Machine host, RemoteFile parent, String name) {
        this(host, parent, name, Charset.defaultCharset());
    }

    /**
     * Construct an instance based on the parent of the remote file.
     * 
     * @param host
     *            The {@link Machine} where this file is physically located
     * @param parent
     *            The remote file's parent directory
     * @param name
     *            The name of the file
     * @param encoding
     *            The character set the file is encoded in
     */
    public RemoteFile(Machine host, RemoteFile parent, String name, Charset encoding) {
        if (parent.getAbsolutePath().endsWith("/")
            || parent.getAbsolutePath().endsWith("\\")) {
            this.filePath = convertPath(parent.getAbsolutePath() + name);
        } else {
            this.filePath = convertPath(parent.getAbsolutePath() + "/" + name);
        }
        this.parentPath = parent.getAbsolutePath();
        this.name = name;
        this.host = host;
        this.encoding = encoding;
        init();
    }

    /**
     * Construct an instance based on the parent of the remote file. Assumes that the new instance resides on the same machine as the parent file.
     * 
     * @param parent
     *            The remote file's parent directory
     * @param name
     *            The name of the file
     */
    public RemoteFile(RemoteFile parent, String name) {
        this(parent.getMachine(), parent, name, Charset.defaultCharset());
    }

    /**
     * Construct an instance based on the parent of the remote file. Assumes that the new instance resides on the same machine as the parent file.
     * 
     * @param parent
     *            The remote file's parent directory
     * @param name
     *            The name of the file
     * @param encoding
     *            The character set the file is encoded in
     */
    public RemoteFile(RemoteFile parent, String name, Charset encoding) {
        this(parent.getMachine(), parent, name, encoding);
    }

    /**
     * Get the {@link Machine} of this RemoteFile
     * 
     * @return The {@link Machine}
     */
    public Machine getMachine() {
        return host;
    }

    /**
     * Returns a String representation the parent directory of this file, or
     * null if this pathname does not name a parent directory.
     * 
     * @return A String representation of this remote file's parent directory
     */
    public String getParent() throws Exception {
        RemoteFile parent = getParentFile();
        if (parent == null) {
            return null;
        } else {
            return parent.getAbsolutePath();
        }
    }

    /**
     * Returns a RemoteFile representation of the parent directory of this file
     * or null if this pathname does not name a parent directory.
     * 
     * @return A RemoteFile representation of the parent directory.
     * @throws Exception
     */
    public RemoteFile getParentFile() throws Exception {
        String path = parentPath;
        if (path == null) {
            return null;
        }
        return new RemoteFile(host, path);
    }

    /**
     * Returns the absolute pathname string of this RemoteFile in the form
     * /dir1/dir2/file
     * 
     * @return The absolute pathname of this RemoteFile
     */
    public String getAbsolutePath() {
        return filePath;
    }

    /**
     * <p>
     * Searches a directory for file names matching a numeric prefix, and
     * returns a new File instance representing a new (unique) file in that
     * directory. Successive calls to this method for the same directory and
     * prefix will produce an alphabetically sorted list of child files. For example:
     * prefix01_nameA, prefix02_nameB, prefix03_nameC, prefix04_nameC, etc. Note
     * that the returned File will not yet exist on the file system.
     * </p>
     * <p>
     * This method is not thread-safe.
     * </p>
     * 
     * @param prefix
     *            an optional String that proceeds ordering information. All
     *            characters must match: [a-zA-Z_0-9\\.]
     * @param digits
     *            the number of digits to use for generated identifiers
     * @param suffix
     *            the optional name of the desired file, without any numeric identifier.
     *            All characters must match: [a-zA-Z_0-9\\.]
     * @return a new RemoteFile representing a unique child of the parent. The file must be created before use.
     * @throws IllegalArgumentException
     *             if this instance does not denote a directory,
     *             if input arguments are invalid,
     *             or if the file cannot be created
     */
    public RemoteFile getOrderedChild(String prefix, int digits, String suffix) throws Exception {
        RemoteFile[] children = list(false);
        if (children == null) {
            throw new IllegalArgumentException("Does not denote a directory: " + this);
        }
        Pattern wordCharacters = Pattern.compile("[\\w\\.]*"); // zero or more characters matching [a-zA-Z_0-9\\.].  The '-' character is not allowed!
        if (suffix != null) {
            if (!wordCharacters.matcher(suffix).matches()) { // if the file name contains a non-word character
                throw new IllegalArgumentException("Invalid characters detected in proposed file name: " + suffix);
            }
        }
        int prefixLength = 0;
        if (prefix != null) {
            if (!wordCharacters.matcher(prefix).matches()) { // if the file name contains a non-word character
                throw new IllegalArgumentException("Invalid characters detected in file prefix: " + prefix);
            }
            prefixLength = prefix.length();
        }
        long highest = 0;
        for (RemoteFile child : children) {
            if (child == null) {
                continue; // ignore children with a null name
            }
            String childName = child.getName();
            if (prefixLength > 0 && !childName.startsWith(prefix)) {
                continue; // ignore children missing our prefix in their name
            }
            childName = childName.substring(prefixLength); // remove prefix from the name (if it exists)
            int dashIndex = childName.indexOf("-");
            if (dashIndex > -1) {
                childName = childName.substring(0, dashIndex); // remove suffix from the name (if it exists)
            }
            long number;
            try {
                number = Long.parseLong(childName);
            } catch (Exception e) {
                continue; // ignore children without a number between the prefix and suffix
            }
            if (number > highest) {
                highest = number;
            }
        }
        StringBuilder newName = new StringBuilder();
        if (prefixLength > 0) {
            newName.append(prefix);
        }
        newName.append(zeroPad(highest + 1, digits));
        if (suffix != null) {
            newName.append("-");
            newName.append(suffix);
        }
        return new RemoteFile(getMachine(), this, newName.toString());
    }

    /**
     * Prepends zeros to the left of the input number to ensure that the input
     * number is a total of <code>width</code> digits. Truncates the input
     * number if it has more than <code>width</code> digits.
     * 
     * @param number
     *            a positive integer (negative integers cause problems with odd
     *            widths)
     * @param width
     *            the number of characters that you want in a String
     *            representation of <code>number</code>; must be a positive
     *            integer smaller than 18 (larger numbers cause an overflow
     *            issue)
     * @return a zero-padded String representation of the input number
     */
    private String zeroPad(long number, int width) {
        long n = Math.abs(number);
        long w = width;
        if (w < 0) {
            w = 0;
        } else if (w > 18) {
            w = 18;
        }
        long wrapAt = (long) Math.pow(10, w);
        return String.valueOf(n % wrapAt + wrapAt).substring(1);
    }

    /**
     * Copies the file or directory represented by this instance to a remote
     * machine.
     * 
     * @param destFile
     *            The location on the remote device where you want to place this
     *            file
     * @param recursive
     *            If this instance represents a directory, whether or not to
     *            recursively transfer all files and directories within this
     *            directory
     * @param overwrite
     *            true if files should be overwritten durin the copy. If this is
     *            false and a file is encountered in the destination of the
     *            copy, an Exception is thrown.
     * @return true if the copy was successful
     */
    public boolean copyToDest(RemoteFile destFile, boolean recursive,
                              boolean overwrite) throws Exception {
        return RemoteFile.copy(this, destFile, recursive, overwrite, true);
    }

    /**
     * Copies the file or directory represented by this instance to a remote
     * machine.
     * 
     * @param destFile
     *            The location on the remote device where you want to place this
     *            file
     * @param recursive
     *            If this instance represents a directory, whether or not to
     *            recursively transfer all files and directories within this
     *            directory
     * @param overwrite
     *            true if files should be overwritten durin the copy. If this is
     *            false and a file is encountered in the destination of the
     *            copy, an Exception is thrown.
     * @return true if the copy was successful
     */
    public boolean copyToDestText(RemoteFile destFile, boolean recursive,
                                  boolean overwrite) throws Exception {
        return RemoteFile.copy(this, destFile, recursive, overwrite, false);
    }

    /**
     * Copies the file or directory represented by this instance to a remote
     * machine. If this is a directory the copy is not done recursively. Files
     * are overwritten during the copy.
     * 
     * @param destFile
     *            The location on the remote device where you want to place this
     *            file
     * @return true if the copy was successful
     * @throws Exception
     */
    public boolean copyToDest(RemoteFile destFile) throws Exception {
        return RemoteFile.copy(this, destFile, false, true, true);
    }

    /**
     * Copies the file or directory represented by this instance to a remote
     * machine. If this is a directory the copy is not done recursively. Files
     * are overwritten during the copy.
     * 
     * @param destFile
     *            The location on the remote device where you want to place this
     *            file
     * @return true if the copy was successful
     * @throws Exception
     */
    public boolean copyToDestText(RemoteFile destFile) throws Exception {
        return RemoteFile.copy(this, destFile, false, true, false);
    }

    /**
     * Copies the file or directory from a RemoteMachine to the path specified
     * by this instance
     * 
     * @param srcFile
     *            The location on the remote device where you want to get this
     *            file
     * @param recursive
     *            If this srcFile represents a directory, whether or not to
     *            recursively transfer all files and directories within the
     *            source directory
     * @param overwrite
     *            true if files should be overwritten durin the copy. If this is
     *            false and a file is encountered in the destination of the
     *            copy, an Exception is thrown.
     * @return true if the copy was successful
     */
    public boolean copyFromSource(RemoteFile srcFile, boolean recursive,
                                  boolean overwrite) throws Exception {
        return RemoteFile.copy(srcFile, this, recursive, overwrite, true);
    }

    /**
     * Copies the file or directory from a RemoteMachine to the path specified
     * by this instance
     * 
     * @param srcFile
     *            The location on the remote device where you want to get this
     *            file
     * @param recursive
     *            If this srcFile represents a directory, whether or not to
     *            recursively transfer all files and directories within the
     *            source directory
     * @param overwrite
     *            true if files should be overwritten durin the copy. If this is
     *            false and a file is encountered in the destination of the
     *            copy, an Exception is thrown.
     * @return true if the copy was successful
     */
    public boolean copyFromSourceText(RemoteFile srcFile, boolean recursive,
                                      boolean overwrite) throws Exception {
        return RemoteFile.copy(srcFile, this, recursive, overwrite, false);
    }

    /**
     * Copies the file or directory from a RemoteMachine to the path specified
     * by this instance. If the source is a directory the copy is not done
     * recursively. Files are overwritten during the copy.
     * 
     * @param srcFile
     *            The location on the remote device where you want to get this
     *            file
     * @return true if the copy was successful
     * @throws Exception
     */
    public boolean copyFromSource(RemoteFile srcFile) throws Exception {
        return RemoteFile.copy(srcFile, this, false, true, true);
    }

    /**
     * Copies the file or directory from a RemoteMachine to the path specified
     * by this instance. If the source is a directory the copy is not done
     * recursively. Files are overwritten during the copy.
     * 
     * @param srcFile
     *            The location on the remote device where you want to get this
     *            file
     * @param binary
     *            true if you want the file transfered in binary, ascii if false
     * @return true if the copy was successful
     * @throws Exception
     */
    public boolean copyFromSource(RemoteFile srcFile, boolean binary)
                    throws Exception {
        return RemoteFile.copy(srcFile, this, false, true, binary);
    }

    // Delete ...

    public boolean delete() throws Exception {
        return delete(STANDARD_RETRY_INTERVAL_NS);
    }

    public boolean deleteNoRetry() throws Exception {
        return delete(0L);
    }

    public boolean delete(long retryNs) throws Exception {
        Operation deleteOp = new Operation() {
            public boolean act() throws Exception {
                return basicDelete();
            }
        };
        return retry(deleteOp, retryNs);
        // return retry( () -> basicDelete(), retryNs );
    }

    public boolean deleteLocalDirectory(File localDir) throws Exception {
        return deleteLocalDirectory(localDir, STANDARD_RETRY_INTERVAL_NS);
    }

    public boolean deleteLocalDirectoryNoRetry(File localDir) throws Exception {
        return deleteLocalDirectory(localDir, 0L);
    }

    public boolean deleteLocalDirectory(final File localDir, long retryNs) throws Exception {
        Operation deleteOp = new Operation() {
            public boolean act() {
                return basicDeleteLocalDirectory(localDir);
            }
        };
        return retry(deleteOp, retryNs);
        // return retry( () -> basicDeleteLocalDirectory(localDir), retryNs );
    }

    private boolean basicDelete() throws Exception {
        if ( host.isLocal() ) { // This is 'localFile != null' in open-liberty.
            if ( localFile.isDirectory() ) {
                return basicDeleteLocalDirectory(localFile);
            } else {
                return basicDeleteLocalFile(localFile);
            }
        } else {
            return basicDeleteRemoteFile();
        }
    }

    private boolean basicDeleteLocalDirectory(File localDir) {
        if ( !localDir.exists() ) {
            return true;
        }

        File[] files = localDir.listFiles();
        for ( File file : files ) {
            if ( file.isDirectory() ) {
                if ( !basicDeleteLocalDirectory(file) ) {
                    return false;
                }
            } else {
                if ( !basicDeleteLocalFile(file) ) {
                    return false;
                }
            }
        }

        return ( basicDeleteLocalFile(localDir) );
    }

    private boolean basicDeleteLocalFile(File useLocalFile) {
        String methodName = "basicDeleteLocalFile";

        Path localPath = useLocalFile.toPath();
        try {
            return Files.deleteIfExists(localPath);
        } catch ( IOException e ) {
            Log.info(c, methodName, "Failed to delete '" + localPath + "': " + e.getMessage());
            return false;
        }
    }

    private boolean basicDeleteRemoteFile() throws Exception {
        return providerDelete();
    }

    //

    /**
     * Tests whether the file represented by this RemoteFile is a directory.
     * 
     * @return true if and only if the file denoted by this abstract pathname
     *         exists and is a directory; false otherwise
     */
    public boolean isDirectory() throws Exception {
        if (!exists()) {
            return false;
        }
        if (host.isLocal())
            return localFile.isDirectory();
        else
            return providerIsDirectory();
    }

    /**
     * Tests whether the file denoted by this RemoteFile is a normal file. A
     * file is normal if it is not a directory and, in addition, satisfies other
     * system-dependent criteria. Any non-directory file created by a Java
     * application is guaranteed to be a normal file.
     * 
     * @return true if and only if the file denoted by this abstract pathname
     *         exists and is a normal file; false otherwise
     */
    public boolean isFile() throws Exception {
        if (!exists()) {
            return false;
        }
        if (host.isLocal())
            return localFile.isFile();
        else
            return providerIsFile();
    }

    /**
     * Tests whether the file or directory denoted by this Remotefile exists
     * 
     * @return if and only if the file or directory denoted by this abstract
     *         pathname exists; false otherwise
     */
    public boolean exists() throws Exception {
        if (host.isLocal())
            return localFile.exists();
        else
            return providerExists();
    }

    /**
     * Returns an array of RemoteFiles denoting the files in the directory
     * denoted by this RemoteFile.
     * 
     * @param recursive
     *            If this instance represents a directory, whether or not to
     *            recursively list all files and directories
     * 
     * @return An array of RemoteFiles denoting the files and directories in the
     *         directory denoted by this RemoteFile. The array will be empty if
     *         the directory is empty. Returns null if this abstract pathname
     *         does not denote a directory
     */
    public RemoteFile[] list(boolean recursive) throws Exception {
        final String method = "list";
        Log.entering(c, method, recursive);
        if (!isDirectory()) {
            Log.finer(c, method, "This is not a directory.");
            Log.exiting(c, method, null);
            return null;
        }

        RemoteFile[] remoteFiles = null;
        List<RemoteFile> remoteFilesList = new ArrayList<RemoteFile>();
        if (host.isLocal()) {
            File[] list = localFile.listFiles();
            for (int i = 0; i < list.length; ++i) {
                RemoteFile remoteFile = new RemoteFile(host, list[i]
                                .getCanonicalPath());
                remoteFilesList.add(remoteFile);

                // If needed recurse
                if (recursive && remoteFile.isDirectory()) {
                    RemoteFile[] grandchildren = remoteFile.list(true);
                    for (RemoteFile grandchild : grandchildren) {
                        remoteFilesList.add(grandchild);
                    }
                }
            }
            remoteFiles = remoteFilesList.toArray(new RemoteFile[0]);
        } else {
            String[] fileList = providerList(recursive);
            remoteFiles = new RemoteFile[fileList.length];
            for (int i = 0; i < fileList.length; ++i) {
                remoteFiles[i] = new RemoteFile(host, fileList[i]);
            }
        }
        Log.exiting(c, method, remoteFiles);
        return remoteFiles;
    }

    /**
     * Creates the directory named by this RemoteFile
     * 
     * @return true if and only if the directory was created; false otherwise
     */
    public boolean mkdir() throws Exception {
        if (host.isLocal())
            return localFile.mkdir();
        else
            return providerMkdir();
    }

    /**
     * Creates the directory named by this RemoteFile, including any necessary
     * but nonexistent parent directories. Note that if this operation fails it
     * may have succeeded in creating some of the necessary parent directories.
     * 
     * @return true if and only if the directory was created, along with all
     *         necessary parent directories; false otherwise
     */
    public boolean mkdirs() throws Exception {
        if (host.isLocal())
            return localFile.mkdirs();
        else
            return providerMkdirs();
    }

    //

    public boolean rename(RemoteFile newFile) throws Exception {
        return rename(newFile, STANDARD_RETRY_INTERVAL_NS);
    }

    public boolean renameNoRetry(RemoteFile newFile) throws Exception {
        return rename(newFile, 0L);
    }

    public boolean rename(final RemoteFile newFile, long retryNs) throws Exception {
        Operation renameOp = new Operation() {
            public boolean act() throws Exception {
                return basicRename(newFile);
            }
        };
        return retry(renameOp, retryNs);
        // return retry( () -> basicRename(newFile), retryNs );
    }

    private boolean basicRename(RemoteFile newFile) throws Exception {
        if ( host.isLocal() ) {
            return localFile.renameTo(new File(newFile.getAbsolutePath()));
        } else {
            return providerRename(newFile);
        }
    }

    //

    /**
     * Returns the name of the file or directory denoted by this RemoteFile
     * 
     * @return The name of the file
     * @throws Exception
     */
    public String getName() throws Exception {
        if (name == null) {
            name = getAbsolutePath();
            int startIndex = name.lastIndexOf("/");
            if (startIndex != -1) {
                name = name.substring(startIndex + 1);
            }
        }
        return name;
    }

    public InputStream openForReading() throws Exception {
        if (host.isLocal())
            return new FileInputStream(localFile);
        else
            return providerOpenFileForReading();
    }

    public OutputStream openForWriting(boolean append) throws Exception {
        if (host.isLocal())
            return new FileOutputStream(localFile, append);
        else
            return providerOpenFileForWriting(append);
    }

    /**
     * Returns a String representation of the RemoteFile
     */
    @Override
    public String toString() {
        return getAbsolutePath();
    }

    /**
     * Copy a RemoteFile
     * 
     * @param srcFile
     *            The source RemoteFile
     * @param destFile
     *            The destination RemoteFile
     * @param recursive
     *            true if this a recursive copy
     * @param overwrite
     *            true if files should be overwritten during the copy
     * @return true if the copy was successful
     * @throws Exception
     */
    private static boolean copy(RemoteFile srcFile, RemoteFile destFile,
                                boolean recursive, boolean overwrite, boolean binary)
                    throws Exception {
        final String method = "copy";
        Log.entering(c, method, new Object[] { srcFile, destFile, recursive, overwrite });

        if (!srcFile.exists()) {
            throw new Exception("Cannot copy a file or directory that does not exist: "
                                + srcFile.getAbsolutePath() + ": "
                                + srcFile.getMachine().getHostname());
        }

        boolean destExists = destFile.exists();
        boolean destIsDir = destFile.isDirectory();

        if (!overwrite && destExists && !destIsDir) {
            throw new Exception("Destination " + destFile.getAbsolutePath()
                                + " on machine " + destFile.getMachine().getHostname()
                                + " already exists.");
        }

        if (srcFile.isDirectory()) {
            Log.finer(c, method, "Source file is a directory.");
            if (!destIsDir) {
                Log.finer(c, method, "Converting the destination file to a directory.");
                if (destExists) {
                    if (!destFile.delete()) {
                        throw new Exception("The destination directory exists as a file. Unable to delete the file and overwrite. DestDir: "
                                            + destFile.getAbsolutePath());
                    }
                }
                Log.finer(c, method, "Creating the destination directory.");
                if (!destFile.mkdirs()) {
                    throw new Exception("Unable to create destination directory " + destFile.getAbsolutePath());
                }
            }

            RemoteFile[] childEntries = srcFile.list(false);
            boolean copied = true;
            if (childEntries != null && recursive) {
                Log.finer(c, method, "Copying children...");
                for (int i = 0; i < childEntries.length; ++i) {
                    RemoteFile destChild = new RemoteFile(destFile.host, destFile, childEntries[i].getName());
                    copied = copied && RemoteFile.copy(childEntries[i], destChild, recursive, overwrite, binary);
                    Log.finer(c, method, "Child copied successfully: " + copied);
                }
            }

            Log.exiting(c, method, copied);
            return copied;

        } else {
            Log.finer(c, method, "The source file is a file. Copying the file.");
            if ( !destFile.getParentFile().equals(null) ) {
                RemoteFile parentFolder = new RemoteFile(destFile.getMachine(), destFile.getParent());
                Log.finer(c, method, destFile.getParent());
                parentFolder.mkdirs();
            }

            boolean result = providerCopy(srcFile, destFile, binary);
            Log.exiting(c, method, result);
            return result;
        }
    }

    /**
     * Get the parent path of a file
     * 
     * @param path
     *            The path of the file
     * @return The parent path of the file
     */
    private String getParentPath(String path) {
        if (path.equals("/")) { // root
            return null;
        }
        path = path.replace('\\', '/');
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        int endIndex = path.lastIndexOf("/");
        if (endIndex != -1) {
            path = path.substring(0, endIndex);
        }
        if (path.length() == 0) {
            path = "/";
        }
        return path;
    }

    private String convertPath(String path) {
        if (path != null) {
            path = path.replace('\\', '/');
            if (!path.equals("/") && path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void init() {
        if (host.isLocal())
            localFile = new File(filePath);
    }

    /**
     * Get the size of the file
     * 
     * @return the size of the file
     */
    public long length() {
        if (localFile != null)
            return localFile.length();
        else
            return 0;
    }

    /**
     * Get the last modified timestamp of the file. If directory
     * returns 0L
     * 
     * @return the last modified timestamp
     */
    public long lastModified() {
        return localFile.lastModified();
    }

    /**
     * Returns the encoding of the file
     * 
     * @return the character set the file is encoded in
     */
    public Charset getEncoding() {
        return encoding;
    }

    // Provider implemented operations ...

    // Isolate these changes to keep the open-liberty and WS-CD-Open
    // copies of this source as close as possible.

    private boolean providerDelete() throws Exception{
        return LocalProvider.delete(this);
    }
    
    private boolean providerIsDirectory() throws Exception {
        return LocalProvider.isDirectory(this);
    }

    private boolean providerIsFile() throws Exception {
        return LocalProvider.isFile(this);
    }

    private boolean providerExists() throws Exception {
        return LocalProvider.exists(this);
    }
    
    private String[] providerList(boolean recursive) throws Exception {
        return LocalProvider.list(this, recursive);
    }

    private boolean providerMkdir() throws Exception {
        return LocalProvider.mkdir(this);
    }

    private boolean providerMkdirs() throws Exception {
        return LocalProvider.mkdirs(this);
    }

    private boolean providerRename(RemoteFile newFile) throws Exception {
        return LocalProvider.rename(this, newFile);
    }

    private InputStream providerOpenFileForReading() throws Exception {
        return LocalProvider.openFileForReading(this);
    }

    private OutputStream providerOpenFileForWriting(boolean append) throws Exception {
        return LocalProvider.openFileForWriting(this, append);
    }

    private static boolean providerCopy(RemoteFile srcFile, RemoteFile destFile, boolean binary) throws Exception {
        return LocalProvider.copy(srcFile, destFile, binary);
    }
}
