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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;

import com.ibm.websphere.simplicity.log.Log;
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
        return this.host;
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
        return new RemoteFile(this.host, path);
    }

    /**
     * Returns the absolute pathname string of this RemoteFile in the form
     * /dir1/dir2/file
     * 
     * @return The absolute pathname of this RemoteFile
     */
    public String getAbsolutePath() {
        return this.filePath;
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
        RemoteFile[] children = this.list(false);
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
        return new RemoteFile(this.getMachine(), this, newName.toString());
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


    /**
     * Uses the {@link Files} class for the deletion operation because
     * it throws informational exception on operation failure. Outputs 
     * exception message to liberty output for debugging.
     * 
     * @param path
     *              The {@link File} object that represents a file to be deleted
     * @return true if deletetion was successful, false if failure
     */
    private boolean deleteExecutionWrapper(File path) {
        try{
            java.nio.file.Files.delete(path.toPath());
            return true;
        }catch(Exception e){
            Log.info(c, "deleteExecutionWrapper", "Delete Operation for [" + path + "] could not be completed.\n" + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the file or directory denoted by this abstract pathname.
     * 
     * @return true if and only if the file or directory is successfully
     *         deleted; false otherwise
     */
    public boolean delete() throws Exception {
        if (host.isLocal()) {
            if (localFile.isDirectory()) {
                return this.deleteLocalDirectory(localFile);
            } else
                return this.deleteExecutionWrapper(localFile);
        } else
            return LocalProvider.delete(this);
    }

    /**
     * Recursively deletes the contents then the directory denoted by this
     * pathname.
     * 
     * @return true if and only if the directory is successfully deleted; false
     *         otherwise
     */
    public boolean deleteLocalDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteLocalDirectory(files[i]);
                } else {
                    boolean b = this.deleteExecutionWrapper(files[i]);
                    if (!b) {
                        Log.info(c, "deleteLocalDirectory", "couldn't delete localfile = " + files[i]);
                    }
                }
            }
        }
        return (this.deleteExecutionWrapper(path));
    }

    /**
     * Tests whether the file represented by this RemoteFile is a directory.
     * 
     * @return true if and only if the file denoted by this abstract pathname
     *         exists and is a directory; false otherwise
     */
    public boolean isDirectory() throws Exception {
        if (!this.exists()) {
            return false;
        }
        if (host.isLocal())
            return this.localFile.isDirectory();
        else
            return LocalProvider.isDirectory(this);
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
        if (!this.exists()) {
            return false;
        }
        if (host.isLocal())
            return this.localFile.isFile();
        else
            return LocalProvider.isFile(this);
    }

    /**
     * Tests whether the file or directory denoted by this Remotefile exists
     * 
     * @return if and only if the file or directory denoted by this abstract
     *         pathname exists; false otherwise
     */
    public boolean exists() throws Exception {
        if (host.isLocal())
            return this.localFile.exists();
        else
            return LocalProvider.exists(this);
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
        if (!this.isDirectory()) {
            Log.finer(c, method, "This is not a directory.");
            Log.exiting(c, method, null);
            return null;
        }

        RemoteFile[] remoteFiles = null;
        List<RemoteFile> remoteFilesList = new ArrayList<RemoteFile>();
        if (host.isLocal()) {
            File[] list = this.localFile.listFiles();
            for (int i = 0; i < list.length; ++i) {
                RemoteFile remoteFile = new RemoteFile(this.host, list[i]
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
            String[] fileList = LocalProvider.list(this, recursive);
            remoteFiles = new RemoteFile[fileList.length];
            for (int i = 0; i < fileList.length; ++i) {
                remoteFiles[i] = new RemoteFile(this.host, fileList[i]);
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
            return this.localFile.mkdir();
        else
            return LocalProvider.mkdir(this);
    }

    //

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
            return this.localFile.mkdirs();
        else
            return LocalProvider.mkdirs(this);
    }

    //

    /**
     * Sleep a specified interval, measured in nano-seconds.
     *
     * @param ns The interval, in nano-seconds.
     *
     * @throws Exception Thrown if an error occurs.  This should only
     *     ever be {@link InterruptedException}.
     */
    public static void sleep(long ns) throws Exception {
        Thread.currentThread().sleep( (long) (ns / 1000), (int) (ns % 1000) );
        // throws InterruptedException;
    }

    //

    /**
     * Attempt to rename this file.  The file may be non-local.
     *
     * @param newFile The target file.
     *
     * @throws Exception Thrown if the rename failed.
     */
    public boolean rename(RemoteFile newFile) throws Exception {
        if ( host.isLocal() ) {
            return this.localFile.renameTo(new File(newFile.getAbsolutePath()));
        } else {
            return LocalProvider.rename(this, newFile);
        }
    }

    /**
     * The standard retry interval for rename operations.
     * This is based on the artifact file system minimum
     * retry interval of 200 nano-seconds, per
     * <code>
     *   open-liberty/dev/com.ibm.ws.artifact.zip/src/
     *   com/ibm/ws/artifact/zip/cache/
     *   ZipCachingProperties.java
     * </code>
     *
     * The default largest pending close time is specified
     * by property <code>zip.reaper.slow.pend.max</code>.
     */
    public static final long STANDARD_RENAME_INTERVAL = 2 * 200 * 1000 * 1000; // 2 * 200 nano-seconds.

    /**
     * Attempt to rename a file including a retry interval.  Use the standard
     * retry interval, {@link #STANDARD_RENAME_INTERVAL}.
     *
     * If the initial rename fails, sleep for the specified interval, then try again.
     *
     * This is provided as a guard against latent holds on archive files, which
     * occur because of zip file caching.
     *
     * See also {@link #renameWithRetry(RemoteFile, long)}, {@link #rename(RemoteFile)},
     * and {@link #sleep(long)}.
     *
     * @param newFile The new file.
     *
     * @return True or false telling if the retry was successful.
     */
    public boolean renameWithRetry(RemoteFile newFile) throws Exception {
        return renameWithRetry(newFile, STANDARD_RENAME_INTERVAL);
    }

    /**
     * Attempt to rename a file including a retry interval.  A retry will be
     * attempted even when the source file is non-local, since the lock can
     * be on either the source or the target file, and the target file is
     * always local.
     *
     * This is provided as a guard against latent holds on archive files, which
     * occur because of zip file caching.
     *
     * @param newFile The new file.
     * @param retryNs The retry interval, in nano-seconds.
     *
     * @return True or false telling if the retry was successful.
     */
    public boolean renameWithRetry(RemoteFile newFile, long retryNs) throws Exception {
        boolean firstResult = rename(newFile); // throws Exception
        if ( firstResult ) {
            return firstResult;
        }
        sleep(retryNs); // throws Exception
        boolean secondResult = rename(newFile); // throws Exception
        return secondResult;
    }

    //

    /**
     * Returns the name of the file or directory denoted by this RemoteFile
     * 
     * @return The name of the file
     * @throws Exception
     */
    public String getName() throws Exception {
        if (this.name == null) {
            name = this.getAbsolutePath();
            int startIndex = name.lastIndexOf("/");
            if (startIndex != -1) {
                name = name.substring(startIndex + 1);
            }
        }
        return this.name;
    }

    public InputStream openForReading() throws Exception {
        if (host.isLocal())
            return new FileInputStream(this.localFile);
        else
            return LocalProvider.openFileForReading(this);
    }

    public OutputStream openForWriting(boolean append) throws Exception {
        if (host.isLocal())
            return new FileOutputStream(this.localFile, append);
        else
            return LocalProvider.openFileForWriting(this, append);
    }

    /**
     * Returns a String representation of the RemoteFile
     */
    @Override
    public String toString() {
        return this.getAbsolutePath();
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
        Log.entering(c, method, new Object[] { srcFile, destFile, recursive,
                                              overwrite });
        boolean destExists = destFile.exists();
        boolean destIsDir = destFile.isDirectory();
        boolean copied = true;
        if (destFile == null) {
            throw new Exception("destFile cannot be null.");
        }
        if (!srcFile.exists()) {
            throw new Exception("Cannot copy a file or directory that does not exist: "
                                + srcFile.getAbsolutePath() + ": "
                                + srcFile.getMachine().getHostname());
        }
        if (!overwrite && destExists && !destIsDir) {
            throw new Exception("Destination " + destFile.getAbsolutePath()
                                + " on machine " + destFile.getMachine().getHostname()
                                + " already exists.");
        }

        RemoteFile[] childEntries = null;

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
                // create the directory
                Log.finer(c, method, "Creating the destination directory.");
                if (!destFile.mkdirs()) {
                    throw new Exception("Unable to create destination directory " + destFile.getAbsolutePath());
                }
            }
            childEntries = srcFile.list(false);

            // now copy any children
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
            //Now we ensure the parent directory path exists and create if it doesn't as long as it has a parent
            if (!!!destFile.getParentFile().equals(null)) {
                RemoteFile parentFolder = new RemoteFile(destFile.getMachine(), destFile.getParent());
                Log.finer(c, method, destFile.getParent()); // info level is inconsistent with other messages in this method, and also very loud for large transfers
                parentFolder.mkdirs();
            }

            // copy the file
            boolean result = LocalProvider.copy(srcFile, destFile, binary);
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
        if (this.host.isLocal())
            this.localFile = new File(this.filePath);
    }

    /**
     * Get the size of the file
     * 
     * @return the size of the file
     */
    public long length() {
        if (this.localFile != null)
            return this.localFile.length();
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
        return this.localFile.lastModified();
    }

    /**
     * Returns the encoding of the file
     * 
     * @return the character set the file is encoded in
     */
    public Charset getEncoding() {
        return this.encoding;
    }
}
