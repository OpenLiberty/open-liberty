/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package web;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.library.Library;

@SuppressWarnings("serial")
public class SharedLibraryServlet extends HttpServlet {

    public static final String ERROR_MESSAGE = "Error: ";
    public static final String SUCCESS_MESSAGE = "Success";
    public static final String TEST_STRING = "String that should be passed from app to app";
    private static final int MAX_SECONDS_WAIT = 60;
    private static BundleContext ctx = null;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("GET: " + request.getRequestURI());
        PrintWriter writer = response.getWriter();
        String testName = request.getParameter("testName");
        String result = ERROR_MESSAGE;

        if (ctx == null) {
            Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);
            ctx = bundle.getBundleContext();
        }

        Collection<ServiceReference<Library>> references = new ArrayList<ServiceReference<Library>>();
        try {
            if ("ping".equals(testName)) {
                result = "ACK";
            } else if ("testOneFileset".equals(testName)) {
                result = testOneFileset(references);
            } else if ("testOneFilesetRef".equals(testName)) {
                result = testOneFilesetRef(references);
            } else if ("testThreeFilesets".equals(testName)) {
                result = testThreeFilesets(references);
            } else if ("testThreeFilesetsRef".equals(testName)) {
                result = testThreeFilesetsRef(references);
            } else if ("testFilesetReference".equals(testName)) {
                result = testFilesetReference(references);
            } else if ("testFilesetChange".equals(testName)) {
                result = testFilesetChange(references);
            } else if ("testFilesetChangeRef".equals(testName)) {
                result = testFilesetChangeRef(references);
            } else if ("testConfigChange".equals(testName)) {
                result = testConfigChange(references);
            } else if ("testConfigChangeRef".equals(testName)) {
                result = testConfigChangeRef(references);
            } else if ("testConfigChangeAfter".equals(testName)) {
                result = testConfigChangeAfter(references);
            } else if ("testConfigChangeAfterRef".equals(testName)) {
                result = testConfigChangeAfterRef(references);
            } else if ("testAppClassloader".equals(testName)) {
                result = testAppClassloader(references);
            } else if ("testClassloaderSimple".equals(testName)) {
                result = testClassloaderSimple(references);
            } else if ("testClassloaderDirectory".equals(testName)) {
                result = testClassloaderDirectory(references);
            } else if ("testClassloaderNoScan".equals(testName)) {
                result = testClassloaderNoScan(references);
            } else if ("testClassloaderEar".equals(testName)) {
                result = testClassloaderEar(references);
            } else if ("testCommonLibraryLoadClass".equals(testName)) {
                String className = request.getParameter("className");
                result = testCommonLibraryLoadClass(className);
            } else if ("testCommonLibraryReadResource".equals(testName)) {
                String resourceName = request.getParameter("resourceName");
                result = testCommonLibraryReadResource(resourceName);
            } else if ("testCommonLibraryReadResources".equals(testName)) {
                String resourceName = request.getParameter("resourceName");
                result = testCommonLibraryReadResources(resourceName);
            } else if ("testResourceDiscovery".equals(testName)) {
                result = testResourceDiscovery(references);
            } else if ("testCommonLibraryReadDirectoryResource".equals(testName)) {
                String resourceName = request.getParameter("resourceName");
                result = testCommonLibraryReadDirectoryResource(resourceName);
            } else if ("testCommonLibraryReadDirectoryResources".equals(testName)) {
                String resourceName = request.getParameter("resourceName");
                result = testCommonLibraryReadDirectoryResources(resourceName);
            } else if ("testGlobalLibrary".equals(testName)) {
                result = testGlobalLibrary();
            } else if ("testGlobalLibraryPart2".equals(testName)) {
                result = testGlobalLibraryPart2();
            } else if ("testGlobalLibraryFolder".equals(testName)) {
                result = testGlobalFolderLibrary();
            } else if ("testGlobalLibraryFolderPart2".equals(testName)) {
                result = testGlobalFolderLibraryPart2();
            } else if ("compareClassLoaders".equals(testName)) {
                result = compareClassLoaders();
            } else if ("testFiles".equals(testName)) {
                result = testFiles(references);
            } else if ("testFolders".equals(testName)) {
                result = testFolders(references);
            }

        } catch (Throwable e) {
            result += e.toString();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            result += baos.toString();
        } finally {
            writer.println(result);
            writer.flush();
            writer.close();
        }
    }

    private String testFilesetReference(Collection<ServiceReference<Library>> references) throws Exception {
        String result = ERROR_MESSAGE;
        Library lib = getLibraryCommon("testFilesetReference", references);
        if (lib != null) {
            ArrayList<File> files = getFiles(lib);
            if (files != null && files.size() == 1) {
                File file = files.get(0);
                String s = file.toString();
                if (s.contains("test1.jar")) {
                    result = SUCCESS_MESSAGE;
                }
            } else {
                result = "No Filesets found" + files;
            }
        }
        return result;
    }

    private String testOneFileset(Collection<ServiceReference<Library>> references) throws Exception {
        return testOneFilesetCommon(references, "testOneFileset");
    }

    private String testOneFilesetRef(Collection<ServiceReference<Library>> references) throws Exception {
        return testOneFilesetCommon(references, "testOneFilesetRef");
    }

    private String testOneFilesetCommon(Collection<ServiceReference<Library>> references, String library) throws InterruptedException {
        String result = ERROR_MESSAGE;
        Library lib = getLibraryCommon(library, references);
        if (lib == null)
            return "Could not find library " + library;
        int count = 0;
        while (null == findFile(lib, "test2.jar") && count++ < MAX_SECONDS_WAIT) {
            Thread.sleep(1000);
        }
        if (findFile(lib, "test2.jar") != null) {
            result = SUCCESS_MESSAGE;
        } else {
            result = "File test2.jar not found in Shared library " + library;
        }
        return result;
    }

    private String testThreeFilesets(Collection<ServiceReference<Library>> references) throws Exception {
        return testThreeFilesetsCommon(references, "testThreeFilesets");
    }

    private String testThreeFilesetsRef(Collection<ServiceReference<Library>> references) throws Exception {
        return testThreeFilesetsCommon(references, "testThreeFilesetsRef");
    }

    private String testThreeFilesetsCommon(Collection<ServiceReference<Library>> references, String library) {
        String result = ERROR_MESSAGE;
        Library lib = this.getLibraryCommon(library, references);
        if (lib != null) {
            ArrayList<File> files = getFiles(lib);
            if (files != null && files.size() == 3) {
                boolean found3a = false, found3b = false, found3c = false;
                for (File file : files) {
                    String s = file.toString();
                    if (s.contains("test3a.jar")) found3a = true;
                    else if (s.contains("test3b.jar")) found3b = true;
                    else if (s.contains("test3c.jar")) found3c = true;
                }
                if (found3a && found3b && found3c && files.size() == 3) {
                    result = SUCCESS_MESSAGE;
                } else {
                    result = "Three filesets were expected but not found";
                }
            } else {
                result = (files == null) ? "No Filesets found" : "wrong number of files found. Found: " + files.size();
            }
        } else {
            result = "Could not find shared library " + library;
        }
        return result;
    }

    private String testFilesetChange(Collection<ServiceReference<Library>> references) throws Exception {
        return testFilesetChangeCommon("testFilesetChange", references);
    }

    private String testFilesetChangeRef(Collection<ServiceReference<Library>> references) throws Exception {
        return testFilesetChangeCommon("testFilesetChangeRef", references);
    }

    private String testFilesetChangeCommon(String library, Collection<ServiceReference<Library>> references) throws Exception {
        Library lib = getLibraryCommon(library, references);
        if (lib == null)
            return "Could not find library " + library;
        int count = 0;
        while (null == findFile(lib, "test4a.jar") && count++ < MAX_SECONDS_WAIT) Thread.sleep(1000);
        if (findFile(lib, "test4a.jar") == null)
            return "File test4a.jar never arrived in Shared library " + library;

        File file = findFile(lib, "test4a.jar");
        String dir = file.getAbsolutePath().substring(0, file.getAbsolutePath().indexOf("test4a.jar"));
        File newJar = new File(dir + "test4b.jar");
        BufferedWriter out = new BufferedWriter(new FileWriter(newJar));
        out.write("Hello");
        out.close();

        count = 0;
        while (null == findFile(lib, "test4b.jar") && count++ < MAX_SECONDS_WAIT) Thread.sleep(1000);
        if (findFile(lib, "test4b.jar") == null)
            return "File test4b.jar never arrived in Shared library " + library;

        final File finalNewJar = newJar;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override public Object run() { finalNewJar.delete(); return null; }
        });

        count = 0;
        while (null != findFile(lib, "test4b.jar") && count++ < MAX_SECONDS_WAIT) Thread.sleep(1000);
        if (findFile(lib, "test4b.jar") != null)
            return "File test4b.jar never vanished from Shared library " + library;

        return SUCCESS_MESSAGE;
    }

    private File findFile(Library lib, String filename) {
        ArrayList<File> files = getFiles(lib);
        if (files != null) {
            for (File f : files) {
                if (f.toString().contains(filename)) return f;
            }
        }
        return null;
    }

    private Library getLibraryCommon(String id, Collection<ServiceReference<Library>> references) {
        Library result = null;
        String filter = "(" + "id=" + id + ")";
        references = null;
        try {
            references = ctx.getServiceReferences(Library.class, filter);
        } catch (InvalidSyntaxException e) {
            return null;
        }
        if (references == null) return null;
        for (ServiceReference<Library> ref : references) {
            result = ctx.getService(ref);
        }
        return result;
    }

    private ArrayList<File> getFiles(Library lib) {
        ArrayList<File> result = new ArrayList<File>();
        Collection<Fileset> filesets = lib.getFilesets();
        if (filesets == null || filesets.isEmpty()) return null;
        for (Fileset fileset : filesets) {
            for (File file : fileset.getFileset()) result.add(file);
        }
        return result;
    }

    private String testConfigChange(Collection<ServiceReference<Library>> references) throws Exception {
        return testConfigChangeCommon("testConfigChange", references);
    }

    private String testConfigChangeRef(Collection<ServiceReference<Library>> references) throws Exception {
        return testConfigChangeCommon("testConfigChangeRef", references);
    }

    private String testConfigChangeCommon(String library, Collection<ServiceReference<Library>> references) throws Exception {
        Library lib = getLibraryCommon(library, references);
        if (lib == null) return "Could not find library " + library;
        int count = 0;
        while (null == findFile(lib, "test6.jar") && count++ < MAX_SECONDS_WAIT) Thread.sleep(1000);
        if (findFile(lib, "test6.jar") != null) return SUCCESS_MESSAGE;
        return "File test6.jar not found in Shared library " + library;
    }

    private String testConfigChangeAfter(Collection<ServiceReference<Library>> references) throws Exception {
        return testConfigChangeAfterCommon("testConfigChange", references);
    }

    private String testConfigChangeAfterRef(Collection<ServiceReference<Library>> references) throws Exception {
        return testConfigChangeAfterCommon("testConfigChangeRef", references);
    }

    private String testConfigChangeAfterCommon(String library, Collection<ServiceReference<Library>> references) throws Exception {
        Library lib = getLibraryCommon(library, references);
        if (lib == null) return "Could not find library " + library;
        int count = 0;
        while (null == findFile(lib, "test7.jar") && count++ < MAX_SECONDS_WAIT) Thread.sleep(1000);
        if (findFile(lib, "test7.jar") != null) return SUCCESS_MESSAGE;
        return "File test7.jar not found in Shared library " + library;
    }

    private String testAppClassloader(Collection<ServiceReference<Library>> references) throws Exception {
        String result = SUCCESS_MESSAGE;
        try {
            result = callClass("test.HelloA");
            if (!SUCCESS_MESSAGE.equals(result)) return result;
            result = callClass("test.HelloB");
            if (!SUCCESS_MESSAGE.equals(result)) return result;
            result = callClass("test.HelloC");
            if (!SUCCESS_MESSAGE.equals(result)) return result;
            try {
                result = callClass("library.test.DoesNotExist");
                return ERROR_MESSAGE;
            } catch (ClassNotFoundException x) {
                result = SUCCESS_MESSAGE;
            }
        } catch (Exception x) {
            result = x.toString();
        }
        return result;
    }

    private String testResourceDiscovery(Collection<ServiceReference<Library>> references) throws Exception {
        String result = SUCCESS_MESSAGE;
        try {
            String commonDirectory = "webResources";
            String commonFile = "webResource";
            String commonPropFile = "file.properties";
            String dirInClasses = "DirInClasses";
            String fileInClasses = "FileInClasses";
            String fileInJar = "FileInJar";
            String dirInJar = "DirInJar";
            String nestedDir = "webResources/test";
            String nestedFile = "webResources/file.properties";

            URL location = locateResource(dirInClasses);
            if (location != null && location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + " location was null or did end with / " + location;
            }
            location = locateResource(dirInClasses + "/");
            if (location == null || !location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + " location/ was null or did not end with / " + location;
            }
            InputStream stream = this.getClass().getClassLoader().getResourceAsStream(dirInJar);
            if (stream == null) return ERROR_MESSAGE + " got null stream for dirInJar";
            location = locateResource(dirInJar);
            if (location != null && location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + "  dirInJar was null, or did not end with / " + location;
            }
            location = locateResource(dirInJar + "/");
            if (location == null || !location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + " dirInJar/ was null or did not end with / " + location;
            }
            location = locateResource(fileInJar);
            if (location == null || location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + " fileInJar was null or ended with / " + location;
            }
            location = locateResource(fileInJar + "/");
            if (location != null) return ERROR_MESSAGE + " fileInJar/ should be null " + location;
            location = locateResource(fileInClasses);
            if (location == null || location.toExternalForm().endsWith("/")) {
                return ERROR_MESSAGE + " fileInClasses was null or ended with / " + location;
            }
            location = locateResource(fileInClasses + "/");
            if (location != null) return ERROR_MESSAGE + "fileInClasses/ should be null " + location;

            Enumeration<URL> locations = locateResources(commonDirectory);
            int size = 0;
            while (locations.hasMoreElements()) {
                location = locations.nextElement();
                if (location.toExternalForm().endsWith("/"))
                    return ERROR_MESSAGE + "location " + size + " ended with / " + location.toExternalForm();
                size++;
            }
            if (size != 2) return ERROR_MESSAGE + " expected 2 items for commonDir, got " + size;

            locations = locateResources(commonFile);
            size = 0;
            while (locations.hasMoreElements()) {
                location = locations.nextElement();
                if (location.toExternalForm().endsWith("/"))
                    return ERROR_MESSAGE + " location " + size + " for commonFile ended in / " + location.toExternalForm();
                size++;
            }
            if (size != 2) return ERROR_MESSAGE + " expected 2 for commonFile, had " + size;

            locations = locateResources(commonPropFile);
            size = 0;
            while (locations.hasMoreElements()) { locations.nextElement(); size++; }
            if (size != 2) return ERROR_MESSAGE + " enum for commonPropFile expected 2, got " + size;

            locations = locateResources(nestedDir);
            size = 0;
            while (locations.hasMoreElements()) {
                location = locations.nextElement();
                if (location.toExternalForm().endsWith("/"))
                    return ERROR_MESSAGE + " location for nestedDir ended in / " + location.toExternalForm();
                size++;
            }
            if (size != 2) return ERROR_MESSAGE + " expected 2 for nestedDir, got " + size;

            locations = locateResources(nestedFile);
            size = 0;
            while (locations.hasMoreElements()) { locations.nextElement(); size++; }
            if (size != 2) return ERROR_MESSAGE + " expected 2 for nestedFile, got " + size;

        } catch (Exception x) {
            result = x.toString();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            x.printStackTrace(ps);
            result += baos.toString();
        }
        return result;
    }

    private String callClass(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        String result = ERROR_MESSAGE;
        Class<?> c = Class.forName(className);
        Object t = c.newInstance();
        Method m = c.getMethod("areYouThere", new Class<?>[] { String.class });
        m.setAccessible(true);
        Object o = m.invoke(t, new Object[] { "Test" });
        if (o instanceof String) {
            String s = (String) o;
            if (s.equals(className + "Test")) result = SUCCESS_MESSAGE;
            else result = s;
        }
        return result;
    }

    private URL locateResource(String resourceName) {
        return this.getClass().getClassLoader().getResource(resourceName);
    }

    private Enumeration<URL> locateResources(String resourceName) throws IOException {
        return this.getClass().getClassLoader().getResources(resourceName);
    }

    private String testClassloaderSimple(Collection<ServiceReference<Library>> references) throws Exception {
        try { return callClass("test.HelloA"); } catch (Exception x) { return x.toString(); }
    }

    private String testClassloaderDirectory(Collection<ServiceReference<Library>> references) throws Exception {
        try { return callClass("test.HelloA"); } catch (Exception x) { return x.toString(); }
    }

    private String testClassloaderNoScan(Collection<ServiceReference<Library>> references) throws Exception {
        try { return callClass("test.HelloA"); } catch (Exception x) { return x.toString(); }
    }

    private String testClassloaderEar(Collection<ServiceReference<Library>> references) throws Exception {
        try { return callClass("test.HelloC"); } catch (Exception x) { return x.toString(); }
    }

    private String testCommonLibraryLoadClass(String className) throws Exception {
        Class<?> c = Class.forName(className);
        return c.toString() + "@" + Integer.toHexString(c.hashCode());
    }

    private String testCommonLibraryReadResources(String resourceName) throws IOException {
        StringBuffer retMe = new StringBuffer("");
        Enumeration<URL> urls = this.getClass().getClassLoader().getResources(resourceName);
        while (urls.hasMoreElements()) retMe.append(readURL(urls.nextElement()) + ";");
        return retMe.toString();
    }

    private String testCommonLibraryReadResource(String resourceName) throws IOException {
        return readURL(this.getClass().getClassLoader().getResource(resourceName));
    }

    private String testCommonLibraryReadDirectoryResources(String resourceName) throws IOException {
        StringBuffer retMe = new StringBuffer("");
        Enumeration<URL> urls = this.getClass().getClassLoader().getResources(resourceName);
        while (urls.hasMoreElements()) {
            if (!retMe.toString().equals("")) retMe.append("***");
            retMe.append(urls.nextElement());
        }
        return retMe.toString();
    }

    private String testCommonLibraryReadDirectoryResource(String resourceName) throws IOException {
        return this.getClass().getClassLoader().getResource(resourceName).toExternalForm();
    }

    private String readURL(URL url) throws IOException {
        StringBuffer retMe = new StringBuffer("");
        if (url != null) {
            InputStream is = url.openStream();
            while (is.available() != 0) {
                byte[] b = new byte[is.available()];
                is.read(b);
                retMe.append(new String(b));
            }
        }
        return retMe.toString();
    }

    private String testGlobalLibrary() throws Exception {
        try {
            Class<?> c = Class.forName("test.HelloD");
            Object t = c.newInstance();
            Method m = c.getMethod("setstaticValue", new Class<?>[] { String.class });
            m.setAccessible(true);
            m.invoke(t, new Object[] { TEST_STRING });
            return SUCCESS_MESSAGE;
        } catch (Exception x) { return x.toString(); }
    }

    private String testGlobalLibraryPart2() throws Exception {
        try {
            Class<?> c = Class.forName("test.HelloD");
            Object t = c.newInstance();
            Method m = c.getMethod("getstaticValue", new Class<?>[] {});
            m.setAccessible(true);
            Object o = m.invoke(t);
            if (o instanceof String && ((String) o).equals(TEST_STRING)) return SUCCESS_MESSAGE;
            return (String) o;
        } catch (Exception x) { return x.toString(); }
    }

    private String testGlobalFolderLibrary() throws Exception {
        try {
            Class<?> c = Class.forName("test.HelloE");
            Object t = c.newInstance();
            Method m = c.getMethod("setstaticValue", new Class<?>[] { String.class });
            m.setAccessible(true);
            m.invoke(t, new Object[] { TEST_STRING });
            return SUCCESS_MESSAGE;
        } catch (Exception x) { return x.toString(); }
    }

    private String testGlobalFolderLibraryPart2() throws Exception {
        try {
            Class<?> c = Class.forName("test.HelloE");
            Object t = c.newInstance();
            Method m = c.getMethod("getstaticValue", new Class<?>[] {});
            m.setAccessible(true);
            Object o = m.invoke(t);
            if (o instanceof String && ((String) o).equals(TEST_STRING)) return SUCCESS_MESSAGE;
            return (String) o;
        } catch (Exception x) { return x.toString(); }
    }

    private String compareClassLoaders() throws Exception {
        try {
            Class<?> c = Class.forName("test.HelloD");
            Object t = c.newInstance();
            Method m = c.getMethod("getMyClassLoader", new Class<?>[] {});
            m.setAccessible(true);
            ClassLoader globalClassLoader = (ClassLoader) m.invoke(t);
            ClassLoader myClassLoader = this.getClass().getClassLoader();
            while (myClassLoader != globalClassLoader) {
                myClassLoader = myClassLoader.getParent();
                if (myClassLoader == null) return "Traversed up classloader tree and got no match";
            }
            return SUCCESS_MESSAGE;
        } catch (Exception x) { return x.toString(); }
    }

    private String testFiles(Collection<ServiceReference<Library>> references) {
        String library = "SharedLibrary";
        Library lib = getLibraryCommon(library, references);
        if (lib == null) return "Could not find library " + library;
        Collection<File> allFiles = lib.getFiles();
        if (allFiles == null || allFiles.isEmpty()) return "Files were completely missing";
        if (allFiles.size() != 2) return "Wrong number of files found. Found " + allFiles.size() + " expecting 2";
        for (File f : allFiles) {
            if (!f.exists()) return "file does not exist " + f;
        }
        return SUCCESS_MESSAGE;
    }

    private String testFolders(Collection<ServiceReference<Library>> references) {
        String library = "SharedLibrary";
        Library lib = getLibraryCommon(library, references);
        if (lib == null) return "Could not find library " + library;
        Collection<File> allFolders = lib.getFolders();
        if (allFolders == null || allFolders.isEmpty()) return "Folders were completely missing";
        if (allFolders.size() != 2) return "Wrong number of folders found. Found " + allFolders.size() + " expecting 2";
        for (File f : allFolders) {
            if (!f.isDirectory()) return f + " is not a folder";
        }
        return SUCCESS_MESSAGE;
    }
}
