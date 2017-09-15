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
package org.apache.myfaces.view.facelets;

import java.io.Writer;
import java.util.Comparator;

/**
 *
 * @author lu4242
 */
public class FaceletsVDLUtils
{
        /**
     * As specified in JSF 2.2 section 11.4.2.1, note it is different from the
     * allowed format in xml url-pattern type.
     * 
     * @param path
     * @param pattern
     * @return A
     */
    public static boolean matchPattern(String path, String pattern)
    {
        // Normalize the argument strings
        if ((path == null) || (path.length() == 0))
        {
            path = "*";
        }
        if ((pattern == null) || (pattern.length() == 0))
        {
            pattern = "*";
        }

        // Check for exact match
        if (path.equals(pattern))
        {
            return (true);
        }

        // Check for path prefix matching
        if (pattern.startsWith("/") && pattern.endsWith("/*"))
        {
            pattern = pattern.substring(0, pattern.length() - 2);
            if (pattern.length() == 0)
            {
                return (true);  // "/*" is the same as "/"
            }
            if (path.endsWith("/"))
            {
                path = path.substring(0, path.length() - 1);
            }
            while (true)
            {
                if (pattern.equals(path))
                {
                    return (true);
                }
                int slash = path.lastIndexOf('/');
                if (slash <= 0)
                {
                    break;
                }
                path = path.substring(0, slash);
            }
            return (false);
        }

        // Check for universal mapping
        if (pattern.equals("*"))
        {
            return (true);
        }

        return (false);
    }
    
    public static final class KeyComparator implements Comparator<String>
    {
        public int compare(String s1, String s2)
        {
            return -s1.compareTo(s2);
        }
    }
    
    public static class NullWriter extends Writer
    {

        static final NullWriter INSTANCE = new NullWriter();

        public void write(char[] buffer)
        {
        }

        public void write(char[] buffer, int off, int len)
        {
        }

        public void write(String str)
        {
        }

        public void write(int c)
        {
        }

        public void write(String str, int off, int len)
        {
        }

        public void close()
        {
        }

        public void flush()
        {
        }
    }
}
