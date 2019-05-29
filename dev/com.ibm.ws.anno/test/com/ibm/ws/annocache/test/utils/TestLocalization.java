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

package com.ibm.ws.annocache.test.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestLocalization {

    // The linked and unlinked paths are used to handle paths to repository
	// folders, which do not resolve properly in eclipse as linked folders.
	//
	// When running in eclipse, unlinked files in the project folder resolve
	// correctly, and when running from the command line, files in the project
	// folder resolve correctly.
	//
	// When running in eclipse, linked files do not resolve.  Resolution of linked
	// files needs an explicit link to the repository location.

	/** Path used to locate unlinked test resources. */
    public static final String UNLINKED_PROJECT_PATH = "./";

    /** Path used to locate linked test resources. */
    public static final String LINKED_PROJECT_PATH = null;
    // public static final String LINKED_PROJECT_PATH = "c:/dev/repos-pub/anno-beta/dev/com.ibm.ws.anno";

    public static String getEclipseProjectPath() {
        return UNLINKED_PROJECT_PATH;
    }

    public static String getRepositoryProjectPath() {
        return LINKED_PROJECT_PATH;
    }

    public static String selectProjectPath(String partialPath) {
        if ( LINKED_PROJECT_PATH == null ) {
            return partialPath;
        }

        System.out.println("Placing project folder [ " + partialPath + " ]:");

        File eclipseFile = new File(partialPath);
        String eclipsePath = eclipseFile.getAbsolutePath();
        System.out.println("Trying eclipse location [ " + eclipsePath + " ]");

        if ( eclipseFile.exists() ) {
            return partialPath;
        }

        File repoRootFile = new File(LINKED_PROJECT_PATH);
        String repoRootPath = repoRootFile.getAbsolutePath();
        System.out.println("Trying repository location [ " + repoRootPath + " ]");

        if ( !repoRootFile.exists() ) {
            String errorMessage =
                "Unable to locate eclipse file [ " + eclipsePath + " ]" +
                " or repository root file [ " + repoRootPath + " ]";
            throw new IllegalArgumentException(errorMessage);
        }

        File repoFile = new File(repoRootFile, partialPath);
        String repoPath = repoFile.getAbsolutePath();
        System.out.println("Verifying target location [ " + repoPath + " ]");

        if ( repoFile.exists() ) {
            return repoPath;
        }

        String errorMessage =
            "Unable to locate eclipse file [ " + eclipsePath + " ]" +
            "; located repository root file [ " + repoRootPath + " ]" +
            " but failed to locate target file [ " + repoPath + " ]";
        throw new IllegalArgumentException(errorMessage);
    }

    public static String putIntoProject(String path) {
        return selectProjectPath( putInto(UNLINKED_PROJECT_PATH, path) );
    }

    public static String putIntoProject(String path1, String path2) {
        return selectProjectPath( putInto(UNLINKED_PROJECT_PATH, path1, path2) );
    }

    public static final String DATA_RELATIVE_PATH = "publish/appData/";
    public static final String DATA_PATH = putIntoProject(DATA_RELATIVE_PATH);

    public static final String STORAGE_RELATIVE_PATH = "build/appData/";
    public static final String STORAGE_PATH = putIntoProject(STORAGE_RELATIVE_PATH);

    public static final String LOGS_RELATIVE_PATH = "build/logs/";
    public static final String LOGS_PATH = putIntoProject(LOGS_RELATIVE_PATH);
    
    public static final String CLASSES_RELATIVE_PATH_LIBERTY = "build/classes/java/test/";
    public static final String CLASSES_RELATIVE_PATH_ECLIPSE = "bin/";

    public static final String CLASSES_RELATIVE_PATH;
    public static final String CLASSES_PATH;

    static {
        String useClassesPath;
        String useClassesRelativePath;
        String pathCase;

        String libertyClasses = putIntoProject(CLASSES_RELATIVE_PATH_LIBERTY);
        File libertyClassesFile = new File(libertyClasses);
        if ( libertyClassesFile.exists() ) {
            useClassesPath = libertyClasses;
            useClassesRelativePath = CLASSES_RELATIVE_PATH_LIBERTY;
            pathCase = "Standard liberty classes folder";

        } else {
            String eclipseClasses = putIntoProject(CLASSES_RELATIVE_PATH_ECLIPSE);
            File eclipseClassesFile = new File(eclipseClasses);
            if ( eclipseClassesFile.exists() ) {
                useClassesPath = eclipseClasses;
                useClassesRelativePath = CLASSES_RELATIVE_PATH_ECLIPSE;
                pathCase = "Eclipse classes folder";

            } else {
                String msg =
                    "Neither liberty classes [ " + libertyClassesFile.getAbsolutePath() + " ]" +
                    " nor eclipse classes [ " + eclipseClassesFile.getAbsolutePath() + " ] exists";
                throw new IllegalArgumentException(msg);
            }
        }

        CLASSES_RELATIVE_PATH = useClassesRelativePath;
        CLASSES_PATH = useClassesPath;

        System.out.println("Classes path selection [ " + pathCase + " ]");
        System.out.println("Classes full path [ " + CLASSES_PATH + " ]");
        System.out.println("Classes relative path [ " + CLASSES_RELATIVE_PATH + " ]");
    }

    public static String getDataRelativePath() {
        return DATA_RELATIVE_PATH;
    }

    public static String getDataPath() {
        return DATA_PATH;
    }

    public static String putIntoData(String path) {
        return putInto(DATA_PATH, path);
    }

    public static String putIntoData(String path1, String path2) {
        return putInto(DATA_PATH, path1, path2);
    }

    public static String getStorageRelativePath() {
        return STORAGE_RELATIVE_PATH;
    }

    public static String getStoragePath() {
        return STORAGE_PATH;
    }

    public static String putIntoStorage(String path) {
        return putInto(STORAGE_PATH, path);
    }

    public static String putIntoStorage(String path1, String path2) {
        return putInto(STORAGE_PATH, path1, path2);
    }

    public static String getClassesRelativePath() {
        return CLASSES_RELATIVE_PATH;
    }

    public static String getClassesPath() {
        return CLASSES_PATH;
    }

    public static String putIntoClasses(String path) {
        return putInto(CLASSES_PATH, path);
    }

    //

    public static String putInto(String rootPath, String childPath) {
        char lastRootChar = rootPath.charAt( rootPath.length() - 1 );
        if ( (lastRootChar != '/') && (lastRootChar != '\\') ) {
            rootPath += '/';
        }
        return rootPath + childPath;
    }

    public static String putInto(String projectPath, String dataPath, String path) {
        return putInto( projectPath, putInto(dataPath, path) );
    }

    public static List<String> putInto(String rootPath, List<String> childPaths) {
        List<String> adjustedPaths = new ArrayList<String>(childPaths.size());

        for ( String nextChildPath : childPaths ) {
            adjustedPaths.add( putInto(rootPath, nextChildPath) );
        }

        return adjustedPaths;
    }

    public static String[] putInto(String rootPath, String[] childPaths) {
        String[] adjustedPaths = new String[ childPaths.length ];

        for ( int pathNo = 0; pathNo < childPaths.length; pathNo++ ) {
            adjustedPaths[pathNo] = putInto( rootPath, childPaths[pathNo] );
        }

        return adjustedPaths;
    }
    
    public static List<String> putInto(String projectPath, String dataPath, List<String> paths) {
        List<String> adjustedPaths = new ArrayList<String>(paths.size());

        for ( String nextPath : paths ) {
            adjustedPaths.add( putInto(projectPath, dataPath, nextPath) );
        }

        return adjustedPaths;
    }
    
    //

    public static List<String> collectPaths(File rootDir) throws IOException {
        List<String> fileNames = new ArrayList<String>();

        int rootLength = rootDir.getCanonicalPath().length(); // 'getCanonicalPath' throws IOException

        collectPaths(
            fileNames,
            rootDir, rootLength,
            rootDir); // throws IOException

        return fileNames;
    }

    private static final boolean DO_NORMALIZE = ('\\' == File.separatorChar);

    public static void collectPaths(
        List<String> fileNames,
        File rootDir, int rootLength,
        File currentDir) throws IOException {

        File[] childFiles = currentDir.listFiles();
        if ( childFiles == null ) {
            return;
        }
        
        for ( File childFile : childFiles ) {
            if ( childFile.isDirectory() ) {
                collectPaths(fileNames, rootDir, rootLength, childFile); // throws IOException

            } else {
                String filePath = childFile.getCanonicalPath(); // throws IOException
                String fileRelativePath = filePath.substring(rootLength + 1);

                if ( DO_NORMALIZE ) {
                    fileRelativePath = fileRelativePath.replace('\\', '/');
                }

                fileNames.add(fileRelativePath);
            }
        }
    }
}
