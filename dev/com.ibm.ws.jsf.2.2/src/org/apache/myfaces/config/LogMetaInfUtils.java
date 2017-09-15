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

package org.apache.myfaces.config;

import org.apache.myfaces.shared.util.ClassUtils;

import javax.faces.FacesException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LogMetaInfUtils
{

    private static final Logger log = Logger.getLogger(LogMetaInfUtils.class.getName());

    /**
     * Regular expression used to extract the jar information from the
     * files present in the classpath.
     * <p>The groups found with the regular expression are:</p>
     * <ul>
     *   <li>Group 2: file path</li>
     *   <li>Group 3: artifact id</li>
     *   <li>Group 4: version</li>
     * </ul>
     * The regexp is searching in the file name to the first - followed by a digit to split artifact name and version.
     */
    public static final String REGEX_LIBRARY
        = "(jar|besjar|wsjar|zip)"
        + ":(file:.*/((myfaces|tomahawk|trinidad|tobago|commons\\-el|jsp\\-api)[\\w\\-\\_]+?)"
        + "-(\\d+.*)\\.jar)!/META-INF/MANIFEST.MF";
    private static final Pattern REGEX_LIBRARY_PATTERN = Pattern.compile(REGEX_LIBRARY);

    private static final int REGEX_LIBRARY_FILE_PATH = 2;
    private static final int REGEX_LIBRARY_ARTIFACT_ID = 3;
    private static final int REGEX_LIBRARY_VERSION = 5;

    /**
     * This method performs part of the factory search outlined in section 10.2.6.1.
     */
    @SuppressWarnings("unchecked")
    protected static void logMetaInf()
    {
        if (!log.isLoggable(Level.WARNING))
        {
          return;
        }

        try
        {
            Map<String, List<JarInfo>> libs = new HashMap<String, List<JarInfo>>(30);

            Iterator<URL> it = ClassUtils.getResources("META-INF/MANIFEST.MF", LogMetaInfUtils.class);
            while (it.hasNext())
            {
                URL url = it.next();
                addJarInfo(libs, url);
            }

            final List<String> keys = new ArrayList(libs.keySet());
            Collections.sort(keys);

            if (log.isLoggable(Level.WARNING))
            {
                for (String artifactId : keys)
                {
                    List<JarInfo> versions = libs.get(artifactId);
                    if (versions != null && versions.size() > 1)
                    {
                        StringBuilder builder = new StringBuilder(1024);
                        builder.append("You are using the library: ");
                        builder.append(artifactId);
                        builder.append(" in different versions; first (and probably used) version is: ");
                        builder.append(versions.get(0).getVersion());
                        builder.append(" loaded from: ");
                        builder.append(versions.get(0).getUrl());
                        builder.append(", but also found the following versions: ");

                        boolean needComma = false;
                        for (int i = 1; i < versions.size(); i++)
                        {
                            JarInfo info = versions.get(i);
                            if (needComma)
                            {
                                builder.append(", ");
                            }

                            builder.append(info.getVersion());
                            builder.append(" loaded from: ");
                            builder.append(info.getUrl());

                            needComma = true;
                        }

                        log.warning(builder.toString());
                    }
                }
            }

            if (log.isLoggable(Level.INFO))
            {
                for (String artifactId : keys)
                {
                    logArtifact(artifactId, libs);
                }
            }
        }
        catch (Throwable e)
        {
            throw new FacesException(e);
        }
    }

    protected static void addJarInfo(Map<String, List<JarInfo>> libs, URL url)
    {
        Matcher matcher = REGEX_LIBRARY_PATTERN.matcher(url.toString());
        if (matcher.matches())
        {
            // We have a valid JAR
            String artifactId = matcher.group(REGEX_LIBRARY_ARTIFACT_ID);
            List<JarInfo> versions = libs.get(artifactId);
            if (versions == null)
            {
                versions = new ArrayList<JarInfo>(1);
                libs.put(artifactId, versions);
            }

            String path = matcher.group(REGEX_LIBRARY_FILE_PATH);

            String version = matcher.group(REGEX_LIBRARY_VERSION);

            JarInfo newInfo = new JarInfo(path, version);
            if (!versions.contains(newInfo))
            {
                versions.add(newInfo);
            }
        }
    }

    private static void logArtifact(String artifactId, Map<String, List<JarInfo>> libs)
    {
        List<JarInfo> versions = libs.get(artifactId);
        if (versions == null)
        {
            log.info("Artifact '" + artifactId + "' was not found.");
        }
        else
        {
            JarInfo info = versions.get(0);
            log.info("Artifact '" + artifactId + "' was found in version '"
                     + info.getVersion() + "' from path '" + info.getUrl() + "'");
        }
    }


    protected static class JarInfo implements Comparable<JarInfo>
    {
        private String url;
        private String version;

        public JarInfo(String url, String version)
        {
            this.url = url;
            this.version = version;
        }

        public String getVersion()
        {
            return version;
        }

        public String getUrl()
        {
            return url;
        }

        public int compareTo(JarInfo info)
        {
            return version.compareTo(info.version);
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this)
            {
                return true;
            }
            else if (o instanceof JarInfo)
            {
                JarInfo other = (JarInfo) o;
                return version.equals(other.version);
            }
            else
            {
                return false;
            }
        }

        @Override
        public int hashCode()
        {
            return version.hashCode();
        }
    }

}
