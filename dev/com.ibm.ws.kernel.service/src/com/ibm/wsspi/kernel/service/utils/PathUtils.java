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
package com.ibm.wsspi.kernel.service.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.MalformedLocationException;

public class PathUtils {

    /**
     * Cache of whether the platform is a windows platform. Set by testing
     * if system property "os.name" contains the text "windows" (using a case-insensitive
     * test). Used by {@link #fixPathString(File)} and related methods.
     */
    private static final boolean isWindows = System.getProperty("os.name", "unknown").toUpperCase(Locale.ENGLISH).contains("WINDOWS");

    /**
     * Pattern used to determine if a URI is an absolute URI.
     *
     * The pattern is "^[^/#\\?]+?:/.*". The pattern is an approximation
     * for full URI matching.
     *
     * For example, the pattern matches "file:/" and "schema:/" as absolute URIs.
     */
    final static Pattern ABSOLUTE_URI = Pattern.compile("^[^/#\\?]+?:/.*");

    /**
     * Cache of whether the active file system is case insensitive.
     *
     * The result is computed by {@link #isOsCaseSensitive()}.
     */
    private static boolean IS_OS_CASE_SENSITIVE = isOsCaseSensitive();

    /**
     * File name restricted characters. Used by {@link #replaceRestrictedCharactersInFileName(String)}.
     *
     * The file name restricted characters are: &lt; &gt; \: \" \/ \\ \| \? \*
     *
     * Used by {@link #replaceRestrictedCharactersInFileName(String)}.
     */
    private static String FILE_NAME_RESTRICTED_CHARS = "<>:\"/\\|?*";

    /**
     *
     * @deprecated - Instead use !isOsCaseSensitive()
     */
    @Deprecated
    static boolean isPossiblyCaseInsensitive() {
        return !isOsCaseSensitive();
    }

    /**
     * Test whether the active file system is case <em>sensitive</em>. A true result means
     * that the active file system is case sensitive. A false result means
     * that the active file system is not case sensitive.
     *
     * The test accesses the bundle context of this class, and creates and removes files in the
     * persistent data storage area of that bundle context. If the bundle or bundle context are
     * not available, or if the persistent data storage area cannot be accessed, then we test the
     * file system directly (using the canonical name) by writing a file to the file system and
     * comparing the File to a File that differs only by case.
     *
     * The result is used when testing for file existence. See {@link #checkCase(File, String)}.
     *
     * @return True if the active file system is case sensitive, otherwise false.
     */
    static boolean isOsCaseSensitive() {
        File caseSensitiveFile = null;
        Bundle bundle = FrameworkUtil.getBundle(PathUtils.class);
        if (bundle != null) {
            BundleContext ctx = bundle.getBundleContext();
            if (ctx != null) {
                caseSensitiveFile = ctx.getDataFile("caseSensitive");
                if (caseSensitiveFile != null && (caseSensitiveFile.delete() || !caseSensitiveFile.exists())) {
                    try {
                        if (caseSensitiveFile.createNewFile()) {
                            // We created "caseSensitive", so check if "CASeSENSITIVE" exists.
                            // new File("A").equals(new File("a")) returns true on Windows, but
                            // OS/400 returns false even though the files are the same, so use
                            // getCanonicalFile() first, which allows the comparison to succeed.
                            //
                            // Note that OS/400 only considers two files equal if they have both
                            // been canonicalized...
                            return !getCanonicalFile(caseSensitiveFile).equals(getCanonicalFile(new File(caseSensitiveFile.getParentFile(), "CASEsENSITIVE")));
                        }
                    } catch (PrivilegedActionException pae) {
                        // auto FFDC
                    } catch (IOException ioe) {
                        // auto FFDC
                    } finally {
                        caseSensitiveFile.delete();
                    }
                }
            }
        }

        try {
            // Need to double check, since the above code is intended to be run in an
            // OSGi environment, not a Java SE / JUnit env
            caseSensitiveFile = File.createTempFile("caseSENSITIVEprefix", "TxT");
            boolean iAmCaseSensitive = !getCanonicalFile(caseSensitiveFile).equals(new File(caseSensitiveFile.getAbsolutePath().toUpperCase()));
            if (iAmCaseSensitive) {
                return true;
            }
        } catch (Exception e) {
            // We can't tell if this OS is case sensitive or not.
            // Assume we might not be case sensitive.
            return false;
        } finally {
            //caseSensitiveFile.delete();
        }
        // Something went wrong. Assume we might be not be case sensitive.
        return false;
    }

    /**
     * Copy the path, replacing backward slashes ('\\') with forward slashes ('/').
     *
     * @param path The path in which to replace slashes. An exception
     *                 will be thrown if the path is null.
     *
     * @return The path with backward slashes replaced with forward slashes.
     *         Answer the initial file path if no backward slashes are present.
     */
    @Trivial
    public static String slashify(String filePath) {
        // callers ensure never null
        return filePath.replace('\\', '/');
    }

    /**
     * Normalize a relative path using {@link #normalize(String)} and verify that
     * the normalized path does not begin with an upwards path element ("..").
     *
     * The path is required to be a relative path which uses forward slashes ('/').
     *
     * The normalized path is tested as an absolute path according to {@link #pathIsAbsolute(String)}.
     *
     * @param relativePath The relative path which is to be normalized.
     *
     * @return The normalized path. An empty path if the path was null.
     *
     * @throws MalformedLocationException Thrown if the normalized path starts with an upwards path element ("..").
     */
    @Trivial
    public static String normalizeDescendentPath(String path) {
        if (path == null || path.length() == 0)
            return "";

        path = normalizeRelative(path);

        if (path.startsWith(".."))
            throw new MalformedLocationException("Can not reference \"..\" when creating a descendant (path=" + path + ")");

        return path;
    }

