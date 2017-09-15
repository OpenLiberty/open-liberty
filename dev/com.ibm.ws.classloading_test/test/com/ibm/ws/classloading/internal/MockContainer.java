/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

public class MockContainer implements Container {

// UNCOMMENT IF YOU NEED TO DEBUG THIS STUFF!
//    private static void print(Container c) throws UnableToAdaptException {
//        print(c, 2);
//    }
//
//    private static void print(Container c, int depth) throws UnableToAdaptException {
//        if (c == null)
//            return;
//        for (Entry e : c) {
//            System.out.printf("%" + depth + "s%s%n", "", e.getName());
//            print(e.adapt(Container.class), depth + 2);
//        }
//    }

    private final String name;
    /** Use a URLClassLoader to provide the lookup mechanics */
    private final URLClassLoader ucl;
    /** Container can be adapted to a butler, but it should always be the same one */
    private final ClassLoadingButlerImpl butler;
    /** The path of this container relative to its root */
    private final String path;
    /** Reference to the root container, possibly <code>this</code> */
    private final MockContainer root;

    private Set<String> getImmediateChildren() {
        URL url = ucl.getURLs()[0];
        return (url.toString().endsWith(".jar"))
                        ? getChildrenFromJar(url, path)
                        : getChildrenFromDir(url, path);
    }

    private static Set<String> getChildrenFromDir(URL dirUrl, String relPath) {
        try {
            Set<String> children = new TreeSet<String>();
            File f;
            f = new File(dirUrl.toURI());
            File parent = new File(f, relPath);
            if (!!!parent.isDirectory())
                throw new Error();
            for (File child : parent.listFiles()) {
                // add a trailing slash if this is a directory
                String suffix = child.isDirectory() ? "/" : "";
                children.add(relPath + child.getName() + suffix);
            }
            return children;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail("Could not create a file object for " + dirUrl);
            throw null; // unreachable code
        }
    }

    private static Set<String> getChildrenFromJar(URL jarUrl, String relPath) {
        ZipFile zf = null;
        try {
            Set<String> children = new TreeSet<String>();
            File f = new File(jarUrl.toURI());
            zf = new ZipFile(f);
            Enumeration<? extends ZipEntry> e = zf.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                // match
                String n = ensureLeadingSlash(ze.getName());
                // match the first word after the known path, including any slash after it
                Pattern p = Pattern.compile("(" + Pattern.quote(relPath) + "[^/]+/?)");
                Matcher m = p.matcher(n);
                if (m.find())
                    children.add(m.group(1));
            }
            return children;
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not read jar at " + jarUrl);
            throw null; // unreachable code
        } finally {
            try {
                zf.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String stripLeadingSlash(String s) {
        return s.replaceFirst("^/", "");
    }

    private static String stripTrailingSlash(String s) {
        return s.replaceFirst("/$", "");
    }

    private static String ensureLeadingSlash(String s) {
        return s.replaceFirst("^/?", "/");
    }

    /** create a root container for a single jar or directory URL */
    MockContainer(String name, URL url) {
        assertNotNull(name);
        assertNotNull(url);
        assertThat(url.toString(), startsWith("file:"));
        this.name = name;
        this.ucl = new URLClassLoader(new URL[] { url }, null);
        this.butler = new ClassLoadingButlerImpl(this);
        this.path = "/"; // by convention the root always has this path
        this.root = this;
    }

    /** create a subcontainer at a given path offset within a root container */
    private MockContainer(MockContainer root, String path) {
        this.name = root.name + "::" + path;
        this.ucl = root.ucl;
        this.butler = root.butler;
        this.path = ensureLeadingSlash(path);
        this.root = root;
    }

    @Override
    public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
        if (adaptTarget == ClassLoadingButler.class)
            return adaptTarget.cast(butler);
        return null;
    }

    @Override
    public Iterator<Entry> iterator() {
        List<Entry> entries = new ArrayList<Entry>();
        for (String child : getImmediateChildren()) {
            entries.add(new MockEntry(child));
        }
        return entries.iterator();
    }

    @Override
    public Entry getEntry(String path) {
        return ucl.findResource(stripLeadingSlash(path)) == null ? null : new MockEntry(path);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return isRoot() ? "/" : stripTrailingSlash(path);
    }

    @Override
    public Container getEnclosingContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRoot() {
        return this == root;
    }

    @Override
    public Container getRoot() {
        return root;
    }

    @Override
    public Collection<URL> getURLs() {
        return Collections.unmodifiableCollection(Arrays.asList(ucl.getURLs()));
    }

    @Override
    public String getPhysicalPath() {
        final URL rootUrl = ucl.getURLs()[0];
        final boolean rootIsADir = !!!rootUrl.toString().endsWith(".jar");
        if (isRoot() || rootIsADir) {
            try {
                return stripTrailingSlash(rootUrl.toURI().getPath()) + path;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return ucl.getURLs()[0].toString() + "::" + path;
    }

    class MockEntry implements Entry {

        final String path;

        MockEntry(String path) {
            this.path = ensureLeadingSlash(path);
        }

        @Override
        public <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException {
            // if it is a dir, the path ends with a slash
            if (adaptTarget == Container.class && path.endsWith("/"))
                return adaptTarget.cast(new MockContainer(root, path));
            if (adaptTarget == InputStream.class && !!!path.endsWith("/")) {
                try {
                    return adaptTarget.cast(ucl.findResource(stripLeadingSlash(path)).openStream());
                } catch (IOException e) {
                    throw new UnableToAdaptException(e);
                }
            }
            return null;
        }

        @Override
        public String getName() {
            return root.name + "::" + path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public long getSize() {
            InputStream rsrc = ucl.getResourceAsStream(stripLeadingSlash(path));
            try {
                return rsrc == null ? 0 : rsrc.available();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            } finally {
                try {
                    rsrc.close();
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public long getLastModified() {
            throw new UnsupportedOperationException();
        }

        @Override
        public URL getResource() {
            return ucl.findResource(stripLeadingSlash(path));
        }

        @Override
        public String getPhysicalPath() {
            final URL rootUrl = ucl.getURLs()[0];
            final boolean rootIsADir = !!!rootUrl.toString().endsWith(".jar");
            if (rootIsADir)
                try {
                    return stripTrailingSlash(rootUrl.toURI().getPath()) + path;
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            return null;
        }

        @Override
        public Container getEnclosingContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Container getRoot() {
            return root;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
