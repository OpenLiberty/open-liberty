/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.shared;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;

/**
 *
 */
public class TestUtils {
    private static AtomicInteger count = new AtomicInteger(0);
    static Field bootJarField = null;
    static Field bootLibDirField = null;
    static Field utilsInstallDirField = null;

    public static File findBuiltKernelBundle() {
        File root = new File(Constants.BOOTSTRAP_LIB_DIR);

        File fileList[] = root.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("com.ibm.ws.kernel.boot.*\\.jar");
            }
        });

        if (fileList == null || fileList.length < 1)
            throw new RuntimeException("Unable to find com.ibm.ws.kernel.boot.*\\.jar in " + root.getAbsolutePath());

        return fileList[0];
    }

    public static void setKernelUtilsBootstrapJar(File bootstrapJar) throws Exception {
        if (bootJarField == null) {
            bootJarField = KernelUtils.class.getDeclaredField("launchHome");
            bootJarField.setAccessible(true);
        }
        bootJarField.set(null, bootstrapJar);
    }

    public static void setKernelUtilsBootstrapLibDir(File bootLibDir) throws Exception {
        if (bootLibDirField == null) {
            bootLibDirField = KernelUtils.class.getDeclaredField("libDir");
            bootLibDirField.setAccessible(true);
        }
        bootLibDirField.set(null, bootLibDir);
    }

    public static void setUtilsInstallDir(File installDir) throws Exception {
        if (utilsInstallDirField == null) {
            utilsInstallDirField = Utils.class.getDeclaredField("installDir");
            utilsInstallDirField.setAccessible(true);
        }
        utilsInstallDirField.set(null, installDir);
    }

    public static File createTempFile(String name, String suffix) throws IOException {
        if (!Constants.TEST_TMP_ROOT_FILE.isDirectory()) {
            Constants.TEST_TMP_ROOT_FILE.mkdirs();
        }
        return File.createTempFile(name, suffix, Constants.TEST_TMP_ROOT_FILE);
    }

    public static File createTempFile(String name, String suffix, File dir) throws IOException {
        return File.createTempFile(name, suffix, dir);
    }

    public static File createTempDirectory(String name) throws IOException {
        File f = new File(Constants.TEST_TMP_ROOT_FILE, name + count.incrementAndGet());

        if (!f.exists() && !f.mkdirs()) {
            System.out.println("alex: file creation failed for: " + f.getAbsolutePath());
            throw new IOException("Unable to create temporary directory");
        }
        return f;
    }

    public static void cleanTempFiles(File dir) {
        recursiveClean(dir);
    }

    public static void cleanTempFiles() {
        recursiveClean(Constants.TEST_TMP_ROOT_FILE);
    }

    private static boolean recursiveClean(final File fileToRemove) {
        if (fileToRemove == null)
            return true;

        if (!fileToRemove.exists())
            return true;

        boolean success = true;

        if (fileToRemove.isDirectory()) {
            File[] files = fileToRemove.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    success |= recursiveClean(file);
                } else {
                    success |= file.delete();
                }
            }
            files = fileToRemove.listFiles();
            if (files.length == 0)
                success |= fileToRemove.delete();
        } else {
            success |= fileToRemove.delete();
        }
        return success;
    }

    public static Method getMethod(Class<?> subjectClass, String methodName, Class<?>... parameterClasses) throws NoSuchMethodException {
        try {
            return subjectClass.getDeclaredMethod(methodName, parameterClasses);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = subjectClass.getSuperclass();
            if (superClass != null) {
                return getMethod(superClass, methodName, parameterClasses);
            } else {
                throw new NoSuchMethodException();
            }
        }
    }
}
