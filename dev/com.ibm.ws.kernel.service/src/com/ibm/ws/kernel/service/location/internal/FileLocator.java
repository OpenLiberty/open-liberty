/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Locates files and directories using filters on names.
 */
public final class FileLocator {
    private FileLocator() {
        throw new AssertionError("This class is not instantiable");
    }

    /**
     * Look for given file in any of the specified directories, return the first
     * one found.
     * 
     * @param name
     *            The name of the file to find
     * @param pathNameList
     *            The list of directories to check
     * @return The File object if the file is found; null if name is null,
     *         pathNameList is null or empty, or file is not found.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file.
     */
    public static File findFileInNamedPath(String name, Collection<String> pathNameList) {
        if (name == null || pathNameList == null || pathNameList.size() == 0)
            return null;

        for (String path : pathNameList) {
            if (path == null || path.length() == 0)
                continue;

            File result = getFile(new File(path), name);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * Look for given file in any of the specified directories, return the first
     * one found.
     * 
     * @param name
     *            The name of the file to find
     * @param pathList
     *            The list of directories to check
     * @return The File object if the file is found; null if name is null,
     *         pathList is null or empty, or file is not found.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file.
     */
    public static File findFileInFilePath(String name, Collection<File> pathList) {
        if (name == null || pathList == null || pathList.size() == 0)
            return null;

        for (File dirPath : pathList) {
            File result = getFile(dirPath, name);
            if (result != null)
                return result;
        }
        return null;
    }

    /**
     * @param resourceURI
     * @param path
     * @return
     */
    public static File findLowerCaseFileInFilePath(String name, Collection<File> pathList) {
        if (name == null || pathList == null || pathList.size() == 0)
            return null;

        // Exact name
        for (File dirPath : pathList) {
            File result = getFile(dirPath, name);
            if (result != null)
                return result;
        }

        // Fallback to case insensitive file name
        for (File dirPath : pathList) {
            FileFilter filter = new LowerCaseFileName(name);
            File[] fileList = dirPath.listFiles(filter);

            if (fileList != null && fileList.length >= 1) {
                return fileList[0];
            }
        }

        return null;
    }

/** Internal method: performs test common to 
     * {@link #findFileInNamedPath(String, List)}
     * and {@link #findFileInFilePath(String, Collection)
     *
     * @param dirPath
     *            Directory to check for named file
     * @param name
     *            The name of the file to find
     * @return The File object if the file is found;
     *         null if the dirPath is null, does not exist, or is not a directory.
     */
    private static File getFile(File dirPath, String name) {
        if (dirPath != null && dirPath.isDirectory()) {
            File myFile = new File(dirPath, name);
            if (myFile.exists())
                return myFile;
        }
        return null;
    }

    /**
     * Look for given file in any of the specified directories, return the first
     * one found.
     * 
     * @param name
     *            The name of the file to find
     * @param pathNameList
     *            The collection of directories to check
     * @return The File object if the file is found;
     *         null if the pathList is null or empty, or file is not found.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file.
     */
    public static File matchFileInNamedPath(String regex, Collection<String> pathNameList) {
        if (regex == null || pathNameList == null || pathNameList.size() == 0)
            return null;

        for (String currentPath : pathNameList) {
            if (currentPath == null || currentPath.length() == 0)
                continue;

            File result = matchFile(new File(currentPath), regex);
            if (result != null)
                return result;
        }

        return null;
    }

    /**
     * Look for given file in any of the specified directories, return the first
     * one found.
     * 
     * @param name
     *            The name of the file to find
     * @param pathList
     *            The list of directories to check
     * @return The File object if the file is found;
     *         null if the pathList is null or empty, or file is not found.
     * 
     * @throws SecurityException
     *             If a security manager exists and its <code>{@link java.lang.SecurityManager#checkRead(java.lang.String)}</code> method denies read access to the file.
     */
    public static File matchFileInFilePath(String regex, Collection<File> pathList) {
        if (regex == null || pathList == null || pathList.size() == 0)
            return null;

        for (File dirPath : pathList) {
            File result = matchFile(dirPath, regex);
            if (result != null)
                return result;
        }

        return null;
    }

/**
     * Internal method: performs test common to matchFileInNamedPath(String, List)
     * and {@link #matchFileInFilePath(String, Collection)
     *
     * @param dirPath
     *            Directory to check for named file
     * @param name
     *            The name of the file to find
     * @return The File object if the file is found;
     *         null if the dirPath is null, does not exist, or is not a directory.
     */
    private static File matchFile(File dirPath, String regex) {
        if (dirPath != null && dirPath.isDirectory()) {
            List<File> fileList = FileLocator.getMatchingFiles(dirPath, regex);
            for (File currentFile : fileList) {
                if (currentFile.exists())
                    return currentFile;
            }
        }
        return null;
    }

    /**
     * Get a list of Files from the given path that match the provided filter;
     * not recursive.
     * 
     * @param root
     *            base directory to look for files
     * @param filterExpr
     *            the regular expression to match, may be null
     * @return List of File objects; List will be empty if root is null
     */
    public static List<File> getMatchingFiles(String root, String filterExpr) {
        if (root == null)
            return Collections.emptyList();

        File file = new File(root);
        return getMatchingFiles(file, filterExpr);
    }

    /**
     * Get a list of Files from the given path that match the provided filter;
     * not recursive.
     * 
     * @param root
     *            base directory to look for files
     * @param filterExpr
     *            the regular expression to match, may be null
     * @return List of File objects; List will be empty if root is null
     */
    public static List<File> getMatchingFiles(File root, String filterExpr) {
        if (root == null)
            return Collections.emptyList();

        File fileList[];
        FileFilter filter;

        if (filterExpr == null)
            filter = new FilesOnly();
        else
            filter = new FilesOnlyFilter(filterExpr);

        fileList = root.listFiles(filter);

        if (fileList == null)
            return Collections.emptyList();
        else {
            if (fileList.length > 1) {
                Arrays.sort(fileList, new SortByFileName());
            }
            return Arrays.asList(fileList);
        }
    }

    /**
     * Get a list of file names from the given path that match the provided
     * filter; not recursive.
     * 
     * @param root
     *            base directory to look for files
     * @param filterExpr
     *            the regular expression to match, may be null
     * @return List of File objects; List will be empty if root is null
     */
    public static List<String> getMatchingFileNames(String root, String filterExpr, boolean fullPath) {
        List<File> fileList = getMatchingFiles(root, filterExpr);
        List<String> list = new ArrayList<String>(fileList.size());

        for (File f : fileList) {
            if (fullPath)
                list.add(f.getAbsolutePath());
            else
                list.add(f.getName());
        }

        return list;
    }

    /**
     * Get a list of all sub-directories (as String names) located in the provided
     * directory
     * 
     * @param root
     *            Starting directory
     * @param fullPath
     *            If true, return full paths, paths could otherwise be relative
     * @return Array of String directory names
     */
    public static List<String> getDirectoryNames(String root, boolean fullPath) {
        List<File> fileList = getDirectoriesByName(root);
        List<String> list = new ArrayList<String>(fileList.size());

        for (File f : fileList) {
            String name = fullPath ? f.getAbsolutePath() : f.getName();
            list.add(name);
        }

        return list;
    }

    /**
     * Get a list of all sub-directories (as Files) located in the provided
     * directory
     * 
     * @param root
     *            Starting directory
     * @return List of File objects (one per sub-directory), or null
     * @see File#listFiles(FileFilter)
     */
    public static List<File> getDirectoriesByName(String root) {
        if (root == null || root.length() == 0)
            return Collections.emptyList();

        return getDirectories(new File(root));
    }

    /**
     * Get a list of all sub-directories (as Files) located in the provided
     * directory
     * 
     * @param root
     *            Starting directory
     * @return List of File objects (one per sub-directory), or null
     * @see File#listFiles(FileFilter)
     */
    public static List<File> getDirectories(File root) {
        if (root == null)
            return Collections.emptyList();

        FileFilter filter = new DirsOnlyFilter();

        File fileList[] = root.listFiles(filter);

        if (fileList == null)
            return Collections.emptyList();
        else
            return Arrays.asList(fileList);
    }

    /**
     * Inner class: only accept directories
     */
    static class DirsOnlyFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            if (file != null && file.isDirectory())
                return true;

            return false;
        }
    }