    /**
     * Normalize a relative path using {@link #normalize(String)} and verify that the normalized path is
     * an absolute path. Answer the normalized path.
     *
     * The path is required to be a relative path which uses forward slashes ('/').
     *
     * The normalized path is an absolute path according to the rules implemented by {@link #pathIsAbsolute(String)}.
     *
     * @param relativePath The relative path which is to be normalized. An exception will
     *                         be thrown if the path is null.
     *
     * @return The normalized path.
     *
     * @throws MalformedLocationException Thrown if the normalized path is not an absolute path.
     */
    @Trivial
    public static String normalizeRelative(String relativePath) {
        relativePath = normalize(relativePath);

        if (pathIsAbsolute(relativePath))
            throw new MalformedLocationException("path must be relative (path=" + relativePath + ")");

        return relativePath;
    }

    /**
     * Tell if a normalized path is an absolute path.
     *
     * A normalized path which starts with a forward slash character ('/') is absolute.
     *
     * A normalized path which starts with a symbolic substitution (see {@link #isSymbol(String)} is absolute.
     *
     * A true result for paths which start with a symbolic substitution is a conservative
     * answer: Whether the resulting path is absolute after performing symbolic substitution
     * depends on what value was placed by the substitution.
     *
     * A normalized path which starts with a drive letter combination followed by a forward
     * slash character, for example, "C\:/", is absolute.
     *
     * A normalized path which starts with a protocol followed by a forward slash character,
     * for example, "file:/", is absolute.
     *
     * Otherwise, the path is not absolute.
     *
     * @param normalizedPath The normalized path which is to be tested.
     *                           An exception will be thrown if the path is null.
     *
     * @return True or false telling if the path is absolute.
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

    /**
     * Normalize a path. Remove all "." elements, and resolve all ".." elements.
     *
     * ".." elements are resolved by removing the ".." element along with the higher (preceding)
     * element. Leading ".." elements are not removed.
     *
     * Paths which represent resource locators (which start with "http:", "https:", or "ftp:")
     * are not modified. Paths which represent file resource locators (which start with "file:")
     * are modified. However, the "file:" prefix is preserved. Normalization is performed on the
     * text which follows the "file:" prefix.
     *
     * @param path The path which is to be normalized. An exception will be thrown if the path is null.
     *
     * @return The normalized path.
     */
    public static String normalize(String path) {
        // We don't want to normalize if this is not a file name. This could be improved, but
        // might involve some work.
        if ((path.startsWith("http:") || (path.startsWith("https:")) || (path.startsWith("safkeyring:")) || (path.startsWith("ftp:")))) {
            return path;
        }
        boolean slash_change = false;
        final String origFullPath = path;
        boolean pathChanged = false;

        String prefix = "";

        // remove the file:// prefix for UNC path before normalize
        // must preserve file:////UNC/Path w/ //UNC/path all in the "getPath"
        // portion of
        // URI.
        if (path.length() >= 9 && path.startsWith("file:////")) {
            prefix = "file://";
            path = path.substring(7); // skip "file://", new path will start with //
            slash_change = true;
            pathChanged = true;
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

        if (origLength != 0) {
            if (path.charAt(0) == '.') // potential dot segment
                dotSegment = true;

            for (; i < origLength; i++) {
                char c = path.charAt(i);

                if (c == '/' || c == '\\') {
                    if (c == '\\') // need to slashify
                        backslash = true;

                    if (i == 0) // catch leading slash(es) (whee! UNC paths: \\remote\location)
                    {
                        num_segments++;
                        while (i + 1 < origLength && (path.charAt(i + 1) == '/' || path.charAt(i + 1) == '\\'))
                            i++;

                        if (i > 0) // if there are extra leading slashes, note it
                            slash_change = true;
                    } else if (i == lastSlash) // catch repeated//slash in the middle of path
                        slash_change = true;
                    else if (i != lastSlash) // at least one character since the last slash:
                        // another path segment
                        num_segments++;

                    // leading dot in next segment could be a '.' or '..' segment
                    if (!dotSegment && i + 1 < origLength && path.charAt(i + 1) == '.')
                        dotSegment = true;

                    lastSlash = i + 1; // increment beyond the slash character
                }
            }

            if (i > lastSlash)
                num_segments++;

            if (backslash) {
                path = slashify(path);
                pathChanged = true;
            }

            // Remove leading slash: /c:/windows/path... have to adjust "original" length
            if (path.length() > 3 && path.charAt(0) == '/' && path.charAt(2) == ':') {
                path = path.substring(1);
                pathChanged = true;
            }
        }

        // if there are no segment-sensitive/collapsing elements, just return
        if (!slash_change && !dotSegment)
            return pathChanged ? prefix + path : origFullPath;

        pathChanged = false;
        origLength = path.length();

        ArrayList<String> segments = new ArrayList<String>(num_segments);
        for (lastSlash = 0, i = 0; i < origLength; i++) {
            if (path.charAt(i) == '/') {
                if (i == 0) // catch leading slash(es) (whee! UNC paths: //remote/location/)
                {
                    // Guard against a case where we have multiple slashes for a unix root
                    while ((i + 1) < origLength && path.charAt(i + 1) == '/')
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
            StringBuilder sb = new StringBuilder(origFullPath.length());
            sb.append(prefix);

            for (String s : segments)
                sb.append(s);

            return sb.toString();
        }

        return prefix + path;
    }

    // Leave these unused for now.  APICHK forces a version upgrade
    // if they are added.

    // /** Character used to start a symbolic substitution sequence. */
    // public static final char SYMBOL_START_CHARACTER = '$';

    // /** Character used to open the name section of a symbolic substitution sequence. */
    // public static final char SYMBOL_OPEN_CHARACTER = '{';

    // /** Character used to close the name section of a symbolic substitution sequence. */
    // public static final char SYMBOL_CLOSE_CHARACTER = '}';

    /**
     * Tell if a path starts with a symbolic substitution. A symbolic substitution is
     * the character sequence "\$\{\}" with at least one character between braces.
     *
     * Other than requiring at least one character, this method does not validate
     * the characters between the braces. This method does not validate that
     * a closing brace is present.
     *
     * @param path The path to test for a symbolic substitution.
     *
     * @return True or false telling if the path contains and starts with
     *         a symbolic substitution.
     */
    @Trivial
    public static boolean isSymbol(String s) {
        if (s.length() > 3 && s.charAt(0) == '$' && s.charAt(1) == '{')
            return true;

        return false;
    }

    /**
     * Tell if a path contains a symbolic substitution. A symbolic substitution
     * is the character sequence "\$\{\}" with at least one character between
     * braces.
     *
     * Other than requiring at least one character, this method does not validate
     * the characters between the braces.
     *
     * The return value is unpredictable for a path which contains a malformed substitution,
     * for example, "leading\$\{A/inner/\$\{B\}/trailing".
     *
     * @param path The path to test for a symbolic substitution.
     *
     * @return True or false telling if the path contains a symbolic substitution.
     */
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

    /**
     * Answer the first symbolic substitution within a path.
     *
     * See {@link #containsSymbol(String)} for a description of a symbolic substitution.
     *
     * Answer null if the path is null or if the path contains no symbolic substitution.
     *
     * For example, for "leading/trailing" answer null.
     *
     * For example, for "leading/\$\{A\}"/trailing" answer "\$\{A\}".
     *
     * For example, for "leading/\$\{A\}"/inner/\$\{B\}/trailing" answer "\$\{A\}".
     *
     * Paths with badly formed substitutions answer unpredictable values. For example,
     * for "leading\$\{A/inner/\$\{B\}/trailing" answer "\$\{A/inner/\$\{B\}".
     *
     * @param path The path from which to obtain the first symbol.
     *
     * @return The first symbol of the path. Null if the path is null or contains
     *         no symbolic substitutions.
     */
    @Trivial
    public static String getSymbol(String s) {
        String outputSymbol = null;
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
                                outputSymbol = s.substring(pos, pos2 + 1);
                            }
                        }
                    }
                }
            }
        }
        return outputSymbol;
    }

    /**
     * Answer a copy of the path with the trailing forward slash ('/') removed.
     * Answer the path itself if the path has no trailing forward slash.
     *
     * @param path The path from which to remove the trailing slash. An exception will
     *                 be thrown if the path is null.
     *
     * @return The path with any trailing forward slash removed.
     */
    @Trivial
    private static String trimSlash(String segment) {
        // due to where this is called from, segment will never be null
        int len = segment.length();

        if (len >= 1 && segment.charAt(len - 1) == '/')
            return segment.substring(0, len - 1);

        return segment;
    }

    /**
     * Tell if a file is a child of another. Answer true for both immediate and
     * non-immediate cases.
     *
     * An immediate case is, for example, "aParent/aChild".
     *
     * A non-immediate case is, for example, "aParent/anIntermediate/aChild".
     *
     * The test uses normalized absolute paths. See {@link #normalize(String)} and {@link File#getAbsolutePath()}.
     *
     * Answer false when applied to a single file as both the parent and the child file
     * (a non-reflexive parent-child relationship test is implemented).
     *
     * @param candidateParent The file which is tested as a parent. An exception will be
     *                            thrown if the path is null.
     * @param candidateChild  The file which is tested as a child. An exception will result
     *                            if the path is null.
     *
     * @return True or false telling if candidate child is a child of the candidate parent.
     */
    static boolean isFileAChild(File parent, File child) {
        String pNormalized = normalize(parent.getAbsolutePath());
        String cNormalized = normalize(child.getAbsolutePath());

        // If the child's normalized path length is <= to the the parent's,
        // it isn't a child
        if (cNormalized.length() <= pNormalized.length())
            return false;

        if (cNormalized.startsWith(pNormalized))
            return true;

        return false;
    }

    /**
     * Comparator for normalized paths.
     */
    @Trivial
    public static class PathComparator implements Comparator<String>, Serializable {
        /** Serial version ID. This does not have a default value. */
        private static final long serialVersionUID = 8845848986582493462L;

        /** The separator character for normalized paths. ('/'). */
        public static final char PATH_SEPARATOR = '/';

        /**
         * Comparison result for a less-than relationship. Matches the sign
         * of the result, but does not necessarily have the same absolute value
         * as the result.
         */
        private static final int CMP_LT = -1;

        /**
         * Comparison result for a greater-than relationship. Matches the sign
         * of the result, but does not necessarily have the same absolute value
         * of the result.
         */
        private static final int CMP_GT = +1;

        // Leave this out for now.  APICHK forces a version upgrade
        // if it is included, and we get a warning if it is included
        // as a private variable because it is unused.
        // /** Comparison result for an equals relationship. */
        // private static final int CMP_EQ = 0;

        /**
         * Compare two relative paths. Take into account the presence of path separator
         * characters. That is, the path are compared lists of path segments.
         *
         * The result will be one of {@link #CMP_LT}, {@link #CMP_EQ}, {@link #CMP_GT},
         * or, an integer value which measures the difference between the
         * two path (subtracting the second value from the first), with the sign of
         * the difference indicating the comparison result. In effect:
         *
         * <code>Math.signum(compare(path1, path2)) == Math.signum(path1 - path2)</code>
         *
         * "In effect", because an actual difference between two path values is not
         * defined. This implementation answers the difference between the first
         * characters which do not match, or the differences between the path lengths
         * if all characters of the shorter path match characters of the longer path,
         * or CMP_LT or CMP_GT if slashes are not in the same positions in both paths.
         *
         * For example, "parent/child" is compared as \{ "parent", "child" \}.
         *
         * Then, "parent/child" is less than "parentAlt/child", because
         * "parent" is less than "parentAlt".
         *
         * @param path1 The first relative path which is to be compared.
         *                  An exception will result if the path is null.
         * @param path2 The second second path which is to be compared.
         *                  An exception will result if the path is null.
         *
         * @return A integer value which corresponds to the comparison result.
         *         A value less than zero indicates that the first path is less than
         *         the second path. A value greater than zero indicates that first
         *         path is greater than the second. A value of zero indicates that
         *         the two paths are equal.
         */
        @Override
        @Trivial
        public int compare(String o1, String o2) {
            int len1 = o1.length(), l2 = o2.length();
            int minLen = Math.min(len1, l2);
            for (int i = 0; i < minLen; i++) {
                char c1 = o1.charAt(i), c2 = o2.charAt(i);
                if (c1 == c2)
                    continue;
                if (c1 == PATH_SEPARATOR)
                    return CMP_LT;
                if (c2 == PATH_SEPARATOR)
                    return CMP_GT;
                return c1 - c2;
            }
            // Strings differ in length only - shorter string should come first
            return len1 - l2;
        }
    }

    /** Singleton instance of {@link PathComparator}. */
    public static final Comparator<String> PATH_COMPARATOR = new PathComparator();

    /**
     * Answer the path with the last file name removed, using forward
     * slash ('/') as the path separator character.
     *
     * Answer null for a path which contains no slashes.
     *
     * Answer null for a path which is a single slash character.
     *
     * Answer the path which is a single slash character when
     * the path starts with a slash, which is not a single slash
     * character, and which contains no other slashes.
     *
     * Answer the path with the trailing slash removed for
     * a path which has a trailing slash and which is not a single
     * slash character.
     *
     * Answer the the path up to but not including the trailing
     * slash in all other cases.
     *
     * For example:
     *
     * For "/grandParent/parent/child" answer "/grandParent/parent".
     *
     * For "/grandParent/parent/" answer "/grandParent/parent".
     *
     * For "/parent" answer "/".
     *
     * For "/" answer null.
     *
     * For "child" answer null.
     *
     * For "" answer null.
     *
     * @param path The path with the last file named removed. An exception
     *                 will be thrown if the path is null.
     *
     * @return The path with the last file name removed.
     */
    public static String getParent(String path) {
        String parent = null;
        int lastIndex = path.lastIndexOf('/');
        if (lastIndex != -1) {
            if (path.length() == 1) {
                parent = null;
            } else if (lastIndex == 0) {
                parent = "/";
            } else {
                parent = path.substring(0, lastIndex);
            }
        }
        return parent;

    }

    /**
     * Answer the last file name of a path, using the forward slash ('/') as the
     * path separator character. Answer the path element which follows the last
     * forward slash of the path.
     *
     * Answer the entire path if the path contains no path separator.
     *
     * For example:
     *
     * For "/parent/child" answer "child".
     *
     * For "child" answer "child".
     *
     * An exception will be thrown if the path ends with a trailing slash.
     *
     * @param path The path from which to answer the last file name.
     *
     * @return The last file name of the path.
     */
    public static String getName(String pathAndName) {
        int i = pathAndName.lastIndexOf('/');
        int l = pathAndName.length();
        if (i == -1) {
            return pathAndName;
        } else if (l == i) {
            return "/";
        } else {
            return pathAndName.substring(i + 1);
        }
    }

    /**
     * Answer the first file name of a path, using the forward slash character ('/')
     * as the path separator character. Answer the path element which follows the last
     * forward slash of the path.
     *
     * Answer the entire path if the path contains no path separator.
     *
     * Answer the path element which precedes the first path separator,
     * except, ignore a leading path separator.
     *
     * For example:
     *
     * For "/parent/child" answer "parent".
     *
     * For "parent/child" answer "parent".
     *
     * For "parent" answer "parent".
     *
     * For "/parent" answer "parent".
     *
     * For "/" answer "".
     *
     * @param path The path from which to answer the first file name.
     *                 An exception will be thrown if the path is null.
     *
     * @return The first file name of the path.
     */
    public static String getFirstPathComponent(String path) {
        int sIdx = path.indexOf('/');

        //path has no /'s ? return path.
        if (sIdx == -1) {
            return path;
        }
        //path has 1 char or less, (if 1, must be a '/' by previous if), return ""
        if (path.length() <= 1) {
            return "";
        }
        //path has more than 1 char, starts with /, has no other /'s, return path without leading /.
        if (sIdx == 0 && path.indexOf('/', 1) == -1) {
            return path.substring(1);
        }

        if (sIdx != 0) {
            //path does not start with /, but has / present.
            //return up to but not including 1st / position.
            return path.substring(0, sIdx);
        } else {
            //path starts with /, has multiple /'s present
            //remove 1st char and return up to but not including 1st / position.
            return path.substring(1, path.indexOf('/', 1));
        }

    }

    /**
     * Answer the first path element of a path which follows a leading sub-path.
     *
     * For example, for path "/grandParent/parent/child/grandChild" and
     * leading sub-path "/grandParent/parent", answer "child".
     *
     * The result is unpredictable if the leading path does not start the
     * target path, and does not reach a separator character in the target
     * path. An exception will be thrown if the leading path is longer
     * than the target path.
     *
     * @param path        The path from which to obtain a path element.
     * @param leadingPath A leading sub-path of the target path.
     *
     * @return The first path element of the target path following the
     *         leading sub-path.
     */
    public static String getChildUnder(String path, String parentPath) {
        int start = parentPath.length();
        String local = path.substring(start, path.length());
        String name = getFirstPathComponent(local);
        return name;
    }

    /**
     * Tell if a path, if applied to a target location, will reach above
     * the target location. For example, "../siblingParent/child", when applied
     * to "grandParent/parent" reaches "grandParent/siblingParent/child",
     * which is not beneath the initial target location.
     *
     * The test resolves all ".." elements before performing the test. A test which
     * simply examines the starting character of the path is not sufficient. For
     * example, the path "parent/../../siblingParent/child" reaches above target
     * locations.
     *
     * The path must use forward slashes.
     *
     * @param path The path which is to be tested.
     *
     * @return True or false telling if the path reaches above target locations.
     */
    public static boolean isUnixStylePathAbsolute(String unixStylePath) {
        String nPath = normalizeUnixStylePath(unixStylePath);
        //System.out.println("unixStylePath " + unixStylePath + " normalized to " + nPath);
        //System.out.println("..".equals(nPath));
        //System.out.println(nPath.startsWith("../"));

        return isNormalizedPathAbsolute(nPath);
    }

    /**
     * Tell if a path, if applied to a target location, will reach above the
     * target location. The path must use forward slashes and must have
     * all ".." elements resolved.
     *
     * The path reaches above target locations if it is "..", or if it starts
     * with "../" or starts with "/..".
     *
     * @param normalizedPath The path which is to be tested.
     *
     * @return True or false telling if the path reaches above target locations.
     */
    public static boolean isNormalizedPathAbsolute(String nPath) {
        boolean retval = "..".equals(nPath) || nPath.startsWith("../") || nPath.startsWith("/..");
        //System.out.println("returning " + retval);
        return !retval;
    }

    /**
     * Resolve all ".." elements of a path. The path must use forward slashes.
     *
     * Verify that the path does not reach above target elements (see {@link #isUnixStylePathAbsolute(String)}.
     *
     * Add a leading slash if the path does not have one.
     *
     * Remove any trailing slash.
     *
     * Verify that the resulting path is neither empty nor a single slash character.
     *
     * @param path The path which is to be normalized.
     *
     * @return The normalized path. An exception will result if the path is null.
     *
     * @throws IllegalArgumentException If the resolved path is empty or has just a single slash,
     *                                      or if the resolved path reaches above target locations.
     */
    public static String checkAndNormalizeRootPath(String path) throws IllegalArgumentException {
        path = PathUtils.normalizeUnixStylePath(path);

        //check the path is not trying to go upwards.
        if (!PathUtils.isNormalizedPathAbsolute(path)) {
            throw new IllegalArgumentException();
        }

        //ZipFileContainer is always the root.
        //so all relative paths requested can be made into absolute by adding /
        if (!path.startsWith("/")) {
            path = '/' + path;
        }

        //remove trailing /'s
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.equals("/") || path.equals("")) {
            throw new IllegalArgumentException();
        }

        return path;
    }

    /**
     * Private enumeration used for state management when resolving ".."
     * elements in paths.
     */
    private enum ParseState {
        NEW_ELEM, IN_ELEM, ONE_DOT, TWO_DOTS
    }

    /**
     * Normalize a path, resolving as many ".." elements as possible.
     *
     * Do not remove a leading '/', but remove a trailing '/'.
     *
     * The path must use forward slashes as path separators.
     *
     * After resolution, all unresolved ".." elements will be at the beginning
     * of the path, possibly including a leading '/'.
     *
     * @param path A path which is to be normalized.
     *
     * @return The normalized path.
     */
    public static String normalizeUnixStylePath(String path) {
        // OK so this could be a couple of regexps, but this method needs to be FAST.
        // That is best done by walking backwards over a copy of the string, resolving
        // /.. sequences after they are encountered by skipping the next non-.. element.

        ///////////////////////////////////////////////////////////
        // NO PARSING - null or empty or single character string //
        ///////////////////////////////////////////////////////////

        final int pathLen;
        // cannot require any modification unless there are at least two characters.
        if (path == null || (pathLen = path.length()) < 2)
            return path;
        // start from the end of the path and walk to the start
        int pathIndex = pathLen - 1;
        // track the current parse state
        ParseState state = ParseState.NEW_ELEM;

        ///////////////////////////////////////////////
        // FAST PARSING - the path is in normal form //
        ///////////////////////////////////////////////

        FAST_PARSING: {
            // Loop BACKWARDS from the final character to the SECOND character.
            // If we get as far as the second character without needing to
            // do any transformation, then the first character can never make
            // any difference and the string will be returned without transformation.
            // Er... the above statement is not true in cases where there is a leading
            // double slash - i.e. //META-INF/resources/someFile.  We need to check the
            // first character in the path string.
            WALKING_BACKWARDS: for (pathIndex = pathLen - 1; pathIndex >= 0; pathIndex--) {
                final char c = path.charAt(pathIndex);
                switch (state) {
                    case NEW_ELEM:
                        switch (c) {
                            case '/':
                                // need to strip trailing or duplicate slashes
                                // break out to complex parsing
                                break FAST_PARSING;
                            case '.':
                                state = ParseState.ONE_DOT;
                                continue WALKING_BACKWARDS;
                            default:
                                state = ParseState.IN_ELEM;
                                continue WALKING_BACKWARDS;
                        }
                    case IN_ELEM:
                        if (c == '/')
                            state = ParseState.NEW_ELEM;
                        continue WALKING_BACKWARDS;
                    case ONE_DOT:
                        switch (c) {
                            case '/':
                                state = ParseState.NEW_ELEM;
                                continue WALKING_BACKWARDS;
                            case '.':
                                state = ParseState.TWO_DOTS;
                                continue WALKING_BACKWARDS;
                            default:
                                state = ParseState.IN_ELEM;
                                continue WALKING_BACKWARDS;
                        }
                    case TWO_DOTS:
                        switch (c) {
                            case '/':
                                // we've found a "/.." (and it isn't the whole path)
                                // rewind the last two characters (i.e. so the next loop starts at the first dot)
                                pathIndex += 2;
                                // mark that we are at the start of an element so the two dots get parsed correctly
                                state = ParseState.NEW_ELEM;
                                // now we've set up the required state, break out to complex parsing
                                break FAST_PARSING;
                            default:
                                // anything other than a slash means the two dots were just part of an element with no special meaning
                                state = ParseState.IN_ELEM;
                                continue WALKING_BACKWARDS;
                        }
                }
            }
            return path;
        }

        //////////////////////////////////////////////////
        // COMPLEX PARSING - path is not in normal form //
        //////////////////////////////////////////////////

        // start with a complete copy of the path - normal form cannot be longer than this
        char[] buffer = path.toCharArray();
        int offset = pathIndex + 1; // offset always points to start of result-so-far
        int elemsToSkip = 0;
        // Loop BACKWARDS from the current character to the SECOND character.
        // The final character is special and is dealt with at the end.
        WALKING_BACKWARDS: for (; pathIndex > 0; pathIndex--) {
            final char c = buffer[pathIndex];
            OUTER_HERE: switch (state) {
                case NEW_ELEM:
                    switch (c) {
                        case '/':
                            // strip duplicate slash
                            continue WALKING_BACKWARDS;
                        case '.':
                            // strip first dot of element, copy back later if it wasn't "/.."
                            state = ParseState.ONE_DOT;
                            continue WALKING_BACKWARDS;
                        default:
                            state = ParseState.IN_ELEM;
                            if (elemsToSkip > 0)
                                continue WALKING_BACKWARDS;
                            break OUTER_HERE;
                    }
                case IN_ELEM:
                    switch (c) {
                        case '/':
                            state = ParseState.NEW_ELEM;
                            if (elemsToSkip == 0)
                                break OUTER_HERE;
                            elemsToSkip--;
                            continue WALKING_BACKWARDS;
                        default:
                            if (elemsToSkip == 0)
                                break OUTER_HERE;
                            continue WALKING_BACKWARDS;
                    }
                case ONE_DOT:
                    switch (c) {
                        case '.':
                            // strip second dot of element, copy back later if it wasn't "/.."
                            state = ParseState.TWO_DOTS;
                            continue WALKING_BACKWARDS;
                        case '/':
                            state = ParseState.NEW_ELEM;
                            if (elemsToSkip == 0) {
                                // copy the one dot we didn't copy earlier
                                buffer[--offset] = '.';
                                break OUTER_HERE;
                            }
                            elemsToSkip--;
                            continue WALKING_BACKWARDS;
                        default:
                            state = ParseState.IN_ELEM;
                            if (elemsToSkip == 0) {
                                // copy the one dot we didn't copy earlier
                                buffer[--offset] = '.';
                                break OUTER_HERE;

                            }
                            continue WALKING_BACKWARDS;
                    }
                case TWO_DOTS:
                    switch (c) {
                        case '/':
                            state = ParseState.NEW_ELEM;
                            // just read in a "/.." so increase the number of elements to skip
                            elemsToSkip++;
                            // never copy this slash
                            continue WALKING_BACKWARDS;
                        default:
                            state = ParseState.IN_ELEM;
                            if (elemsToSkip == 0) {
                                // copy the two dots we just skipped
                                buffer[--offset] = '.';
                                buffer[--offset] = '.';
                                break OUTER_HERE;
                            }
                            continue WALKING_BACKWARDS;
                    }
            }
            buffer[--offset] = c;
            continue WALKING_BACKWARDS;
        } // end for loop

        ///////////////////////////////////////////////////////////
        // COMPLEX PARSING pt II - deal with the final character //
        ///////////////////////////////////////////////////////////

        final char c = buffer[0];
        final boolean pathHasLeadingSlash = c == '/';
        // check whether we just finished skipping an element
        // write in any dots that were previously ignored
        // this path has no further scope to collapse them
        switch (state) {
            case IN_ELEM: // FALL-THROUGH
                if (elemsToSkip == 0)
                    buffer[--offset] = c;
                else
                    elemsToSkip--;
                break;
            case ONE_DOT: // treat as IN_ELEM
                if (elemsToSkip == 0 || c == '.') {
                    buffer[--offset] = '.';
                    buffer[--offset] = c;
                } else {
                    elemsToSkip--;
                }
                break;
            case NEW_ELEM:
                if (elemsToSkip == 0) {
                    if ((offset == pathLen) || !!!pathHasLeadingSlash)
                        buffer[--offset] = c;
                } else if (!!!pathHasLeadingSlash) {// one-char path element being skipped
                    elemsToSkip--;
                }
                break;
            case TWO_DOTS:
                if (elemsToSkip == 0 || pathHasLeadingSlash) {
                    buffer[--offset] = '.';
                    buffer[--offset] = '.';
                    buffer[--offset] = c;
                } else {
                    elemsToSkip--;
                }
                break;
        }

        if (elemsToSkip == 0) {
            boolean resultHasLeadingSlash = offset < pathLen && buffer[offset] == '/';
            // ensure leading slash matches
            if (pathHasLeadingSlash ^ resultHasLeadingSlash)
                if (resultHasLeadingSlash)
                    offset++; //strip a leading slash
                else
                    buffer[--offset] = '/'; // add a leading slash
        } else {
            // ensure we have a leading slash or empty string before prepending ".."
            if (offset < pathLen && buffer[offset] != '/')
                buffer[--offset] = '/';
            // add in all but one unresolved "/.."
            while (--elemsToSkip > 0) {
                buffer[--offset] = '.';
                buffer[--offset] = '.';
                buffer[--offset] = '/';
            }
            // now write in the last .. or /..
            buffer[--offset] = '.';
            buffer[--offset] = '.';
            if (pathHasLeadingSlash)
                buffer[--offset] = '/';
        }
        return new String(buffer, offset, pathLen - offset);
    }

    /**
     * The artifact API is case sensitive even on a file system that is not case sensitive.
     *
     * This method will test that the case of the supplied <em>existing</em> file matches the case
     * in the pathToTest. It is assumed that you already tested that the file exists using
     * the pathToTest. Therefore, on a case sensitive files system, the case must match and
     * true is returned without doing any further testing. In other words, the check for file
     * existence is sufficient on a case sensitive file system, and there is no reason to call
     * this checkCase method.
     *
     * If the file system is not case sensitive, then a test for file existence will pass
     * even when the case does not match. So this method will do further testing to ensure
     * the case matches.
     *
     * It assumes that the final part of the file's path will be equal to
     * the whole of the pathToTest.
     *
     * The path to test should be a unix style path with "/" as the separator character,
     * regardless of the operating system. If the file is a directory then a trailing slash
     * or the absence thereof will not affect whether the case matches since the trailing
     * slash on a directory is optional.
     *
     * If you call checkCase(...) with a file that does NOT exist:
     * On case sensitive file system: Always returns true
     * On case insensitive file system: It compares the pathToTest to the file path of the
     * java.io.File that you passed in rather than the file on disk (since it doesn't exist).
     * file.getCanonicalFile() returns the path using the case of the file on disk, if it exists.
     * If the file doesn't exist then it returns the path using the case of the java.io.File itself.
     *
     * @param file       The existing file to compare against
     * @param pathToTest The path to test if it is the same
     * @return <code>true</code> if the case is the same in the file and the pathToTest
     */
    public static boolean checkCase(final File file, String pathToTest) {
        if (pathToTest == null || pathToTest.isEmpty()) {
            return true;
        }

        if (IS_OS_CASE_SENSITIVE) {
            // It is assumed that the file exists.  Therefore, its case must
            //  match if we know that the file system is case sensitive.
            return true;
        }

        try {
            // This will handle the case where the file system is not case sensitive, but
            // doesn't support symbolic links.  A canonical file path will handle this case.
            if (checkCaseCanonical(file, pathToTest)) {
                return true;
            }

            // We didn't know for sure that the file system is case sensitive and a
            // canonical check didn't pass. Try to handle both symbolic links and case
            // insensitive files together.
            return checkCaseSymlink(file, pathToTest);

        } catch (PrivilegedActionException e) {
            // We couldn't access the file system to test the path so must have failed
            return false;
        }
    }

    /**
     * Test that the path to a file matches a specified path.
     *
     * The test does a case sensitive string comparison of the canonical path
     * of the file with the specified path, restricting the form of the
     * path which may be used for the test.
     *
     * Trailing slashes on either the file or the path are ignored.
     *
     * @param file         The file which is to be tested.
     * @param trailingPath The path which is to be tested.
     *
     * @return True or false telling if the path reaches the file.
     *
     * @throws PrivilegedActionException Thrown if the caller does not
     *                                       have privileges to access the file or its ascending path.
     */
    private static boolean checkCaseCanonical(final File file, String pathToTest) throws PrivilegedActionException {
        // The canonical path returns the actual path on the file system so get this
        String onDiskCanonicalPath = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
            /** {@inheritDoc} */
            @Override
            public String run() throws IOException {
                return file.getCanonicalPath();
            }
        });
        onDiskCanonicalPath = onDiskCanonicalPath.replace("\\", "/");

        // The trailing / on a file name is optional so add it if this is a directory
        final String expectedEnding;
        if (onDiskCanonicalPath.endsWith("/")) {
            if (pathToTest.endsWith("/")) {
                expectedEnding = pathToTest;
            } else {
                expectedEnding = pathToTest + "/";
            }
        } else {
            if (pathToTest.endsWith("/")) {
                expectedEnding = pathToTest.substring(0, pathToTest.length() - 1);
            } else {
                expectedEnding = pathToTest;
            }
        }
        if (expectedEnding.isEmpty()) {
            // Nothing to compare so case must match
            return true;
        }

        return onDiskCanonicalPath.endsWith(expectedEnding);
    }

    /**
     * Test if a file is reached by a path. Handle symbolic links. The
     * test uses the canonical path of the file, and does a case sensitive
     * string comparison.
     *
     * Ignore a leading slash of the path.
     *
     * @param file         The file which is to be tested.
     * @param trailingPath The path which is to be tested.
     *
     * @return True or false telling if the path reaches the file.
     *
     * @throws PrivilegedActionException Thrown if the caller does not
     *                                       have privileges to access the file or its ascending path.
     */
    private static boolean checkCaseSymlink(File file, String pathToTest) throws PrivilegedActionException {
        // java.nio.Path.toRealPath(LinkOption.NOFOLLOW_LINKS) in java 7 seems to do what
        // we are trying to do here

        //On certain platforms, i.e. iSeries, the path starts with a slash.
        //Remove this slash before continuing.
        if (pathToTest.startsWith("/"))
            pathToTest = pathToTest.substring(1);

        String[] splitPathToTest = pathToTest.split("/");
        File symLinkTestFile = file;

        for (int i = splitPathToTest.length - 1; i >= 0; i--) {
            File symLinkParentFile = symLinkTestFile.getParentFile();
            if (symLinkParentFile == null) {
                return false;
            }

            // If the current file isn't a symbolic link, make sure the case matches using the
            // canonical file.  Otherwise get the parents list of files to compare against.
            if (!isSymbolicLink(symLinkTestFile, symLinkParentFile)) {
                if (!getCanonicalFile(symLinkTestFile).getName().equals(splitPathToTest[i])) {
                    return false;
                }
            } else if (!contains(symLinkParentFile.list(), splitPathToTest[i])) {

                return false;
            }

            symLinkTestFile = symLinkParentFile;
        }

        return true;
    }

    /**
     * Test if a file is a symbolic link. Test only the file.
     * A symbolic link elsewhere in the path to the file is not detected.
     *
     * Gets the canonical form of the parent directory and appends the file name.
     * Then compares that canonical form of the file to the "Absolute" file. If
     * it doesn't match, then it is a symbolic link.
     *
     * @param candidateChildFile  The file to test as a symbolic link.
     * @param candidateParentFile The immediate parent of the target file.
     *
     * @return True or false telling if the child file is a symbolic link
     *         from the parent file.
     *
     * @throws PrivilegedActionException Thrown in case of a failure to
     *                                       obtain a canonical file.
     */
    private static boolean isSymbolicLink(final File file, File parentFile) throws PrivilegedActionException {
        File canonicalParentDir = getCanonicalFile(parentFile);
        File fileInCanonicalParentDir = new File(canonicalParentDir, file.getName());
        File canonicalFile = getCanonicalFile(fileInCanonicalParentDir);

        return !canonicalFile.equals(fileInCanonicalParentDir.getAbsoluteFile());
    }

    /**
     * Obtain the canonical file for a file. Performs a call to {@link File#getCanonicalFile()},
     * wrapping the calling a {@link PrivelegedExceptionAction}.
     *
     * @param file The file for which to obtain the canonical file.
     *
     * @return The canonical file for the file.
     *
     * @throws PrivilegedActionException Thrown if the call to {@link File#getCanonicalFile()} threw an exception.
     */
    private static File getCanonicalFile(final File file) throws PrivilegedActionException {
        return AccessController.doPrivileged(new PrivilegedExceptionAction<File>() {

            @Override
            public File run() throws Exception {
                return file.getCanonicalFile();
            }
        });
    }

    /**
     * Tell if a file name is present in an array of file names. Test
     * file names using case sensitive {@link String#equals(Object)}.
     *
     * The parameter file names collection is expected to be obtained from
     * a call to {@link File#list()}, which can return null.
     *
     * @param fileNames The file names to test against. The array may
     *                      be null, but may not contain null elements.
     *
     * @param fileName  The file name to test. May be null.
     *
     * @return True or false telling if the file name matches any of
     *         the file names. False if the file names array is null.
     */
    @Trivial
    private static boolean contains(String[] fileList, String fileName) {
        if (fileList != null) {
            for (String name : fileList) {
                if (name.equals(fileName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Copy a file name, replacing each restricted character with a single period
     * character ('.').
     *
     * Restricted characters are control characters and the following:
     *
     * <pre>
     * &lt; &gt; : " / \\ | ? *
     * </pre>
     *
     * (See {@link #FILE_NAME_RESTRICTED_CHARS}.
     *
     * Control characters are character with a value greater than or equal to zero
     * and less than or equal to 31.
     *
     * The set of restricted characters was selected conservatively based on the
     * rules for Windows file names.
     *
     * The parameter should be a file name. Both the forward slash ('/') and
     * the backwards slash ('\\') are restricted characters.
     *
     * Answer null if all characters of the file name are replaced, or if the resulting
     * file name is "." or "..".
     *
     * @param name The file name in which to replace restricted characters. An exception
     *                 will be thrown if the file name is null.
     *
     * @return The copy of the file name with all restricted characters replaced. Null
     *         if all characters were replaced or of the replaced file name is "." or "..".
     */
    public static String replaceRestrictedCharactersInFileName(String name) {
        int replacedChars = 0;
        StringBuilder sb = new StringBuilder(name.length());
        for (int x = 0; x < name.length(); x++) {
            char y = name.charAt(x);
            if ((FILE_NAME_RESTRICTED_CHARS.indexOf(y) == -1) &&
                ((y < 0) || (y > 31))) /* Exclude control characters */ {
                sb.append(y);
            } else {
                sb.append('.');
                replacedChars++;
            }
        }

        String processedName = sb.toString();
        if ((processedName.length() <= 2) && (replacedChars > 0) &&
            (processedName.equals(".") || processedName.equals(".."))) {
            return null;
        }

        return ((name.length() - replacedChars) > 0) ? processedName : null;
    }

    /**
     * Answer a canonical path for a target path using {@link #fixPathString(File)}.
     *
     * @param targetPath The path for which to obtain a canonical path.
     *
     * @return The canonical path for the target path.
     */
    public static String fixPathString(String absPath) {
        return fixPathString(new File(absPath));
    }

    /**
     * Answer the canonical path of a file. How the canonical path is found depends
     * on the current active environment: On a Windows environment, attempt to
     * obtain the canonical value using {@link File#getCanonicalPath()}. If that
     * fails, obtain the canonical value using {@link File#getAbsolutePath()}. On
     * a non-windows environment, obtain the canonical value using {@link File#getAbsolutePath()}.
     *
     * A failure of {@link File#getCanonicalPath()} causes an FFDC exception
     * report to be generated. However, processing continues with the call to {@link File#getAbsolutePath()}.
     *
     * Use of canonical paths enables the use of java case sensitive string comparisons
     * for file name comparisons. See {@link #IS_OS_CASE_SENSITIVE}.
     *
     * @param targetFile The file for which to answer the canonical path.
     *
     * @return The canonical path for the file.
     */
    @FFDCIgnore(IOException.class)
    public static String fixPathString(File absPath) {
        //on windows.. the path from config can have a different case to the actual filesystem path
        //and when notifications come back, they'll come back with the actual filsystem casing.
        //so, so ensure we all agree on what that should be, for windows, we use canonical path.
        //
        //getCanonicalPath however, sees 'through' symlinks, which means if we use it on paths
        //that are symlinked into the app, we'll see the actual paths instead of the symlinked ones.
        //
        //getCanonicalPath on windows however just happens not to be able to see through windows
        //symlinks (which are infrequently used anyways), so we're still safe to use it there, and
        //non-windows platforms are just fine sticking with the casing of the path supplied via
        //config.
        //
        //A future alternative will be to use File.toPath().getRealPath(LinkOptions.NOFOLLOW_LINKS);
        //when Liberty is java7 only..
        if (isWindows) {
            try {
                return absPath.getCanonicalPath();
            } catch (IOException e) {
                // Bad path. Try the simpler normalization (which always works), but this may fail elsewhere.
                // Note manual FFDC. Apparently injection is not applied here.
                FFDCFilter.processException(e, PathUtils.class.getName(), "67");
                return absPath.getAbsolutePath();
            }
        } else {
            return absPath.getAbsolutePath();
        }
    }

    /**
     * Apply {@link #fixPathString(File)} to a collection of paths. Create a file
     * on each of the adjusted paths. Collect and return the files created on the
     * adjusted paths.
     *
     * Answer an empty collection if the paths collection is null.
     *
     * @param paths The paths to which to apply {@link #fixPathString(String)}.
     *
     * @return The files created from the adjusted paths.
     */
    public static Set<File> getFixedPathFiles(Collection<String> paths) {
        if (paths == null || paths.isEmpty())
            return Collections.emptySet();
        Set<File> result = new HashSet<File>(paths.size());
        for (String p : paths)
            result.add(new File(fixPathString(p)));
        return result;
    }

    /**
     * Collect the results of applying {@link #fixPathString(File)} to
     * a collection of files.
     *
     * Answer an empty collection if the file collection is null.
     *
     * @param files The files to which to apply {@link #fixPathString(File)}.
     *
     * @return The collected files.
     */
    public static Set<File> fixPathFiles(Collection<File> files) {
        if (files == null || files.isEmpty())
            return Collections.emptySet();
        Set<File> result = new HashSet<File>(files.size());
        for (File f : files)
            result.add(new File(fixPathString(f)));
        return result;
    }
}
