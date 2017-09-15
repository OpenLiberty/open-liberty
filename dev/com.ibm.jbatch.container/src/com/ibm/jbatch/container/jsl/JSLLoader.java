/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.jsl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class JSLLoader {

    private final static Logger logger = Logger.getLogger(JSLLoader.class.getName());

    // Holds a list of job.xml files
    private Set<URI> jobFilelist = Collections.synchronizedSet(new LinkedHashSet<URI>());

    public static final String JOBS_FOLDER = "META-INF/jobs";

    public void traverseJobPath() {

    }

    /**
     * 
     * @param rootURL
     *            the jar file associated with this batch application
     * @return list of batch artifact files
     * @throws IOException
     * @throws FileNotFoundException
     * @throws URISyntaxException
     * @throws URISyntaxException
     */
    public Set<URI> getArtifacts(final URL rootURL) throws FileNotFoundException, IOException, URISyntaxException {

        JarFile jarfile = new JarFile(new File(rootURL.toURI()));

        Enumeration<JarEntry> jarEntries = jarfile.entries();
        if (jarEntries == null) {
            throw new IllegalArgumentException();
        }

        while (jarEntries.hasMoreElements()) {
            String entry = jarEntries.nextElement().getName();
            if (entry.startsWith(JOBS_FOLDER) && !(entry.endsWith("/"))) {
                jobFilelist.add(new URI(null, entry, null));

            }
        }
        return jobFilelist;
    }

    private Set<URI> getFolderArtifacts(File directory) throws FileNotFoundException, IOException, URISyntaxException {

        //Get all the xml files in the directory
        File[] xmlFiles = directory.listFiles(new XMLFilenameFilter());

        for (File file : xmlFiles) {
            jobFilelist.add(file.toURI());
        }

        return jobFilelist;
    }



    private class XMLFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String fileName) {

            if (fileName.endsWith(".xml")) {
                return true;
            }
            return false;
        }

    }
}
