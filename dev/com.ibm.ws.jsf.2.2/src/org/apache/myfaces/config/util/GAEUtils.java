/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.config.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.shared.util.StringUtils;

/**
 * Utility methods to use in Google Application Engine (GAE)
 * 
 * @author Leonardo Uribe
 */
public class GAEUtils
{

    public static final String WEB_LIB_PREFIX = "/WEB-INF/lib/";
    

    /**
     * Look in all jars located inside /WEB-INF/lib/ folder for files that has
     * some specified prefix and suffix. It is a simplification that can be done
     * in GAE, because no JSF libraries are outside /WEB-INF/lib
     *
     * @param context
     * @param classloader
     * @param prefix
     * @param suffix
     * @return
     * @throws IOException
     */
    public static Collection<URL> searchInWebLib(
            ExternalContext context, ClassLoader classloader, String filter, 
            String prefix, String suffix) throws IOException
    {
        if (!filter.equals("none"))
        {
            String[] jarFilesToScan = StringUtils.trim(StringUtils.splitLongString(filter, ','));
            Set<URL> urlSet = null;
            Set<String> paths = context.getResourcePaths(WEB_LIB_PREFIX);
            if (paths != null)
            {
                for (Object pathObject : paths)
                {
                    String path = (String) pathObject;
                    if (path.endsWith(".jar") && wildcardMatch(path, jarFilesToScan, WEB_LIB_PREFIX))
                    {
                        // GAE does not use WAR format, so the app is just uncompressed in a directory
                        // What we need here is just take the path of the file, and open the file as a
                        // jar file. Then, if the jar should be scanned, try to find the required file.
                        URL jarUrl = new URL("jar:" + context.getResource(path).toExternalForm() + "!/");
                        JarFile jarFile = JarUtils.getJarFile(jarUrl);

                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements())
                        {
                            JarEntry entry = entries.nextElement();
                            if (entry.isDirectory())
                            {
                                continue; // This is a directory
                            }
                            String name = entry.getName();
                            if (!name.startsWith(prefix))
                            {
                                continue; // Attribute files
                            }
                            if (name.endsWith(suffix))
                            {
                                // Get it from classloader, because no URL can be
                                // derived from JarEntry
                                Enumeration<URL> alternateFacesConfigs = classloader.getResources(name);
                                while (alternateFacesConfigs.hasMoreElements())
                                {
                                    if (urlSet == null)
                                    {
                                        urlSet = new TreeSet<URL>();
                                    }
                                    urlSet.add(alternateFacesConfigs.nextElement());
                                }
                            }
                        }
                    }
                }
            }
            return urlSet;
        }
        return null;
    }

    public static boolean wildcardMatch(String filename, String[] wildcardMatchers, String prefix)
    {
        for (String matcher : wildcardMatchers)
        {
            if (wildcardMatch(filename, prefix + matcher))
            {
                return true;
            }
        }
        return false;
    }
    
    // NOTE: CODE TAKEN FROM COMMONS-IO AND REFACTORED TO USE INSIDE GAE
    //-----------------------------------------------------------------------
    /**
     * Checks a filename to see if it matches the specified wildcard matcher,
     * always testing case-sensitive. <p> The wildcard matcher uses the
     * characters '?' and '*' to represent a single or multiple (zero or more)
     * wildcard characters. This is the same as often found on Dos/Unix command
     * lines. The check is case-sensitive always.
     * <pre>
     * wildcardMatch("c.txt", "*.txt")      --> true
     * wildcardMatch("c.txt", "*.jpg")      --> false
     * wildcardMatch("a/b/c.txt", "a/b/*")  --> true
     * wildcardMatch("c.txt", "*.???")      --> true
     * wildcardMatch("c.txt", "*.????")     --> false
     * </pre> N.B. the sequence "*?" does not work properly at present in match
     * strings.
     *
     * @param filename the filename to match on
     * @param wildcardMatcher the wildcard string to match against
     * @return true if the filename matches the wilcard string
     * @see IOCase#SENSITIVE
     */
    static boolean wildcardMatch(String filename, String wildcardMatcher)
    {
        return wildcardMatch(filename, wildcardMatcher, IOCase.SENSITIVE);
    }

    /**
     * Checks a filename to see if it matches the specified wildcard matcher
     * using the case rules of the system. <p> The wildcard matcher uses the
     * characters '?' and '*' to represent a single or multiple (zero or more)
     * wildcard characters. This is the same as often found on Dos/Unix command
     * lines. The check is case-sensitive on Unix and case-insensitive on
     * Windows.
     * <pre>
     * wildcardMatch("c.txt", "*.txt")      --> true
     * wildcardMatch("c.txt", "*.jpg")      --> false
     * wildcardMatch("a/b/c.txt", "a/b/*")  --> true
     * wildcardMatch("c.txt", "*.???")      --> true
     * wildcardMatch("c.txt", "*.????")     --> false
     * </pre> N.B. the sequence "*?" does not work properly at present in match
     * strings.
     *
     * @param filename the filename to match on
     * @param wildcardMatcher the wildcard string to match against
     * @return true if the filename matches the wilcard string
     * @see IOCase#SYSTEM
     */
    static boolean wildcardMatchOnSystem(String filename, String wildcardMatcher)
    {
        //return wildcardMatch(filename, wildcardMatcher, IOCase.SYSTEM);
        return wildcardMatch(filename, wildcardMatcher, IOCase.SENSITIVE);
    }

    /**
     * Checks a filename to see if it matches the specified wildcard matcher
     * allowing control over case-sensitivity. <p> The wildcard matcher uses the
     * characters '?' and '*' to represent a single or multiple (zero or more)
     * wildcard characters. N.B. the sequence "*?" does not work properly at
     * present in match strings.
     *
     * @param filename the filename to match on
     * @param wildcardMatcher the wildcard string to match against
     * @param caseSensitivity what case sensitivity rule to use, null means
     * case-sensitive
     * @return true if the filename matches the wilcard string
     * @since 1.3
     */
    static boolean wildcardMatch(String filename, String wildcardMatcher, IOCase caseSensitivity)
    {
        if (filename == null && wildcardMatcher == null)
        {
            return true;
        }
        if (filename == null || wildcardMatcher == null)
        {
            return false;
        }
        if (caseSensitivity == null)
        {
            caseSensitivity = IOCase.SENSITIVE;
        }
        String[] wcs = splitOnTokens(wildcardMatcher);
        boolean anyChars = false;
        int textIdx = 0;
        int wcsIdx = 0;
        Stack<int[]> backtrack = new Stack<int[]>();

        // loop around a backtrack stack, to handle complex * matching
        do
        {
            if (backtrack.size() > 0)
            {
                int[] array = backtrack.pop();
                wcsIdx = array[0];
                textIdx = array[1];
                anyChars = true;
            }

            // loop whilst tokens and text left to process
            while (wcsIdx < wcs.length)
            {

                if (wcs[wcsIdx].equals("?"))
                {
                    // ? so move to next text char
                    textIdx++;
                    if (textIdx > filename.length())
                    {
                        break;
                    }
                    anyChars = false;

                }
                else if (wcs[wcsIdx].equals("*"))
                {
                    // set any chars status
                    anyChars = true;
                    if (wcsIdx == wcs.length - 1)
                    {
                        textIdx = filename.length();
                    }

                }
                else
                {
                    // matching text token
                    if (anyChars)
                    {
                        // any chars then try to locate text token
                        textIdx = caseSensitivity.checkIndexOf(filename, textIdx, wcs[wcsIdx]);
                        if (textIdx == -1)
                        {
                            // token not found
                            break;
                        }
                        int repeat = caseSensitivity.checkIndexOf(filename, textIdx + 1, wcs[wcsIdx]);
                        if (repeat >= 0)
                        {
                            backtrack.push(new int[]
                                    {
                                        wcsIdx, repeat
                                    });
                        }
                    }
                    else
                    {
                        // matching from current position
                        if (!caseSensitivity.checkRegionMatches(filename, textIdx, wcs[wcsIdx]))
                        {
                            // couldnt match token
                            break;
                        }
                    }

                    // matched text token, move text index to end of matched token
                    textIdx += wcs[wcsIdx].length();
                    anyChars = false;
                }

                wcsIdx++;
            }

            // full match
            if (wcsIdx == wcs.length && textIdx == filename.length())
            {
                return true;
            }

        } while (backtrack.size() > 0);

        return false;
    }

    /**
     * Splits a string into a number of tokens. The text is split by '?' and
     * '*'. Where multiple '*' occur consecutively they are collapsed into a
     * single '*'.
     *
     * @param text the text to split
     * @return the array of tokens, never null
     */
    static String[] splitOnTokens(String text)
    {
        // used by wildcardMatch
        // package level so a unit test may run on this

        if (text.indexOf('?') == -1 && text.indexOf('*') == -1)
        {
            return new String[]
                    {
                        text
                    };
        }

        char[] array = text.toCharArray();
        ArrayList<String> list = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < array.length; i++)
        {
            if (array[i] == '?' || array[i] == '*')
            {
                if (buffer.length() != 0)
                {
                    list.add(buffer.toString());
                    buffer.setLength(0);
                }
                if (array[i] == '?')
                {
                    list.add("?");
                }
                else if (list.isEmpty()
                        || i > 0 && list.get(list.size() - 1).equals("*") == false)
                {
                    list.add("*");
                }
            }
            else
            {
                buffer.append(array[i]);
            }
        }
        if (buffer.length() != 0)
        {
            list.add(buffer.toString());
        }

        return list.toArray(new String[list.size()]);
    }

    final static class IOCase implements Serializable
    {

        /**
         * The constant for case sensitive regardless of operating system.
         */
        public static final IOCase SENSITIVE = new IOCase("Sensitive", true);
        /**
         * The constant for case insensitive regardless of operating system.
         */
        public static final IOCase INSENSITIVE = new IOCase("Insensitive", false);
        /**
         * The constant for case sensitivity determined by the current operating
         * system. Windows is case-insensitive when comparing filenames, Unix is
         * case-sensitive. <p> <strong>Note:</strong> This only caters for
         * Windows and Unix. Other operating systems (e.g. OSX and OpenVMS) are
         * treated as case sensitive if they use the Unix file separator and
         * case-insensitive if they use the Windows file separator (see {@link java.io.File#separatorChar}).
         * <p> If you derialize this constant of Windows, and deserialize on
         * Unix, or vice versa, then the value of the case-sensitivity flag will
         * change.
         */
        //public static final IOCase SYSTEM = new IOCase("System", !FilenameUtils.isSystemWindows());
        /**
         * Serialization version.
         */
        private static final long serialVersionUID = -6343169151696340687L;
        /**
         * The enumeration name.
         */
        private final String name;
        /**
         * The sensitivity flag.
         */
        private final transient boolean sensitive;

        //-----------------------------------------------------------------------
        /**
         * Factory method to create an IOCase from a name.
         *
         * @param name the name to find
         * @return the IOCase object
         * @throws IllegalArgumentException if the name is invalid
         */
        public static IOCase forName(String name)
        {
            if (IOCase.SENSITIVE.name.equals(name))
            {
                return IOCase.SENSITIVE;
            }
            if (IOCase.INSENSITIVE.name.equals(name))
            {
                return IOCase.INSENSITIVE;
            }
            //if (IOCase.SYSTEM.name.equals(name)){
            //    return IOCase.SYSTEM;
            //}
            throw new IllegalArgumentException("Invalid IOCase name: " + name);
        }

        //-----------------------------------------------------------------------
        /**
         * Private constructor.
         *
         * @param name the name
         * @param sensitive the sensitivity
         */
        private IOCase(String name, boolean sensitive)
        {
            this.name = name;
            this.sensitive = sensitive;
        }

        /**
         * Replaces the enumeration from the stream with a real one. This
         * ensures that the correct flag is set for SYSTEM.
         *
         * @return the resolved object
         */
        private Object readResolve()
        {
            return forName(name);
        }

        //-----------------------------------------------------------------------
        /**
         * Gets the name of the constant.
         *
         * @return the name of the constant
         */
        public String getName()
        {
            return name;
        }

        /**
         * Does the object represent case sensitive comparison.
         *
         * @return true if case sensitive
         */
        public boolean isCaseSensitive()
        {
            return sensitive;
        }

        //-----------------------------------------------------------------------
        /**
         * Compares two strings using the case-sensitivity rule. <p> This method
         * mimics {@link String#compareTo} but takes case-sensitivity into
         * account.
         *
         * @param str1 the first string to compare, not null
         * @param str2 the second string to compare, not null
         * @return true if equal using the case rules
         * @throws NullPointerException if either string is null
         */
        public int checkCompareTo(String str1, String str2)
        {
            if (str1 == null || str2 == null)
            {
                throw new NullPointerException("The strings must not be null");
            }
            return sensitive ? str1.compareTo(str2) : str1.compareToIgnoreCase(str2);
        }

        /**
         * Compares two strings using the case-sensitivity rule. <p> This method
         * mimics {@link String#equals} but takes case-sensitivity into account.
         *
         * @param str1 the first string to compare, not null
         * @param str2 the second string to compare, not null
         * @return true if equal using the case rules
         * @throws NullPointerException if either string is null
         */
        public boolean checkEquals(String str1, String str2)
        {
            if (str1 == null || str2 == null)
            {
                throw new NullPointerException("The strings must not be null");
            }
            return sensitive ? str1.equals(str2) : str1.equalsIgnoreCase(str2);
        }

        /**
         * Checks if one string starts with another using the case-sensitivity
         * rule. <p> This method mimics {@link String#startsWith(String)} but
         * takes case-sensitivity into account.
         *
         * @param str the string to check, not null
         * @param start the start to compare against, not null
         * @return true if equal using the case rules
         * @throws NullPointerException if either string is null
         */
        public boolean checkStartsWith(String str, String start)
        {
            return str.regionMatches(!sensitive, 0, start, 0, start.length());
        }

        /**
         * Checks if one string ends with another using the case-sensitivity
         * rule. <p> This method mimics {@link String#endsWith} but takes
         * case-sensitivity into account.
         *
         * @param str the string to check, not null
         * @param end the end to compare against, not null
         * @return true if equal using the case rules
         * @throws NullPointerException if either string is null
         */
        public boolean checkEndsWith(String str, String end)
        {
            int endLen = end.length();
            return str.regionMatches(!sensitive, str.length() - endLen, end, 0, endLen);
        }

        /**
         * Checks if one string contains another starting at a specific index
         * using the case-sensitivity rule. <p> This method mimics parts of {@link String#indexOf(String, int)}
         * but takes case-sensitivity into account.
         *
         * @param str the string to check, not null
         * @param strStartIndex the index to start at in str
         * @param search the start to search for, not null
         * @return the first index of the search String, -1 if no match or {@code null}
         * string input
         * @throws NullPointerException if either string is null
         * @since 2.0
         */
        public int checkIndexOf(String str, int strStartIndex, String search)
        {
            int endIndex = str.length() - search.length();
            if (endIndex >= strStartIndex)
            {
                for (int i = strStartIndex; i <= endIndex; i++)
                {
                    if (checkRegionMatches(str, i, search))
                    {
                        return i;
                    }
                }
            }
            return -1;
        }

        /**
         * Checks if one string contains another at a specific index using the
         * case-sensitivity rule. <p> This method mimics parts of {@link 
         * String#regionMatches(boolean, int, String, int, int)}
         * but takes case-sensitivity into account.
         *
         * @param str the string to check, not null
         * @param strStartIndex the index to start at in str
         * @param search the start to search for, not null
         * @return true if equal using the case rules
         * @throws NullPointerException if either string is null
         */
        public boolean checkRegionMatches(String str, int strStartIndex, String search)
        {
            return str.regionMatches(!sensitive, strStartIndex, search, 0, search.length());
        }

        //-----------------------------------------------------------------------
        /**
         * Gets a string describing the sensitivity.
         *
         * @return a string describing the sensitivity
         */
        @Override
        public String toString()
        {
            return name;
        }
    }
}
