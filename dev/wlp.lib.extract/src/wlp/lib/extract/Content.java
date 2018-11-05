/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 */
public abstract class Content implements Iterable<Content.Entry> {

    public abstract Manifest getManifest() throws IOException;

    public abstract Entry getEntry(String path);

    public abstract String getName();

    protected abstract int size();

    public void close() throws IOException {
        // do nothing by default
    }

    public abstract boolean isExtracted();

    public interface Entry {
        public String getName();

        public boolean isDirectory();

        public InputStream getInputStream() throws IOException;
    }

    public static Content build(File f) throws IOException {
        if (f.isDirectory()) {
            return new DirectoryContent(f);
        }
        return new JarContent(new JarFile(f));
    }

    public static class JarContent extends Content {
        private final JarFile jar;
        private final List<Entry> entries = new ArrayList<Content.Entry>();

        public JarContent(JarFile jar) {
            this.jar = jar;
            for (Enumeration<JarEntry> eJarEntries = this.jar.entries(); eJarEntries.hasMoreElements();) {
                JarEntry jarEntry = eJarEntries.nextElement();
                entries.add(new JarContentEntry(jarEntry));
            }
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

        @Override
        public Manifest getManifest() throws IOException {
            return jar.getManifest();
        }

        @Override
        public Entry getEntry(String path) {
            JarEntry jarEntry = jar.getJarEntry(path);
            if (jarEntry != null) {
                return new JarContentEntry(jarEntry);
            }
            return null;
        }

        @Override
        public String getName() {
            return jar.getName();
        }

        @Override
        protected int size() {
            return jar.size();
        }

        @Override
        public void close() throws IOException {
            jar.close();
        }

        @Override
        public boolean isExtracted() {
            return false;
        }

        public class JarContentEntry implements Entry {
            private final JarEntry jarEntry;

            public JarContentEntry(JarEntry jarEntry) {
                this.jarEntry = jarEntry;
            }

            @Override
            public String getName() {
                return jarEntry.getName();
            }

            @Override
            public boolean isDirectory() {
                return jarEntry.isDirectory();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return jar.getInputStream(jarEntry);
            }

        }
    }

    public static class DirectoryContent extends Content {
        private final File root;
        private final List<Entry> entries = new ArrayList<Content.Entry>();

        public DirectoryContent(File root) {
            this.root = root;
            addEntries(root, "", this.entries);

        }

        private void addEntries(File file, String pathPrefix, List<Entry> results) {
            for (String path : file.list()) {
                File child = new File(file, path);
                String childPath = pathPrefix.isEmpty() ? path : pathPrefix + '/' + path;
                results.add(new FileEntry(child, childPath));
                if (child.isDirectory()) {
                    addEntries(child, childPath, results);
                }
            }
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

        @Override
        public Manifest getManifest() throws IOException {
            InputStream in = new FileInputStream(new File(root, JarFile.MANIFEST_NAME));
            try {
                return new Manifest(in);
            } finally {
                in.close();
            }
        }

        @Override
        public Entry getEntry(String path) {
            File file = new File(root, path);
            if (file.exists()) {
                return new FileEntry(file, path);
            }
            return null;
        }

        @Override
        public String getName() {
            return root.toString();
        }

        @Override
        protected int size() {
            return 0;
        }

        @Override
        public boolean isExtracted() {
            return true;
        }

        public class FileEntry implements Entry {
            private final File file;
            private final String path;

            /**
             * @param file
             */
            public FileEntry(File file, String path) {
                this.path = path;
                this.file = file;
            }

            @Override
            public String getName() {
                return path;
            }

            @Override
            public boolean isDirectory() {
                return file.isDirectory();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(file);
            }
        }
    }
}
