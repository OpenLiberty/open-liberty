/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;

public final class FileUtils {
    private static final long RETRY_SLEEP_MILLIS = 10L;
    private static File defaultTempDir;
    private static Thread shutdownHook;
    private static final char[] ILLEGAL_CHARACTERS
        = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    private FileUtils() {

    }

    public static boolean isValidFileName(String name) {
        for (int i = name.length(); i > 0; i--) {
            char c = name.charAt(i - 1);
            for (char c2 : ILLEGAL_CHARACTERS) {
                if (c == c2) {
                    return false;
                }
            }
        }
        File file = new File(getDefaultTempDir(), name);
        boolean isValid = true;
        try {
            if (exists(file)) {
                return true;
            }
            if (file.createNewFile()) {
                file.delete();
            }
        } catch (IOException e) {
            isValid = false;
        }
        return isValid;
    }

    public static synchronized File getDefaultTempDir() {
        if (defaultTempDir != null
            && exists(defaultTempDir)) {
            return defaultTempDir;
        }

        String s = SystemPropertyAction.getPropertyOrNull(FileUtils.class.getName() + ".TempDirectory");
        if (s != null) {
            //assume someone outside of us will manage the directory
            File f = new File(s);
            if (f.mkdirs()) {
                defaultTempDir = f;
            }
        }
        if (defaultTempDir == null) {
            defaultTempDir = createTmpDir(false);
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            shutdownHook = new Thread() {
                @Override
                public void run() {
                    removeDir(defaultTempDir, true);
                }
            };
            Runtime.getRuntime().addShutdownHook(shutdownHook);

        }
        return defaultTempDir;
    }

