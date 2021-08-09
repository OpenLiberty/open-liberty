/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ras.instrument.internal.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;

import com.ibm.ws.ras.instrument.internal.introspect.TraceConfigPackageVisitor;
import com.ibm.ws.ras.instrument.internal.model.PackageInfo;

/**
 * Abstract base class that encapsulates some of the common file and argument
 * processing primitives needed by command line tools and ant tasks.
 */
public abstract class AbstractInstrumentation {

    /**
     * The list of class files to process.
     */
    protected List<File> classFiles = new ArrayList<File>();

    /**
     * The list of jar files containing classes to process.
     */
    protected List<File> jarFiles = new ArrayList<File>();

    /**
     * The list of errors that were encountered while processing the classes.
     */
    protected List<Throwable> errors = new ArrayList<Throwable>();

    /**
     * Flag that indicates debug code should be enabled for extra validation
     * and tracing.
     */
    protected boolean debug = false;

    protected Map<String, PackageInfo> packageInfoMap = new HashMap<String, PackageInfo>();

    /**
     * Get the list of class files that need to be processed.
     */
    public List<File> getClassFiles() {
        return this.classFiles;
    }

    /**
     * Set the list of class files that need to be processed.
     */
    public void setClassFiles(List<File> fileList) {
        this.classFiles = fileList;
    }

    /**
     * Get the list of zip or jar files that need to be processed.
     */
    public List<File> getJarFiles() {
        return this.jarFiles;
    }

    /**
     * Set the list of zip or jar files that need to be processed.
     */
    public void setJarFiles(List<File> fileList) {
        this.jarFiles = fileList;
    }

    /**
     * Turn on or off debug messages for problem determination.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Determine if debug messages should be issued.
     */
    public boolean isDebug() {
        return this.debug;
    }

    /**
     * Get the errors that occurred during instrumentation.
     */
    public List<Throwable> getErrors() {
        return errors;
    }

