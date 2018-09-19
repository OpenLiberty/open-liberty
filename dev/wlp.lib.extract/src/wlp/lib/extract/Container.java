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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
public abstract class Container implements Iterable<Container.Entry> {

    public abstract Manifest getManifest() throws IOException;

    public abstract Entry getEntry(String path);

    public abstract String getName();

    protected abstract int size();

    public void close() throws IOException {
        // do nothing by default
    }

    public interface Entry {
        public String getName();

        public boolean isDirectory();

        public InputStream getInputStream() throws IOException;
    }

    public static Container build(File f) throws IOException {
        if (f.isDirectory()) {
            return new DirectoryContainer(f.toPath());
        }
        return new JarContainer(new JarFile(f));
    }

    public static class JarContainer extends Container {
        private final JarFile jar;
        private final List<Entry> entries = new ArrayList<Container.Entry>();

        public JarContainer(JarFile jar) {
            this.jar = jar;
            for (Enumeration<JarEntry> eJarEntries = this.jar.entries(); eJarEntries.hasMoreElements();) {
                JarEntry jarEntry = eJarEntries.nextElement();
                entries.add(new JarContainerEntry(jarEntry));
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
                return new JarContainerEntry(jarEntry);
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

        public class JarContainerEntry implements Entry {
            private final JarEntry jarEntry;

            public JarContainerEntry(JarEntry jarEntry) {
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

    public static class DirectoryContainer extends Container {
        private final Path root;
        private final List<Entry> entries = new ArrayList<Container.Entry>();

        public DirectoryContainer(Path root) {
            this.root = root;
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!attrs.isDirectory()) {
                            entries.add(new FileEntry(file));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Iterator<Entry> iterator() {
            return entries.iterator();
        }

        @Override
        public Manifest getManifest() throws IOException {
            InputStream in = new FileInputStream(new File(root.toFile(), JarFile.MANIFEST_NAME));
            try {
                return new Manifest(in);
            } finally {
                in.close();
            }
        }

        @Override
        public Entry getEntry(String path) {
            File file = new File(root.toFile(), path);
            if (file.exists()) {
                return new FileEntry(file.toPath());
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

        public class FileEntry implements Entry {
            private final Path path;
            private final String sPath;

            /**
             * @param file
             */
            public FileEntry(Path p) {
                this.path = root.relativize(p);
                StringBuffer pBuf = new StringBuffer();
                for (Path component : path) {
                    if (pBuf.length() != 0) {
                        pBuf.append('/');
                    }
                    pBuf.append(component.getFileName().toString());
                }
                sPath = pBuf.toString();
            }

            @Override
            public String getName() {
                return sPath;
            }

            @Override
            public boolean isDirectory() {
                return path.toFile().isDirectory();
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(path.toFile());
            }

        }
    }
}
