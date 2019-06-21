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
package com.ibm.websphere.simplicity;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
    private static final Class CLASS = RemoteFile.class;

    // Retry utility ...

    public static final long ALLOWED_OVERAGE_NS = TimeUnit.MILLISECONDS.toNanos(50L); // 1/20 sec

    /**
     * Sleep a specified interval, measured in nanoseconds.
     * See {@link TimeUnit#NANOSECONDS}.
     *
     * @param requestedNs The interval, in nanoseconds.
     *
     * @throws InterruptedException Thrown if if the sleep is
     *     interrupted.
     */
    public static void sleep(long requestedNs) throws InterruptedException {
        String methodName = "sleep";

        long startNs = System.nanoTime();
        TimeUnit.NANOSECONDS.sleep(requestedNs);
        long endNs = System.nanoTime();

        long actualNs = endNs - startNs;
        if ( actualNs < requestedNs ) {
            Log.warning(CLASS, methodName + ": Truncated: Requested [ " + requestedNs + " (ns) ]; Actual [ " + actualNs + " (ns) ]");
        } else {
            long extraNs = actualNs - requestedNs;
            if ( extraNs > ALLOWED_OVERAGE_NS ) {
                Log.warning(CLASS, methodName + ": Extended: Requested [ " + requestedNs + " (ns) ]; Actual [ " + actualNs + " (ns) ]");
            }
        }
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

    public static final long STANDARD_RETRY_MAX_NS =
        TimeUnit.SECONDS.toNanos(120L);

    public static final long STANDARD_RETRY_PARTIAL_INTERVAL_NS =
        TimeUnit.MILLISECONDS.toNanos(50);

    /**
     * Functional interface for {@link #retry}.
     * 
     * The operation, {@link Operation#act()} is based on
     * {@link Files#deleteIfExists(Path)}, which answers false
     * if the file does not exist, and which throws an exception
     * if the file does exist but could not be deleted.
     */
    public interface Operation {
        /**
         * Attempt an operation.
         *
         * @return True or false telling if the operation failed.
         *
         * @throws Exception Thrown if the operation failed for exceptional reasons.
         */
        public boolean act() throws Exception;
    }

    public boolean retry(Operation op, long retryNs) throws Exception {
        String methodName = "retry";

        if ( retryNs < 0 ) {
            Log.warning(CLASS, methodName +
                ": Increased interval [ " + retryNs + " (ns) ] to [ 0 (ns) ]" +
                " for [ " + getAbsolutePath() + " ]");
            retryNs = 0;

        } else if ( retryNs > STANDARD_RETRY_MAX_NS ) {
            Log.warning(CLASS, methodName +
                ": Decreased interval [ " + retryNs + " (ns) ] to [ " + STANDARD_RETRY_MAX_NS + " (ns) ]" +
                " for [ " + getAbsolutePath() + " ]");
            retryNs = STANDARD_RETRY_MAX_NS;
        }

        // Split the retry interval into pieces.
        //
        // This is intended to balance between overly frequent retries
        // and overly long delays.

        int retryCount = ((int) (retryNs / STANDARD_RETRY_PARTIAL_INTERVAL_NS));
        if ( (retryNs % STANDARD_RETRY_PARTIAL_INTERVAL_NS) > 0 ) {
            retryCount++;
        }

        // Plus one: An initial try, then a retry per partial interval.
        //
        // A delay of 0 results in a retry count of 0, which turns into
        // just an initial attempt and no retries.

        for ( int retryNo = 0; retryNo < retryCount + 1; retryNo++ ) {
            if ( retryNo > 0 ) { // Only retry after the first attempt.
                sleep(STANDARD_RETRY_PARTIAL_INTERVAL_NS);
            }

            if ( retryNo == retryCount ) {
                return op.act(); // Last try: Allow the exception to escape.

            } else {
                try {
                    return op.act();
                } catch ( Exception e ) {
                    Log.info(CLASS, methodName, "Failed attempt [ " + retryNo + " ]: " + e.getMessage());
                }
            }
        }

        throw new IllegalStateException(); // Can't get here
    }

    // Basic state:

    // 'host', 'name', and 'absPath' are never null.
    //
    // 'name' is empty if the file is a root file.
    //
    // 'absPath' is a single slash if the file is a root file.  'absPath'
    // always starts with a single slash, and always uses forward slashes.
    //
    // 'parentPath' is null if the file is a root file.
    //
    // 'parentFile' is null until assigned.  'parentFile' remains null only
    // if the file is a root file.  'parentFile' is assigned on demand in a
    // thread safe manner.
    //
    // 'localFile' is null until assigned.  'localFile' remains null only if the
    // file is a non-local file.  'localFile' is assigned on demand in a thread
    // safe manner.
    //
    // 'path' is null until assigned, after which it is never null.  'path'
    // is assigned on demand in a thread safe manner.

    private final Machine host;

    private final String parentPath;
    private volatile RemoteFile parentFile;

    private final String name;
    private final String absPath;
    private final File localFile;
    private volatile Path path;

    /**
     * Answer the machine / host of this file.
     *
     * @return The host of this file.
     */
    public Machine getMachine() {
        return host;
    }

    /**
     * Tell if this file is local.  A remote file is local if
     * the file's host is local.  See {@link Machine#isLocal()}.
     *
     * @return True or false telling if this file is local.
     */
    public boolean isLocal() {
        return host.isLocal;
    }

    /**
     * Answer the the path of the parent of this file.  Answer
     * null if this file is a root file.
     *
     * @return The path of the parent of this file.
     *
     * @throws Exception Thrown if an error occurs obtaining the
     *     path of the parent of this file.  Never thrown by this
     *     implementation.
     */
    public String getParent() throws Exception {
        return parentPath;
    }

    /**
     * Answer the parent of this remote file.
     * 
     * Answer null if this remote file is a root file.
     * 
     * Put the new parent file on the specified host, and set the
     * encoding of the new parent file to the specified encoding. 
     *
     * @param parentHost The host of the new parent file.
     * @param parentEncoding The encoding of the new parent file.
     *
     * @return The parent of this remote file.
     */    
    public RemoteFile getParentFile(Machine parentHost, Charset parentEncoding) {
        if ( parentPath == null ) {
            return null;
        } else {
            return new RemoteFile(parentHost, parentPath, parentEncoding); 
        }
    }

    /**
     * Answer the parent of this remote file.
     * 
     * Answer null if this remote file is a root file.
     * 
     * Put the new parent file on the same host as this remote file.
     *
     * Use the default encoding for the parent file.
     *
     * The parent value is assigned at most once, and may be accessed
     * freely with very little performance cost.
     * 
     * @return The parent of this remote file.
     */
    public RemoteFile getParentFile() {
        if ( parentPath == null ) {
            return null;
        } else {
            if ( parentFile == null ) {
                synchronized ( this ) {
                    if ( parentFile == null ) {
                        parentFile = createPeer(parentPath);
                    }
                }
            }
            return parentFile;
        }
    }

    /**
     * Answer the name of this file.  An empty string if this file is a
     * root file.  Never null.
     *
     * @return The name of this file.
     */
    public String getName() {
        return name;
    }

    /**
     * Answer the full absolute path of this file.  Never null or empty.
     * A single slash character for a root file.
     *
     * @return The full absolute path of this file.
     */
    public String getAbsolutePath() {
        return absPath;
    }

    /**
     * Answer this remote file as a {@link java.io.File}.
     *
     * Answer null if this remote file is not local.
     *
     * @return This file as a {@link java.io.File}.
     */
    public File asFile() {
        return localFile;
    }

    /**
     * Answer this file as a path.
     *
     * The path value is assigned at most once, and may be accessed
     * freely with very little performance cost.
     *
     * @return This file as a path.
     */
    public Path asPath() {
        if ( path == null ) {
            synchronized ( this ) {
                if ( path == null ) {
                    path = FileSystems.getDefault().getPath( getAbsolutePath() );
                }
            }
        }
        return path;
    }

    // Simple operations ...

    // Passive operations ...

    public boolean exists() throws Exception {
        return providerExists();
    }

    public boolean isDirectory() throws Exception {
        return providerIsDirectory();
    }

    public boolean isFile() throws Exception {
        return providerIsFile();
    }

    public long lastModified() {
        return providerLastModified();
    }

    public long length() {
        return providerLength();
    }

    // A complex 'list(boolean recurse)' is also available.

    public String[] list() throws Exception {
        return providerList();
    }

    // Active operations ...

    public InputStream openForReading() throws Exception {
        return providerOpenFileForReading();
    }

    public OutputStream openForWriting(boolean append) throws Exception {
        return providerOpenFileForWriting(append);
    }

    public boolean mkdir() throws Exception {
        return providerMkdir();
    }

    public boolean mkdirs() throws Exception {
        return providerMkdirs();
    }

    // 'move', 'rename', 'copy', and 'delete' are complex operations:
    //     'move', 'rename', and 'delete' have retry logic.
    //     'copy' and 'delete' are recursive.

    private boolean basicMove(RemoteFile dest) throws Exception {
        // No local implementation is available.
        return providerMove(dest);
    }

    private boolean basicRename(RemoteFile dest) throws Exception {
        // No local implementation is available.
        return providerRename(dest);
    }

    private boolean basicCopy(RemoteFile dest, boolean binary) throws Exception {
        // No local implementation is available. 
        return providerCopy(dest, binary);
    }

    private boolean basicDelete() throws Exception {
        // No local implementation is available.
        return providerDelete();
    }

    // Content handling assist: Associate an encoding with this file.

    // Note that the encoding can be changed!

    private Charset encoding;

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public Charset getEncoding() {
        return encoding;
    }

    // Factory assists ...

    /**
     * Answer the path of the parent of a specified path.
     *
     * The specified path must already be normalized, and must
     * start with a leading slash.
     *
     * Answer null if the specified path is a root path.
     * 
     * Except when the result path is the root path, the result path
     * never has a trailing slash.
     *
     * @param childPath The path for which to answer the parent path.
     *
     * @return The parent of the path.
     */
    private static String trimTail(String childPath) {
        int childLen = childPath.length();

        // Answer null for a root path.
        //   /                ==> return null

        if ( childLen == 1 ) {
            return null;
        }

        // Strip off any trailing slash.  The resulting path cannot be empty.
        //   /dir1/dir2/dir3/ ==> /dir1/dir2/dir3
        //   /dir1/dir2/      ==> /dir1/dir2
        //   /dir1/           ==> /dir1

        if ( childPath.charAt(childLen - 1) == '/' ) {
            childPath = childPath.substring(0, childLen - 1);
        }

        // Strip off the last slash and anything following, unless the last
        // slash is the first character.
        //   /dir1/dir2/dir3 ==> /dir1/dir2
        //   /dir1/dir2      ==> /dir1
        //   /dir1           ==> /

        int lastSlashIndex = childPath.lastIndexOf('/');
        if ( lastSlashIndex == 0 ) {
            return "/";
        } else {
            return childPath.substring(0, lastSlashIndex);
        }
    }

    /**
     * Normalize the path.  Unless the path is a root path,
     * remove any trailing slash.  Answer null for a null path,
     * and answer an empty path for an empty path.
     *
     * @param path The path to process.
     *
     * @return The path normalized with the trailing slash removed.
     */
    private static String trimSlash(String path) {
        if ( path == null ) {
            throw new IllegalArgumentException("Path is null");
        }

        int pathLen = path.length();
        if ( pathLen == 0 ) {
            throw new IllegalArgumentException("Path is empty");
        }

        // Always normalize.
        path = path.replace('\\', '/');

        if ( path.charAt(0) != '/' ) {
            throw new IllegalArgumentException("Path [" + path + " ] is not absolute");
        }

        if ( pathLen == 1 ) {
            return path; // Do not strip the slash from a root path.
        } else if ( path.charAt(pathLen - 1) != '/' ) {
            return path; // No trailing slash.
        } else {
            return path.substring(0, pathLen - 1);
        }
    }

    // Constructors ...

    /**
     * Create a new direct child of this file.  The new child
     * file has the same host and encoding as this file.
     *
     * @param name The name of the new child file.
     *
     * @return The new child file.
     */
    public RemoteFile createChild(String name) {
        return new RemoteFile(this, name);
    }

    /**
     * Create a peer of this file.  The new peer file has the
     * same host and encoding as this file.
     *
     * @param absPath The full path to the file.
     *
     * @return The new peer file.
     */
    public RemoteFile createPeer(String absPath) {
        return new RemoteFile( getMachine(), absPath, getEncoding() );
    }

    /**
     * Direct remote file constructor: Create a remote file for
     * a specified host and path.  The remote file uses the
     * default character set.
     *
     * @param host The host of the new remote file.
     * @param path The path of the new remote file.
     */
    public RemoteFile(Machine host, String filePath) {
        this( host, filePath, Charset.defaultCharset() );
    }

    /**
     * Direct remote file constructor: Create a remote file for
     * a specified host and path. Give a specific encoding to the
     * new remote file.
     *
     * The file path must start with a leading slash.
     *
     * @param host The host of the new remote file.
     * @param path The path of the new remote file.
     * @param encoding The encoding of the new remote file.
     */
    public RemoteFile(Machine host, String filePath, Charset encoding) {
        this.host = host;

        this.absPath = trimSlash(filePath);
        this.parentPath = trimTail(this.absPath); // Null for a root path.

        int startIndex = this.absPath.lastIndexOf('/');
        this.name = this.absPath.substring(startIndex + 1); // Empty for a root path.

        if ( this.isLocal() ) {
            this.localFile = new File(this.absPath);
        } else {
            this.localFile = null;
        }

        this.encoding = encoding;
    }

    /**
     * Indirect remote file constructor: Create a remote file as an immediate
     * child of another remote file. Use the same host for the new remote file
     * as is used by the parent remote file.  Use the default character set for
     * the new remote file.
     *
     * @param parent The parent of the new remote file.
     * @param name The name of the new remote file.
     */
    public RemoteFile(RemoteFile parent, String name) {
        this( parent.getMachine(), parent, name, Charset.defaultCharset() );
    }

    /**
     * Indirect remote file constructor: Create a remote file as an immediate
     * child of another remote file. Use the same host for the new remote file
     * as is used by the parent remote file.  Give a specific encoding to the
     * new remote file.
     *
     * @param parent The parent of the new remote file.
     * @param name The name of the new remote file.
     * @param encoding The encoding of the new remote file.
     */
    public RemoteFile(RemoteFile parent, String name, Charset encoding) {
        this( parent.getMachine(), parent, name, encoding );
    }

    /**
     * Indirect remote file constructor: Create a remote file as an immediate
     * child of another remote file. Give a specific host to the new remote
     * file.  Use the default character set for the new remote file.
     *
     * @param host The host of the new remote file.
     * @param parent The parent of the new remote file.
     * @param name The name of the new remote file.
     */    
    public RemoteFile(Machine host, RemoteFile parent, String name) {
        this( host, parent, name, Charset.defaultCharset() );
    }

    /**
     * Indirect remote file constructor: Create a remote file as an immediate
     * child of another remote file. Give a specific host to the new remote file.
     * Give a specific encoding to the new remote file.
     *
     * @param host The host of the new remote file.
     * @param parentFile The parent of the new remote file.
     * @param name The name of the new remote file.
     * @param encoding The encoding of the new remote file.
     */
    public RemoteFile(Machine host, RemoteFile parentFile, String name, Charset encoding) {
        this.host = host;

        this.parentPath = parentFile.getAbsolutePath(); // Always start with a leading slash.

        if ( name == null ) {
            throw new IllegalArgumentException("Name is null");
        } else if ( name.isEmpty() ) {
            throw new IllegalArgumentException("Name is empty");
        }
        this.name = name;

        // Don't add a slash if the parent path is the root path.
        if ( this.parentPath.length() == 1 ) {
            this.absPath = this.parentPath + name;
        } else {
            this.absPath = this.parentPath + '/' + name;
        }

        if ( this.isLocal() ) {
            this.localFile = new File(this.absPath);
        } else {
            this.localFile = null;
        }

        this.encoding = encoding;
    }

    @Override
    public String toString() {
        return getAbsolutePath();
    }

    private volatile String printString;

    // Would prefer for this implementation to be used for 'toString', but
    // existing code relies on that answering the absolute path.

    public String printString() {
        if ( printString == null ) {
            synchronized ( this ) {
                if ( printString == null ) {
                    printString = getAbsolutePath() + ": " + getMachine().getHostname();
                }
            }
        }
        return printString;
    }

    //

    // zero or more characters matching [a-zA-Z_0-9\\.].  The '-' character is not allowed!    
    private static final Pattern wordCharacters = Pattern.compile("[\\w\\.]*");

    /**
     * Answer a new unique file in this directory having the specified prefix and suffix, and
     * with a digits field padded to a specified length.  Answer the file with with digits one
     * higher than the currently available files.
     *
     * The full pattern for generated files is:
     * 
     * <code>
     *     prefix + digits + '-' + suffix
     * </code>
     *
     * A dash is placed after the digits only if a suffix is present.
     *
     * The first generated digits value is "1", zero padded to the left.
     *
     * The generated file is not created.  Concurrent calls to generate a new unique file
     * will answer the same value.
     *
     * @param prefix An optional prefix to the file name.  The prefix must match the
     *     pattern "[a-zA-Z_0-9\\.]".
     * @param digits The number of characters in the digits field.
     * @param suffix An optional suffix to the file name.  The suffix must match the
     *     pattern "[a-zA-Z_0-9\\.]".
     *
     * @return A new unique remote file.
     *
     * @throws IllegalArgumentException Thrown if this file is not a directory, or if
     *     the prefix, digits, or suffix values are not valid, or if a new unique file
     *     could not be generated.
     */
    public RemoteFile getOrderedChild(String prefix, int digits, String suffix) throws Exception {
        RemoteFile[] children = list(DO_NOT_RECURSE);
        if ( children == null ) {
            throw new IllegalArgumentException("Not a directory: " + printString());
        }

        if ( suffix != null)  {
            if ( !wordCharacters.matcher(suffix).matches() ) {
                throw new IllegalArgumentException("Non-valid suffix: " + suffix);
            }
        }

        int prefixLength;
        if ( prefix != null ) {
            if ( !wordCharacters.matcher(prefix).matches() ) {
                throw new IllegalArgumentException("Non-valid prefix: " + prefix);
            }
            prefixLength = prefix.length();
        } else {
            prefixLength = 0;
        }

        long highestChildNumber = 0;

        for ( RemoteFile child : children ) {
            String childName = child.getName();

            if ( prefixLength > 0 ) {
                if ( !childName.regionMatches(0, prefix, 0, prefixLength) ) {
                   continue; // Doesn't start with the prefix; ignore.
                } else {
                    childName = childName.substring(prefixLength);
                }
            }

            if ( suffix != null ) {
                int dashIndex = childName.indexOf("-");
                if ( dashIndex > -1 ) {
                    if ( !childName.regionMatches(dashIndex + 1, suffix, 0, suffix.length()) ) {
                        continue; // Doesn't end with the suffix; ignore.
                    }
                    childName = childName.substring(0, dashIndex);
                } else {
                    continue; // Doesn't have a dash; ignore.
                }
            }

            long childNumber;
            try {
                childNumber = Long.parseLong(childName);
            } catch ( Exception e ) {
                continue; // Non-numeric where digits are expected; ignore.
            }

            if ( childNumber > highestChildNumber ) {
                highestChildNumber = childNumber;
            }
        }

        StringBuilder nextChildName = new StringBuilder();

        if ( prefixLength > 0 ) {
            nextChildName.append(prefix);
        }

        nextChildName.append( zeroPad(highestChildNumber + 1, digits) );

        if ( suffix != null ) {
            nextChildName.append('-');
            nextChildName.append(suffix);
        }

        return createChild( nextChildName.toString() );
    }

    private static final String PAD_TEXT = "00000000000000000000";
    private static final int PAD_LIMIT = 18;

    /**
     * Prepends zeros to the left of the input number to ensure that the input
     * number is a total of <code>width</code> digits. Truncates the input
     * number if it has more than <code>width</code> digits.
     * 
     * @param numberX
     *            a positive integer (negative integers cause problems with odd
     *            widths)
     * @param targetDigits
     *            the number of characters that you want in a String
     *            representation of <code>number</code>; must be a positive
     *            integer smaller than 18 (larger numbers cause an overflow
     *            issue)
     * @return a zero-padded String representation of the input number
     */
    private String zeroPad(long number, int targetDigits) {
        if ( number < 0 ) {
            throw new IllegalArgumentException("Pad of number less than zero [ " + number + " ]");
        }

        if ( targetDigits < 0 ) {
            targetDigits = 0;
        } else if ( targetDigits > PAD_LIMIT ) {
            targetDigits = PAD_LIMIT;
        }

        String numberText = Long.toString(number);

        int missingDigits = targetDigits - numberText.length();
        if ( missingDigits == 0 ) {
            return numberText;
        } else if ( missingDigits < 0 ) {
            return numberText.substring(-missingDigits); // Truncate
        } else {
            return PAD_TEXT.substring(0, missingDigits) + numberText; // Zero fill to the left
        }
    }

    // Complex operation: List

    /**
     * Answer the child files of this file.
     * 
     * Answer null if this file is not a directory.
     * 
     * Answer all immediate children, including immediate children which are directories.
     * 
     * If requested, answer children recursively.
     *
     * The collection of children is depth first.
     *
     * @param recurse Control parameter: Tell if children are to be collected
     *     recursively.
     * 
     * @return The collected children of this file, conditionally recursing.
     *     Null if this file is not a directory.
     *
     * @throws Exception Thrown if an error occurs while obtaining the listing.
     */
    public RemoteFile[] list(boolean recurse) throws Exception {
        if ( !isDirectory() ) {
            return null;
        }

        List<RemoteFile> children = new ArrayList<RemoteFile>();

        list(recurse, children);

        return children.toArray( new RemoteFile[ children.size() ] );
    }

    /**
     * Collect the child files of this file.
     * 
     * Collection nothing if this file is not a directory.
     * 
     * Collect all immediate children, including immediate children which are directories.
     * 
     * If requested, Collection children recursively.
     *
     * The placement of children is depth first.
     * 
     * @param recurse Control parameter: Tell if children are to be collected
     *     recursively.
     * 
     * @return The collected children of this file, conditionally recursing.
     *
     * @throws Exception Thrown if an error occurs while obtaining the listing.
     */
    public void list(boolean recurse, List<RemoteFile> children) throws Exception {
        String[] childNames = list();
        if ( childNames == null ) {
            return; // Not a directory
        }

        for ( String childName : childNames ) {
            RemoteFile remoteChild = createChild(childName);

            children.add(remoteChild);

            if ( recurse ) { // Depth first!
                remoteChild.list(recurse, children);
            }
        }
    }

    // Copy helpers ...

    // Fully defaulted: Do not recurse, do overwrite, do binary copies.

    public boolean copyToDest(RemoteFile destFile) throws Exception {
        return RemoteFile.copy(this, destFile, DO_NOT_RECURSE, DO_OVERWRITE, IS_BINARY);
    }

    public boolean copyFromSource(RemoteFile srcFile) throws Exception {
        return RemoteFile.copy(srcFile, this, DO_NOT_RECURSE, DO_OVERWRITE, IS_BINARY);
    }

    // Fully parameterized.

    public boolean copyToDest(
        RemoteFile destFile,
        boolean recurse, boolean overwrite, boolean isBinary) throws Exception {

        return RemoteFile.copy(this, destFile, recurse, overwrite, isBinary);
    }

    public boolean copyFromSource(
        RemoteFile srcFile,
        boolean recurse, boolean overwrite, boolean isBinary) throws Exception {
        return RemoteFile.copy(srcFile, this, recurse, overwrite, isBinary);
    }

    // Parameterized binary copy.

    public boolean copyToDest(
        RemoteFile destFile, boolean recurse, boolean overwrite) throws Exception {
        return RemoteFile.copy(this, destFile, recurse, overwrite, IS_BINARY);
    }

    public boolean copyFromSource(
        RemoteFile srcFile, boolean recurse, boolean overwrite) throws Exception {
        return RemoteFile.copy(srcFile, this, recurse, overwrite, IS_BINARY);
    }

    // Defaulted with binary as options.

    public boolean copyFromSource(RemoteFile srcFile, boolean binary) throws Exception {
        return RemoteFile.copy(srcFile, this, DO_NOT_RECURSE, DO_OVERWRITE, binary);
    }

    // Defaulted text.

    public boolean copyToDestText(RemoteFile destFile) throws Exception {
        return RemoteFile.copy(this, destFile, DO_NOT_RECURSE, DO_OVERWRITE, IS_TEXT);
    }

    // Parameterized text.

    public boolean copyToDestText(RemoteFile destFile, boolean recurse, boolean overwrite)
        throws Exception {
        return RemoteFile.copy(this, destFile, recurse, overwrite, IS_TEXT);
    }

    public boolean copyFromSourceText(RemoteFile srcFile, boolean recurse, boolean overwrite)
        throws Exception {
        return RemoteFile.copy(srcFile, this, recurse, overwrite, IS_TEXT);
    }

    // Complex Operation: Copy

    // Copy is implemented as a static operation.  The source file, which might
    // be an internal parameter, is shifted to be an external parameter.

    public static final boolean DO_RECURSE = true;
    public static final boolean DO_NOT_RECURSE = false;

    public static final boolean DO_OVERWRITE = true;
    public static final boolean DO_NOT_OVERWRITE = false;
    
    public static final boolean IS_BINARY = true;
    public static final boolean IS_NOT_BINARY = false;
    public static final boolean IS_TEXT = false;
    public static final boolean IS_NOT_TEXT = true;

    private static Exception copyFailure(String srcText, String destText, String error) {
        return new Exception("Failed to copy " + srcText + " onto " + destText + ": " + error);
    }

    /**
     * Copy a source file onto a destination file.
     *
     * Copy operations are not retried.  However, delete operations which are
     * initiated by copy are retry enabled.
     *
     * There are complex rules for handling file combinations.  Particular
     * conditions are whether the source file is a simple file or a directory,
     * whether the destination file exists, and whether the destination file,
     * if it exists, is a simple file or a directory:
     *
     * A copy of a simple file onto a directory is adjusted to be a copy of the
     * simple file into the directory.
     * 
     * A copy of a directory onto a directory is done by copying the directories
     * recursively.
     * 
     * The copy operation creates destination directories as needed.  A failure occurs
     * if one of the destination ancestors is not a directory.
     *
     * A copy of a simple file onto an existing file (either a file or a directory) starts
     * by deleting the existing file.  That fails if overwrite is not enabled.  Deletion
     * occurs after adjusting for copying a source file onto a directory.
     *
     * @param src The source file which is to be copied.
     * @param dest The destination file which is to receive the copy.
     * @param resurse Control parameter: Tells if the copy is to be performed
     *     recursively.  Only matters if the source file is a directory.
     * @param overwrite Control parameter: Tell if copying should proceed
     *     if the destination already exists.  Copying onto an existing
     *     destination requires that the destination first be deleted.
     * @param binary Control parameter: Tell if the copy should be a binary
     *     copy or a text copy.  This parameter is currently unused.  All copies
     *     are currently binary copies.
     *
     * @return True or false telling if the copy was successful.
     *
     * @throws Exception Thrown if the copy fails for unexpected reasons.
     */
    public static boolean copy(
        RemoteFile src, RemoteFile dest,
        boolean recurse, boolean overwrite, boolean binary) throws Exception {

        String method = "copy";

        String srcText = src.printString();
        String destText = dest.printString();

        boolean srcExists = src.exists();
        boolean srcIsDir = srcExists && src.isDirectory();

        boolean destExists = dest.exists();
        boolean destIsDir = destExists && dest.isDirectory();

        Log.info(CLASS, method, "Copy " + srcText + " onto " + destText);
        Log.info(CLASS, method, "Recurse " + recurse + "; Overwrite " + overwrite + "; Binary " + binary);

        Log.info(CLASS, method, "Source exists " + srcExists + " as directory " + srcIsDir);
        Log.info(CLASS, method, "Destination exists " + destExists + " as directory " + destIsDir);

        if ( !srcExists ) {
            throw copyFailure(srcText, destText, "Source does not exist");
        }

        if ( !destExists ) {
            // No dest: Make sure first the parent exists and is a directory.

            RemoteFile destParent = dest.getParentFile();
            String destParentText = destParent.printString();

            if ( !destParent.exists() ) {
                Log.info(CLASS,  method, "Creating destination parent directory " + destParentText);
                destParent.mkdirs();
                if ( !destParent.exists() || !destParent.isDirectory() ) {
                    throw copyFailure(srcText, destText, "Failed to create destination parent directory " + destParentText);
                }
            } else if ( !destParent.isDirectory() ) {
                throw copyFailure(srcText, destText, "Destination parent is not a directory " + destParentText);
            }

            // The destination parent now exists and is a directory.  The destination, however,
            // still does not exist.

            // Next, if the source is a directory, create the destination as a directory.

            if ( srcIsDir ) {
                Log.info(CLASS, method, "Creating destination directory " + destText);
                dest.mkdir();
                if ( !dest.exists() || !dest.isDirectory() ) {
                    throw copyFailure(srcText, destText, "Failed to recreate destination directory"); 
                }
                destExists = true;
                destIsDir = true;
            } else {
                // The destination still does not exist.
            }

            // Proceed to copy, either as a simple file or as a directory.

        } else if ( !srcIsDir ) {
            // Will copy a simple file ...

            // If the destination is a directory, adjust the destination to be a
            // child of the initially specified destination.

            if ( destIsDir ) {
                dest = dest.createChild( src.getName() );
                destText = dest.printString();
                destExists = dest.exists();
                destIsDir = destExists && dest.isDirectory();
            }

            // The destination initially existed, but might not after the adjustment.

            // If the destination exists, since this is a simple file copy, the destination
            // must be deleted.  Don't allow that unless overwrite is enabled.

            if ( destExists ) {
                if ( !overwrite ) {
                    throw copyFailure(srcText, destText, "Overwrite not allowed");
                }
                dest.delete();
                if ( dest.exists() ) {
                    throw copyFailure(srcText, destText, "Failed to delete destination");
                }
                destExists = false;
                destIsDir = false;
            } else {
                // The destination still doesn't exist.
                // The destination parent must still exist, since it was set as
                // the child of an existing directory.
            }

        } else if ( !destIsDir ) {
            // Will copy as a directory.  The destination, if not a directory, must
            // be deleted.  Don't allow that unless overwrite is enabled.

            if ( !overwrite ) {
                throw copyFailure(srcText, destText, "Overwrite not allowed");
            }
            dest.delete();
            if ( dest.exists() ) {
                throw copyFailure(srcText, destText, "Failed to delete destination");
            }
            dest.mkdir();
            if ( !dest.exists() || !dest.isDirectory() ) {
                throw copyFailure(srcText, destText, "Failed to recreate destination directory"); 
            }
            destExists = true;
            destIsDir = true;

        } else {
            // The source and the destination are both known to exist as directories.
        }

        if ( !srcIsDir ) {
            Log.info(CLASS,  method, "Copy source file " + srcText + " onto destination file " + destText);
            if ( !src.basicCopy(dest, binary) ) {
                throw copyFailure(srcText, destText, "Copy failure");
            }
            return true;

        } else {
            Log.info(CLASS,  method, "Copy source directory " + srcText + " onto destination directory " + destText);

            if ( !recurse ) {
                // Nothing left to copy: Recurs is not enabled.
                return true;

            } else {
                RemoteFile[] srcChildren = src.list(DO_NOT_RECURSE);
                if ( srcChildren == null ) {
                    Log.warning(CLASS, method + ": Null children copying " + srcText);
                    return true;
                }
                for ( RemoteFile srcChild : srcChildren ) {
                    RemoteFile destChild = dest.createChild( srcChild.getName() );
                    if ( !RemoteFile.copy(srcChild, destChild, recurse, overwrite, binary) ) {
                        throw copyFailure(srcText, destText, "Failed to copy child " + srcChild.getName());
                    }
                }
                return true;
            }
        }
    }

    // Complex Operation: Move

    /**
     * Attempt to move a file.
     *
     * Retry in case of a failure, up to the standard retry interval.
     *
     * @return True or false telling if the move was successful.
     *     Answer true if the file already does not exist.
     *
     * @throws Exception Thrown if the move failed unexpectedly.
     */
    public boolean move(RemoteFile dest) throws Exception {
        return move(dest, STANDARD_RETRY_INTERVAL_NS);
    }

    public boolean moveNoRetry(RemoteFile dest) throws Exception {
        return move(dest, 0L);
    }

    public boolean move(final RemoteFile dest, long retryNs) throws Exception {
        Operation moveOp = new Operation() {
            public boolean act() throws Exception {
                return basicMove(dest);
            }
        };
        return retry(moveOp, retryNs);
        // return retry( () -> basicRename(dest), retryNs );
    }

    // Complete Operation: Rename

    /**
     * Attempt to rename a file.
     *
     * Retry in case of a failure, up to the standard retry interval.
     *
     * @return True or false telling if the rename was successful.
     *     Answer true if the file already does not exist.
     *
     * @throws Exception Thrown if the rename failed unexpectedly.
     */
    public boolean rename(RemoteFile dest) throws Exception {
        return rename(dest, STANDARD_RETRY_INTERVAL_NS);
    }

    public boolean renameNoRetry(RemoteFile dest) throws Exception {
        return rename(dest, 0L);
    }

    public boolean rename(final RemoteFile dest, long retryNs) throws Exception {
        Operation renameOp = new Operation() {
            public boolean act() throws Exception {
                return basicRename(dest);
            }
        };
        return retry(renameOp, retryNs);
        // return retry( () -> basicRename(dest), retryNs );
    }

    // Complete operation: Delete

    /**
     * Attempt to delete a file and all of its children.  The file may
     * be a simple file or a directory.  When the file is a directory,
     * recursively delete the file's children before deleting the file.
     *
     * Retry in case of a failure, up to the standard retry interval.
     *
     * @return True or false telling if the delete was successful.
     *     Answer true if the file already does not exist.
     *
     * @throws Exception Thrown if the delete failed unexpectedly.
     */
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

    // Provider implemented operations ...

    // Passive operations ...

    private boolean providerExists() throws Exception {
        return LocalProvider.exists(this);
    }

    private boolean providerIsDirectory() throws Exception {
        return LocalProvider.isDirectory(this);
    }

    private boolean providerIsFile() throws Exception {
        return LocalProvider.isFile(this);
    }

    public long providerLastModified() {
        return LocalProvider.lastModified(this);
    }

    public long providerLength() {
        return LocalProvider.length(this);
    }

    private String[] providerList() throws Exception {
        return LocalProvider.list(this);
    }

    // Active operations ...

    private InputStream providerOpenFileForReading() throws Exception {
        return LocalProvider.openFileForReading(this);
    }

    private OutputStream providerOpenFileForWriting(boolean append) throws Exception {
        return LocalProvider.openFileForWriting(this, append);
    }

    private boolean providerMkdir() throws Exception {
        return LocalProvider.mkdir(this);
    }

    private boolean providerMkdirs() throws Exception {
        return LocalProvider.mkdirs(this);
    }

    private boolean providerCopy(RemoteFile destFile, boolean binary) throws Exception {
        return LocalProvider.copy(this, destFile, binary);
    }

    private boolean providerMove(RemoteFile dest) throws Exception {
        return LocalProvider.move(this, dest);
    }    

    private boolean providerRename(RemoteFile dest) throws Exception {
        return LocalProvider.rename(this, dest);
    }

    private boolean providerDelete() throws Exception{
        return LocalProvider.delete(this);
    }
}
