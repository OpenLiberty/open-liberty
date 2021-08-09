/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory;

/**
 * A set of utilitis for working with Files
 */
public class FileUtils {
    protected static final int DEFAULT_BUFFER_SIZE = 8192;

    /** file:/ or some schema:/ **/
    final static Pattern ABSOLUTE_URI = Pattern.compile("^[^/#\\?]+?:/.*");

    /**
     * List files according to the patterns and the pattern type
     *
     * @param target
     * @param patterns
     * @param include
     * @return
     */
    public static File[] listFiles(final File target, final List<Pattern> patterns, final boolean include) {
        if (patterns == null || patterns.isEmpty())
            return target.listFiles();

        return target.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                for (Pattern pattern : patterns) {
                    Matcher matcher = pattern.matcher(name);
                    if (matcher.matches())
                        return include;
                }
                return !include;
            }

        });

    }

    /**
     * Copy from one file to the other
     *
     * @param dest
     * @param source
     * @throws IOException
     */
    public static void copyFile(File dest, File source) throws IOException {
        InputStream input = null;
        try {
            input = new FileInputStream(source);
            createFile(dest, input);
        } finally {
            Utils.tryToClose(input);
        }
    }

    /**
     * Read the content from an inputStream and write out to the other file
     *
     * @param dest
     * @param sourceInput
     * @throws IOException
     */
    public static void createFile(final File dest, final InputStream sourceInput) throws IOException {
        if (sourceInput == null || dest == null)
            return;

        FileOutputStream fos = null;
        try {
            if (!dest.getParentFile().exists()) {
                if (!dest.getParentFile().mkdirs()) {
                    throw new FileNotFoundException();
                }
            }
            fos = TextFileOutputStreamFactory.createOutputStream(dest);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

            int count = -1;
            while ((count = sourceInput.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.flush();
        } finally {
            Utils.tryToClose(fos);
            Utils.tryToClose(sourceInput);
        }

    }

    /**
     * Read the content from an inputStream and write to the end of the dest file
     *
     * @param dest
     * @param sourceInput
     * @throws IOException
     */
    public static void appendFile(final File dest, final InputStream sourceInput) throws IOException {
        if (sourceInput == null || dest == null)
            return;

        FileOutputStream fos = null;
        try {
            if (!dest.getParentFile().exists()) {
                throw new FileNotFoundException();
            }
            fos = TextFileOutputStreamFactory.createOutputStream(dest, true);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

            int count = -1;
            while ((count = sourceInput.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.flush();
        } finally {
            Utils.tryToClose(fos);
            Utils.tryToClose(sourceInput);
        }

    }

    /**
     * Recursively copy the files from one dir to the other.
     *
     * @param from The directory to copy from, must exist.
     * @param to   The directory to copy to, must exist, must be empty.
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static void copyDir(File from, File to) throws FileNotFoundException, IOException {
        File[] files = from.listFiles();

        if (files != null) {
            for (File ff : files) {
                File tf = new File(to, ff.getName());
                if (ff.isDirectory()) {
                    if (tf.mkdir()) {
                        copyDir(ff, tf);
                    }
                } else if (ff.isFile()) {
                    copyFile(tf, ff);
                }
            }
        }
    }

    /**
     * Create the temporary file the parent directory is create by follow rules:
     * 1 The parentDir is relative, will create the java.io.tempdir/parentDir/tempFile;
     * 2 The parentDir is absolute, will create the parentDir/tempFile;
     * 3 The parentDir is null, will create the java.io.tempdir/tempFile.
     *
     * @param prefix
     * @param suffix
     * @param parentDir
     * @return
     * @throws IOException
     */
    public static File createTempFile(String prefix, String suffix, String parentDir) throws IOException {
        File tempFile = null;
        if (parentDir != null) {
            File parent = new File(parentDir);
            if (!parent.isAbsolute()) {
                String systemTemp = System.getProperty("java.io.tmpdir");
                parent = new File(systemTemp, parentDir);
                if (!parent.exists()) {
                    if (!parent.mkdirs()) {
                        return null;
                    }
                }
            }
            tempFile = File.createTempFile(prefix, suffix, parent);
        } else {
            tempFile = File.createTempFile(prefix, suffix);
        }
        return tempFile;
    }

    /**
     * If child is under parent, will return true, otherwise, return false.
     *
     * @param child
     * @param parent
     * @return
     */
    public static boolean isUnderDirectory(File child, File parent) {
        if (child == null || parent == null)
            return false;

        URI childUri = child.toURI();
        URI relativeUri = parent.toURI().relativize(childUri);
        return relativeUri.equals(childUri) ? false : true;

    }

    /**
     * Create the directory
     *
     * @param dir
     * @return
     */
    public static boolean createDir(File dir) {
        if (!dir.exists() && dir.mkdir()) {
            return true;
        }
        return false;
    }

    /**
     * Convert the file path string to a file.
     * If the filePath starts with a wlp pre-defined location symbol, will translate it to an absolute file path;
     * If the filePath is a relative path, the File returned will be relative to the server's directory (as provided by
     * {@link com.ibm.ws.kernel.boot.BootstrapConfig#getConfigFile(String) BootstrapConfig.getSeverFile(filePath)} If an unknown location symbol is used in the filePath, an
     * IllegalArgument Exception will be thrown.
     *
     * @param filePath
     * @return
     * @exception IllegalArgumentException if the location symbol could not be resolved
     */
    public static File convertPathToFile(String filePath, BootstrapConfig bootProps) {
        if (filePath == null) {
            throw new NullPointerException();
        }
        File resolvedFile = null;

        String resolvedPath = normalizeFilePath(bootProps.replaceSymbols(filePath));
        resolvedFile = new File(resolvedPath);
        if (!resolvedFile.isAbsolute()) {
            resolvedFile = bootProps.getConfigFile(resolvedPath);
        }
        return resolvedFile;
    }

    private static String normalizeFilePath(String filePath) {
        if (filePath.contains("\\/")) {// replace "\/" by File.separator
            filePath = filePath.replace("\\/", File.separator);
        } else if (filePath.contains("/\\")) {// replace "/\" by File.separator
            filePath = filePath.replace("/\\", File.separator);
        }

        // the Linux platform could not get the file correctly if the file separator is "\", not sure other *nix could handle it correctly, so use
        // File.separatorChar == '/' to judge
        if (File.separatorChar == '/') {
            filePath = filePath.replace("\\", File.separator);
        }
        return filePath;
    }

    /**
     * Normalize a relative entry path, so that it can be used in an archive.
     *
     * @param entryPath
     * @return
     */
    public static String normalizeEntryPath(String entryPath) {
        if (entryPath == null || entryPath.isEmpty())
            return "";

        entryPath = entryPath.replace("\\", "/");

        if (entryPath.startsWith("/")) {
            if (entryPath.length() == 1) {
                entryPath = "";
            } else {
                entryPath = entryPath.substring(1, entryPath.length());
            }
        }

        return entryPath;
    }

    /**
     * Normalize a path that represents a directory
     *
     * @param dirPath
     * @return
     */
    public static String normalizeDirPath(String dirPath) {
        if (dirPath == null || dirPath.isEmpty())
            return "";

        dirPath = dirPath.replace("\\", "/");

        if (!dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }

        return dirPath;
    }

    /**
     * Normalize the case of the drive letter on Windows.
     *
     * @param path a path, possibly absolute
     * @return the path with a normalized drive letter
     */
    public static String normalizePathDrive(String path) {
        if (File.separatorChar == '\\' && path.length() > 1 && path.charAt(1) == ':' && path.charAt(0) >= 'a' && path.charAt(0) <= 'z') {
            path = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return path;
    }

    /**
     * Strip "." and ".." elements from path.
     *
     * @param path
     * @return
     */
    public static String normalize(String path) {
        // We don't want to normalize if this is not a file name. This could be improved, but
        // might involve some work.
        if ((path.startsWith("http:") || (path.startsWith("https:")) || (path.startsWith("ftp:")))) {
            return path;
        }
        boolean slash_change = false;

        String prefix = "";

        // remove the file:// prefix for UNC path before normalize
        // must preserve file:////UNC/Path w/ //UNC/path all in the "getPath"
        // portion of
        // URI.
        if (path.length() >= 9 && path.startsWith("file:////")) {
            prefix = "file://";
            path = path.substring(7); // skip "file://", new path will start with //
            slash_change = true;
        }
        if (path.length() >= 9 && path.startsWith("file:///")) {
            prefix = "file:///";
            path = path.substring(8);
        } else if (path.length() >= 6 && path.startsWith("file:/") && path.charAt(7) != '/') {
            prefix = "file:/";
            path = path.substring(6);
        }

        int origLength = path.length();

        int i = 0;
        int j = 0;
        int num_segments = 0;
        int lastSlash = 0;
        boolean dotSegment = false;
        boolean backslash = false;

        for (; i < origLength; i++) {
            if (i == 0 && path.charAt(i) == '.') // potential dot segment
                dotSegment = true;

            if (path.charAt(i) == '/' || path.charAt(i) == '\\') {
                if (path.charAt(i) == '\\') // need to slashify
                    backslash = true;

                if (i == 0) // catch leading slash(es) (whee! UNC paths: \\remote\location)
                {
                    num_segments++;
                    while (i + 1 < path.length() && (path.charAt(i + 1) == '/' || path.charAt(i + 1) == '\\'))
                        i++;

                    if (i > 0) // if there are extra leading slashes, note it
                        slash_change = true;
                } else if (i == lastSlash) // catch repeated//slash in the middle of path
                    slash_change = true;
                else if (i != lastSlash) // at least one character since the last slash:
                    // another path segment
                    num_segments++;

                // leading dot in next segment could be a '.' or '..' segment
                if (!dotSegment && i + 1 < path.length() && path.charAt(i + 1) == '.')
                    dotSegment = true;

                lastSlash = i + 1; // increment beyond the slash character
            }
        }

        if (i > lastSlash)
            num_segments++;

        if (backslash)
            path = slashify(path);

        // Remove leading slash: /c:/windows/path... have to adjust "original" length
        if (path.length() > 3 && path.charAt(0) == '/' && path.charAt(2) == ':')
            path = path.substring(1);

        // if there are no segment-sensitive/collapsing elements, just return
        if (!slash_change && !dotSegment)
            return prefix + path;

        boolean pathChanged = false;
        origLength = path.length();

        ArrayList<String> segments = new ArrayList<String>(num_segments);
        for (lastSlash = 0, i = 0; i < origLength; i++) {
            if (path.charAt(i) == '/') {
                if (i == 0) // catch leading slash(es) (whee! UNC paths: //remote/location/)
                {
                    while (path.charAt(i + 1) == '/')
                        i++;

                    segments.add(path.substring(0, i + 1));
                } else if (i == lastSlash) // ignore extra slashes
                    pathChanged = true;
                else if (i != lastSlash)
                    segments.add(path.substring(lastSlash, i + 1));

                lastSlash = i + 1; // increment beyond the slash character
            }
        }

        if (i > lastSlash)
            segments.add(path.substring(lastSlash));

        int ns = segments.size() - 1;

        for (i = ns; i >= 0; i--) {
            String s = segments.get(i);
            String t = trimSlash(s);

            if (i >= 1 && t.equals("..")) {
                j = i - 1;
                String p = segments.get(j);
                String q = trimSlash(p);
                if (q.equals(".")) {
                    pathChanged = true;
                    // ./.. is really ..
                    segments.remove(j);
                } else if (!q.equals("..") && !isSymbol(q)) {
                    pathChanged = true;
                    segments.remove(i);
                    segments.remove(j);
                }
                // with i-- we should see the .. again on the next round which we need for ../..
                if (segments.size() == j)
                    i = j;
            } else if (t.equals(".")) {
                pathChanged = true;
                segments.remove(i);
            }
        }

        if (pathChanged) {
            StringBuilder sb = new StringBuilder(path.length());

            for (String s : segments)
                sb.append(s);

            return prefix + sb.toString();
        }

        return prefix + path;
    }

    /**
     * Tests if the input string start with the ${ symbolic characters.
     *
     * @param s The string to test.
     * @return True if the input string starts with the ${ symbolic characters. False otherwise.
     */
    @Trivial
    public static boolean isSymbol(String s) {
        if (s.length() > 3 && s.charAt(0) == '$' && s.charAt(1) == '{')
            return true;

        return false;
    }

    /**
     * Test if a path is absolute.<p>
     * Returns true if the path is an absolute one.<br>
     * Eg. c:/wibble/fish, /wibble/fish, ${wibble}/fish
     *
     * @param normalizedPath
     * @return true if absolute, false otherwise.
     */
    @Trivial
    public static boolean pathIsAbsolute(String normalizedPath) {
        // Absolute path with leading /
        if (normalizedPath.length() > 0 && normalizedPath.charAt(0) == '/')
            return true;

        // Absolute path with symbolic
        if (isSymbol(normalizedPath))
            return true;

        if (normalizedPath.contains(":")) {
            // Absolute windows path with leading c:/
            if (normalizedPath.length() > 3 && normalizedPath.charAt(1) == ':' && normalizedPath.charAt(2) == '/')
                return true;

            // Absolute path with scheme, e.g. file://whatever
            Matcher m = ABSOLUTE_URI.matcher(normalizedPath);
            if (m.matches())
                return true;
        }

        return false;
    }

    @Trivial
    private static String trimSlash(String segment) {
        // due to where this is called from, segment will never be null
        int len = segment.length();

        if (len >= 1 && segment.charAt(len - 1) == '/')
            return segment.substring(0, len - 1);

        return segment;
    }

    /**
     * Convert \ separators into /'s
     *
     * @param filePath path to process, must not be null.
     * @return filePath with \'s converted to /.
     */
    @Trivial
    public static String slashify(String filePath) {
        // callers ensure never null
        return filePath.replace('\\', '/');
    }

    /**
     * Recursively delete directory: used to clean up for clean start.
     *
     * @param fileToRemove
     *                         Name of file/directory to delete. If the File is a directory,
     *                         all sub-directories and files will also be deleted except for
     *                         the server lock and server running file.
     *
     *                         This method will return false if the file can not be read or deleted.
     *
     * @return true if the clean succeeded, false otherwise
     */
    public static boolean recursiveClean(final File fileToRemove) {
        if (fileToRemove == null)
            return true;

        Boolean fileExists = Boolean.FALSE;
        final File f_fileToRemove = fileToRemove;

        try {
            fileExists = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    if (f_fileToRemove.exists()) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }
            });
        } catch (Exception ex) {
        }

        if (fileExists != null && !(fileExists.booleanValue()))

            //if (!fileToRemove.exists())
            return true;

        boolean success = true;

        Boolean fileIsDirectory = null;
        //final File f_fileToRemove = fileToRemove;
        try {
            fileIsDirectory = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws Exception {
                    if (f_fileToRemove.isDirectory()) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }
            });
        } catch (Exception ex) {
        }

        if (fileIsDirectory != null && fileIsDirectory.booleanValue()) {
            //if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            // listFiles may return null if we lack read permissions
            if (files == null)
                return false;
            for (File file : files) {
                if (file.isDirectory()) {
                    success |= recursiveClean(file);
                } else {
                    String candidate = file.getName();
                    String candidateParent = fileToRemove.getName();
                    if ((BootstrapConstants.S_LOCK_FILE.equals(candidate) ||
                         BootstrapConstants.SERVER_RUNNING_FILE.equals(candidate))
                        &&
                        (BootstrapConstants.LOC_AREA_NAME_WORKING.equals(candidateParent) ||
                         BootstrapConstants.LOC_AREA_NAME_WORKING_UTILS.equals(candidateParent))) {
                        // skip/preserve workarea/.sLock and workarea/.sRunning files, including those
                        // in embedded server workarea
                    } else {
                        success |= file.delete();
                    }
                }
            }
            files = fileToRemove.listFiles();
            if (files.length == 0)
                success |= fileToRemove.delete();
        } else {

            Boolean successful_delete = Boolean.TRUE;
            final File ftr = fileToRemove;
            try {
                successful_delete = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Boolean>() {
                    @Override
                    public Boolean run() throws Exception {
                        Boolean s = ftr.delete();
                        return s;
                    }
                });
            } catch (Exception ex) {
            }

            //success |= fileToRemove.delete();
            success |= successful_delete.booleanValue();
        }
        return success;
    }

    /**
     * A method that translates a "file:" URL into a File.
     *
     * @param url
     *                the "file:" URL to convert into a file name
     * @return file derived from URL
     */
    public static File getFile(URL url) {
        String path;
        try {
            // The URL for a UNC path is file:////server/path, but the
            // deprecated File.toURL() as used by java -jar/-cp incorrectly
            // returns file://server/path/, which has an invalid authority
            // component.  Rewrite any URLs with an authority ala
            // http://wiki.eclipse.org/Eclipse/UNC_Paths
            if (url.getAuthority() != null) {
                url = new URL("file://" + url.toString().substring("file:".length()));
            }

            path = new File(url.toURI()).getPath();
        } catch (MalformedURLException e) {
            path = null;
        } catch (URISyntaxException e) {
            path = null;
        } catch (IllegalArgumentException e) {
            path = null;
        }

        if (path == null) {
            // If something failed, assume the path is good enough.
            path = url.getPath();
        }

        return new File(normalizePathDrive(path));
    }

    @Trivial
    public static boolean containsSymbol(String s) {
        if (s != null) {
            if (s.length() > 3) {
                // ${} .. look for $
                int pos = s.indexOf('$');
                if (pos >= 0) {
                    // look for { after $
                    int pos2 = pos + 1;
                    if (s.length() > pos2) {
                        if (s.charAt(pos2) == '{') {
                            // look for } after {
                            pos2 = s.indexOf('}', pos2);
                            if (pos2 >= 0) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Trivial
    public static String readFile(File f) {
        if (f == null || !f.exists() || !f.canRead())
            return null;

        try {
            Scanner s = new Scanner(f);
            try {
                // The '\\Z' delimiter is the end of string anchor, which allows the entire file
                // to be scanned in as a single string
                return s.useDelimiter("\\Z").next();
            } finally {
                s.close();
            }
        } catch (Exception e) {
            return null;
        }

    }

    @Trivial
    public static boolean isWSL() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("linux") &&
               System.getProperty("os.version").toLowerCase(Locale.ENGLISH).contains("microsoft");
    }
}
