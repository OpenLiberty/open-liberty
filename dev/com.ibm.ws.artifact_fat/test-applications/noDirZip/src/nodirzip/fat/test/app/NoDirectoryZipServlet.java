/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package nodirzip.fat.test.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/nodirzip")
public class NoDirectoryZipServlet extends FATServlet {

    @Test
    public void testNoDirectoryEntryZip(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        resp.getWriter().println("Running no directory test");
        String classFileName = getClass().getSimpleName() + ".class";
        URL testClassFile = getClass().getResource(classFileName);
        assertNotNull("Missing class resource.", testClassFile);
        assertEquals("Wrong protocol", "jar", testClassFile.getProtocol());
        String dirPath = '/' + getClass().getPackage().getName().replace('.', '/') + '/';

        // do this twice to make sure we only show the warning once per path
        getResourceParents(dirPath);
        getResourceParents(dirPath);

        // also calling CL.getResources to make sure no URLs get returned for that
        getResources(dirPath);

        // now call for something that really doesn't exist
        getResources(dirPath + "subDirMissing/");
    }

    private void getResourceParents(String dirPath) {
        do {
            URL testURL = getClass().getResource(dirPath);
            assertNull("Should not have found directory URL: " + testURL, testURL);
            int nextSlash = dirPath.lastIndexOf('/', dirPath.length() - 2);
            dirPath = dirPath.substring(0, nextSlash + 1);
        } while (dirPath.length() > 1);
    }

    private void getResources(String dirPath) throws IOException {
        List<URL> resources = Collections.list(getClass().getClassLoader().getResources(dirPath));
        assertTrue("Found the directory resources: ", resources.isEmpty());
    }

}