    public static synchronized void maybeDeleteDefaultTempDir() {
        if (defaultTempDir != null) {
            Runtime.getRuntime().gc(); // attempt a garbage collect to close any files
            String[] files = defaultTempDir.list();
            if (files != null && files.length > 0) {
                //there are files in there, we need to attempt some more cleanup

                //HOWEVER, we don't want to just wipe out every file as something may be holding onto
                //the files for a reason. We'll re-run the gc and run the finalizers to see if
                //anything gets cleaned up.
                Runtime.getRuntime().gc(); // attempt a garbage collect to close any files
                Runtime.getRuntime().runFinalization();
                Runtime.getRuntime().gc();
                files = defaultTempDir.list();
            }
            if (files == null || files.length == 0) {
                //all the files are gone, we can remove the shutdownhook and reset
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    shutdownHook.run();
                } catch (IllegalStateException ex) {
                    // The JVM is already shutting down so do nothing
                }
                shutdownHook = null;
                defaultTempDir = null;
            }
        }
    }

    public static File createTmpDir() {
        return createTmpDir(true);
    }
    public static File createTmpDir(boolean addHook) {
        String s = SystemPropertyAction.getProperty("java.io.tmpdir");
        File checkExists = new File(s);
        if (!exists(checkExists) || !checkExists.isDirectory()) {
            throw new RuntimeException("The directory "
                                   + checkExists.getAbsolutePath()
                                   + " does not exist, please set java.io.tempdir"
                                   + " to an existing directory");
        }
        if (!checkExists.canWrite()) {
            throw new RuntimeException("The directory "
                                   + checkExists.getAbsolutePath()
                                   + " is not writable, please set java.io.tempdir"
                                   + " to a writable directory");
        }
        if (checkExists.getUsableSpace() < 1024 * 1024) {
            LogUtils.getL7dLogger(FileUtils.class).warning("The directory " + s + " has very "
                                                           + "little usable temporary space.  Operations"
                                                           + " requiring temporary files may fail.");
        }

        File newTmpDir;
        try {
            Path path = Files.createTempDirectory(checkExists.toPath(), "cxf-tmp-");
            File f = path.toFile();
            f.deleteOnExit();
            newTmpDir = f;
        } catch (IOException ex) {
            Random r = new Random();
            File f = new File(checkExists, "cxf-tmp-" + r.nextInt());
            for (int count = 0; !f.mkdir(); count++) {
                if (count > 10000) {
                    throw new RuntimeException("Could not create a temporary directory in "
                                               + s + ",  please set java.io.tempdir"
                                               + " to a writable directory");
                }
                f = new File(checkExists, "cxf-tmp-" + r.nextInt());
            }
            newTmpDir = f;
        }
        if (addHook) {
            final File f2 = newTmpDir;
            Thread hook = new Thread() {
                @Override
                public void run() {
                    removeDir(f2, true);
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);
        }
        return newTmpDir;
    }

    public static void mkDir(File dir) {
        if (dir == null) {
            throw new RuntimeException("dir attribute is required");
        }

        if (dir.isFile()) {
            throw new RuntimeException("Unable to create directory as a file "
                                    + "already exists with that name: " + dir.getAbsolutePath());
        }

        if (!exists(dir)) {
            boolean result = doMkDirs(dir);
            if (!result) {
                String msg = "Directory " + dir.getAbsolutePath()
                             + " creation was not successful for an unknown reason";
                throw new RuntimeException(msg);
            }
        }
    }

    /**
     * Attempt to fix possible race condition when creating directories on
     * WinXP, also Windows2000. If the mkdirs does not work, wait a little and
     * try again.
     */
    private static boolean doMkDirs(File f) {
        if (!f.mkdirs()) {
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
                return f.mkdirs();
            } catch (InterruptedException ex) {
                return f.mkdirs();
            }
        }
        return true;
    }

    public static void removeDir(File d) {
        removeDir(d, false);
    }
    private static void removeDir(File d, boolean inShutdown) {
        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                removeDir(f, inShutdown);
            } else {
                delete(f, inShutdown);
            }
        }
        delete(d, inShutdown);
    }

    public static void delete(File f) {
        delete(f, false);
    }
    public static void delete(File f, boolean inShutdown) {
        if (!f.delete()) {
            try {
                Thread.sleep(RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            if (!f.delete() && !inShutdown) {
                f.deleteOnExit();
            }
        }
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return createTempFile(prefix, suffix, null, false);
    }

    public static File createTempFile(String prefix, String suffix, File parentDir,
                               boolean deleteOnExit) throws IOException {
        File parent = (parentDir == null)
            ? getDefaultTempDir()
            : parentDir;

        if (prefix == null) {
            prefix = "cxf";
        } else if (prefix.length() < 3) {
            prefix = prefix + "cxf";
        }
        File result = Files.createTempFile(parent.toPath(), prefix, suffix).toFile();

        //if parentDir is null, we're in our default dir
        //which will get completely wiped on exit from our exit
        //hook.  No need to set deleteOnExit() which leaks memory.
        if (deleteOnExit && parentDir != null) {
            result.deleteOnExit();
        }
        return result;
    }

    public static String getStringFromFile(File location) {
        InputStream is = null;
        String result = null;

        try {
            is = Files.newInputStream(location.toPath());
            result = normalizeCRLF(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    //do nothing
                }
            }
        }

        return result;
    }

    public static String normalizeCRLF(InputStream instream) {
        BufferedReader in = new BufferedReader(new InputStreamReader(instream));
        StringBuilder result = new StringBuilder();
        String line = null;

        try {
            line = in.readLine();
            while (line != null) {
                String[] tok = line.split("\\s");

                for (int x = 0; x < tok.length; x++) {
                    String token = tok[x];
                    result.append("  ").append(token);
                }
                line = in.readLine();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String rtn = result.toString();

        rtn = ignoreTokens(rtn, "<!--", "-->");
        rtn = ignoreTokens(rtn, "/*", "*/");
        return rtn;
    }

    private static String ignoreTokens(final String contents,
                                       final String startToken, final String endToken) {
        String rtn = contents;
        int headerIndexStart = rtn.indexOf(startToken);
        int headerIndexEnd = rtn.indexOf(endToken);
        if (headerIndexStart != -1 && headerIndexEnd != -1 && headerIndexStart < headerIndexEnd) {
            rtn = rtn.substring(0, headerIndexStart - 1)
                + rtn.substring(headerIndexEnd + endToken.length() + 1);
        }
        return rtn;
    }

    public static List<File> getFilesUsingSuffix(File dir, final String suffix) {
        return getFilesRecurseUsingSuffix(dir, suffix, false, new ArrayList<>());
    }

    public static List<File> getFilesRecurseUsingSuffix(File dir, final String suffix) {
        return getFilesRecurseUsingSuffix(dir, suffix, true, new ArrayList<>());
    }

    private static List<File> getFilesRecurseUsingSuffix(File dir, final String suffix,
                                                        boolean rec, List<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            int suffixLength = suffix.length();
            for (File file : files) {
                if (file.isDirectory() && rec) {
                    getFilesRecurseUsingSuffix(file, suffix, rec, fileList);
                } else {
                    if (file.getName().endsWith(suffix) && file.getName().length() > suffixLength) {
                        fileList.add(file);
                    }
                }
            }
        }
        return fileList;
    }

    public static List<File> getFiles(File dir, final String pattern) {
        List<File> fileList = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            Pattern p = Pattern.compile(pattern);
            for (File file : files) {
                Matcher m = p.matcher(file.getName());
                if (m.matches()) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    public static List<String> readLines(File file) throws Exception {
        if (!exists(file)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(file.toPath());
    }

    //Liberty change start - no longer needed at CXF 3.3.3
    public static boolean exists(File file) {
        if (System.getSecurityManager() == null) {
            return file.exists();
        }
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> {
            return file.exists();
        });
    }
    //Liberty change end
    /**
     * Strips any leading paths
     */
    public static String stripPath(String name) {
        if (name == null) {
            return null;
        }
        int posUnix = name.lastIndexOf('/');
        int posWin = name.lastIndexOf('\\');
        int pos = Math.max(posUnix, posWin);

        if (pos != -1) {
            return name.substring(pos + 1);
        }
        return name;
    }
}