    /**
     * Inner class: only accept files
     */
    static class FilesOnly implements FileFilter {
        @Override
        public boolean accept(File file) {
            if (file != null && file.isFile()) {
                return true;
            }

            return false;
        }
    }

    /**
     * Inner class: only accept files
     */
    static class LowerCaseFileName implements FileFilter {
        String name;

        /**
         * @param filterExpr
         *            the regular expression to match
         */
        public LowerCaseFileName(String name) {
            this.name = name.toLowerCase();
        }

        @Override
        public boolean accept(File file) {
            if (file != null && file.isFile()) {
                String fileName = file.getName().toLowerCase();
                if (name.equals(fileName))
                    return true;
            }

            return false;
        }
    }

    /**
     * Inner class: only accept files whose names match the given regular
     * expression
     */
    static class FilesOnlyFilter implements FileFilter {
        String filterExpr;

        /**
         * @param filterExpr
         *            the regular expression to match
         */
        public FilesOnlyFilter(String filterExpr) {
            this.filterExpr = filterExpr;
        }

        @Override
        public boolean accept(File file) {
            if (file != null && file.isFile()) {
                String name = file.getName();
                if (name.matches(filterExpr))
                    return true;
            }
            return false;
        }
    }

    /**
     * Inner class: sort by file name
     */
    static class SortByFileName implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    }
}