    /**
     * Recursively search the specified directory for class files.
     * 
     * @param root the directory to search.
     * @param parent the parent directory in the recursion or null if
     *            processing the top level directory.
     * 
     * @return the list of files found that are likely to be class files.
     */
    protected List<File> getClassFiles(File root, File parent) {

        if (root == null || !root.isDirectory()) {
            return null;
        }

        List<File> fileList = new ArrayList<File>();

        File[] files = root.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].isDirectory()) {
                List<File> subdirFiles = getClassFiles(files[i], parent);
                if (subdirFiles != null) {
                    fileList.addAll(subdirFiles);
                }
            } else if (files[i].getName().endsWith(".class")) {
                fileList.add(files[i]);
            }
        }

        return fileList;
    }

    /**
     * Recursively search the specified directory for jar files.
     * 
     * @param root the directory to search.
     * @param parent the parent directory in the recursion or null if
     *            processing the top level directory.
     * 
     * @return the list of files found that are likely to be class files.
     */
    protected List<File> getJarFiles(File root, File parent) {

        if (root == null || !root.isDirectory()) {
            return null;
        }

        List<File> fileList = new ArrayList<File>();

        File[] files = root.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].isDirectory()) {
                List<File> subdirFiles = getJarFiles(files[i], parent);
                if (subdirFiles != null) {
                    fileList.addAll(subdirFiles);
                }
            } else if (files[i].getName().endsWith(".jar")) {
                fileList.add(files[i]);
            } else if (files[i].getName().endsWith(".zip")) {
                fileList.add(files[i]);
            }
        }

        return fileList;
    }

    /**
     * Get the map of package configuration information.
     */
    protected Map<String, PackageInfo> getPackageInfoMap() {
        return packageInfoMap;
    }

    /**
     * Add package configuration information.
     */
    protected void addPackageInfo(PackageInfo packageInfo) {
        if (packageInfo != null) {
            packageInfoMap.put(packageInfo.getInternalPackageName(), packageInfo);
        }
    }

    /**
     * Hook point for command line tools to proces command line arguments.
     * 
     * @param args
     * @throws IOException
     */
    public abstract void processArguments(String[] args) throws IOException;

    /**
     * Get the package configuration information for the specified package.
     */
    protected PackageInfo getPackageInfo(String packageName) {
        return packageInfoMap.get(packageName);
    }

    /**
     * Instrument the classes and jar files.
     */
    public void executeInstrumentation() throws IOException {
        errors.clear();

        // Iterate over all entries in the classes list
        for (File f : this.classFiles) {
            try {
                if (!f.canRead() || !f.canWrite()) {
                    throw new IOException(f + " can not be replaced");
                }
                instrumentClassFile(f);
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(e);
            }
        }

        // Iterate over all entries in the jar list
        for (File f : this.jarFiles) {
            try {
                instrumentZipFile(f);
            } catch (Exception e) {
                e.printStackTrace();
                errors.add(e);
            }
        }

        // Chain the first exception and report the error count
        if (!errors.isEmpty()) {
            Throwable t = errors.get(0);
            IOException ioe = new IOException(errors.size() + " errors occurred");
            ioe.initCause(t);
            throw ioe;
        }
    }

    /**
     * Get an InputStream containing the byte code for the specified class.
     * 
     * @param classInternalName the internal name of the class to find
     * 
     * @return the input stream containing the class or null if the class is
     *         not part of the file set
     */
    protected InputStream getClassInputStream(String classInternalName) {
        if (classInternalName == null || "".equals(classInternalName)) {
            return null;
        }

        InputStream inputStream = null;
        try {
            for (File jarFile : jarFiles) {
                ZipFile zipFile = new ZipFile(jarFile);
                ZipEntry zipEntry = zipFile.getEntry(classInternalName + ".class");
                if (zipEntry != null) {
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    byte[] bytecode = toByteArray(zis);
                    if (isStreamForClass(new ByteArrayInputStream(bytecode), classInternalName)) {
                        inputStream = new ByteArrayInputStream(bytecode);
                    }
                    zis.close();
                }
                zipFile.close();

                if (inputStream != null) {
                    return inputStream;
                }
            }

            String fileName = classInternalName + ".class";
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
            for (File classFile : classFiles) {
                if (classFile.getName().equals(fileName)) {
                    FileInputStream fis = new FileInputStream(classFile);
                    byte[] bytecode = toByteArray(fis);
                    if (isStreamForClass(new ByteArrayInputStream(bytecode), classInternalName)) {
                        inputStream = new ByteArrayInputStream(bytecode);
                    }
                    fis.close();
                }
                if (inputStream != null) {
                    return inputStream;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }

    private boolean isStreamForClass(InputStream inputStream, String classInternalName) throws IOException {
        boolean streamForClass = false;
        try {
            ClassReader reader = new ClassReader(toByteArray(inputStream));
            streamForClass = reader.getClassName().equals(classInternalName);
        } finally {
            inputStream.reset();
        }
        return streamForClass;
    }

    /**
     * Instrument the specified class file.
     */
    public void instrumentClassFile(File classfile) throws IOException {
        FileInputStream fis = new FileInputStream(classfile);
        byte[] bytes = transform(fis);
        fis.close();
        fis = null;

        // If the class was successfully transformed, rewrite it
        if (bytes != null) {
            FileOutputStream fos = new FileOutputStream(classfile);
            fos.write(bytes);
            fos.close();
        }
    }

    /**
     * Instrument the specified zip file.
     */
    public void instrumentZipFile(File zf) throws IOException {
        if (!zf.canRead() || !zf.canWrite()) {
            throw new IOException(zf + " can not be replaced");
        }

        File tempFile = File.createTempFile(zf.getName(), null, zf.getParentFile());
        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("Unable to delete existing temp file " + tempFile.getName());
        }
        if (!zf.renameTo(tempFile)) {
            throw new IOException("Unable to rename existing jar " + zf);
        }

        ZipFile zipFile = new ZipFile(tempFile);
        FileOutputStream fos = new FileOutputStream(zf);
        ZipOutputStream zos = new ZipOutputStream(fos);
        boolean restoreZipFile = false;
        try {
            byte[] buffer = new byte[8192];
            Enumeration<? extends ZipEntry> e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry oldEntry = e.nextElement();
                InputStream zis = zipFile.getInputStream(oldEntry);
                byte[] transformedClass = null;

                if (oldEntry.getName().endsWith(".class")) {
                    transformedClass = transform(zis);
                    if (transformedClass == null) {
                        zis = zipFile.getInputStream(oldEntry);
                    }
                }

                ZipEntry newEntry = new ZipEntry(oldEntry.getName());
                newEntry.setTime(transformedClass == null ? oldEntry.getTime() : System.currentTimeMillis());
                newEntry.setComment(oldEntry.getComment());
                newEntry.setExtra(oldEntry.getExtra());
                newEntry.setSize(transformedClass == null ? oldEntry.getSize() : transformedClass.length);

                if (newEntry.getSize() > Integer.MAX_VALUE) {
                    throw new IOException("ZipEntry too large: " + newEntry.getSize());
                }

                int entrySize = (int) newEntry.getSize();
                if (entrySize > buffer.length) {
                    buffer = new byte[entrySize];
                }

                if (transformedClass != null) {
                    System.arraycopy(transformedClass, 0, buffer, 0, entrySize);
                } else {
                    int totalBytesRead = 0;
                    while (totalBytesRead < entrySize) {
                        int bytesRead = zis.read(buffer, totalBytesRead, entrySize - totalBytesRead);
                        if (bytesRead == -1) {
                            throw new IOException("End of file encountered");
                        }
                        totalBytesRead += bytesRead;
                    }
                }

                zos.putNextEntry(newEntry);
                zos.write(buffer, 0, entrySize);
            }
        } catch (IOException ioe) {
            restoreZipFile = true;
            throw ioe;
        } catch (Throwable t) {
            restoreZipFile = true;
            IOException ioe = new IOException("Unexpected exception encountered during instrumentation");
            ioe.initCause(t);
            throw ioe;
        } finally {
            zos.close();
            fos.close();
            zipFile.close();
            if (restoreZipFile) {
                zf.delete();
                tempFile.renameTo(zf);
            } else {
                tempFile.delete();
            }
        }
    }

    /**
     * Process the package level annotations that exist in the filesets.
     * 
     * @throws IOException
     */
    public void processPackageInfo() throws IOException {
        for (File classFile : classFiles) {
            if (classFile.getName().equals("package-info.class")) {
                FileInputStream fis = new FileInputStream(classFile);
                addPackageInfo(processPackageInfo(fis));
                fis.close();
            }
        }
        for (File jarFile : jarFiles) {
            ZipFile zipFile = new ZipFile(jarFile);
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();
                if (zipEntry.getName().endsWith("/package-info.class")) {
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    addPackageInfo(processPackageInfo(zis));
                }
            }
            zipFile.close();
        }
    }

    /**
     * Process the package level annotation data for trace instrumentation.
     * 
     * @param packageInfoResource an <code>InputStream</code> that contains the
     *            byte code for the package annotation interface. This is generally
     *            the package-info byte code.
     */
    protected PackageInfo processPackageInfo(InputStream packageInfoResource) {
        if (packageInfoResource == null)
            return null;

        PackageInfo packageInfo = null;
        try {
            ClassReader cr = new ClassReader(packageInfoResource);
            TraceConfigPackageVisitor packageVisitor = new TraceConfigPackageVisitor();
            cr.accept(packageVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            packageInfo = packageVisitor.getPackageInfo();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return packageInfo;
    }

    /**
     * Instrument the class at the current position in the specified input stream.
     * 
     * @param classfileStream the class file byte stream to be transformed
     * 
     * @return instrumented class file or null if the class has already
     *         been instrumented.
     * 
     * @throws IOException if an error is encountered while reading from
     *             the <code>InputStream</code>
     */
    protected abstract byte[] transform(InputStream classfileStream) throws IOException;

}
