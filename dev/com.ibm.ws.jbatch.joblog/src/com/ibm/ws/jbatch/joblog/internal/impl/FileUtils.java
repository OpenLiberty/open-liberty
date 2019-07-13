/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog.internal.impl;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    //Limit sub directory search depth.
    //Assumption: Search starts at '../instance.#/execution.#'
    private final static int maxSearchDepth = 2;

    //Constants For Comparator
    private final static String FILE_REGEXP = "part\\.(\\d+)\\.(log|txt)";
    private final static String DIR_REGEXP = "(\\d+)";
    private final static Pattern file_Pattern = Pattern.compile(FILE_REGEXP);
    private final static Pattern dir_Pattern = Pattern.compile(DIR_REGEXP);

    /**
     * Removed the RECURSION
     * Search the current and sub directories for all files that match the given FileFilter.
     *
     * @return all files beneath the given dir that match the given fileFilter.
     */
    public static List<File> findFiles(File dir, FileFilter fileFilter) {

        List<File> retMe = new ArrayList<File>();
        List<File> wrkLst;

        //Fail fast. File filter is required.
        if (fileFilter == null) {
            return retMe;
        }

        //Helper class
        class DirStackObj {
            List<File> d_lst;
            int index;

            public DirStackObj(List<File> f, int i) {
                d_lst = f;
                index = i;
            }
        }

        Stack<DirStackObj> dirLstStack = new Stack<DirStackObj>();

        //List is sorted files appended ahead of sorted directories.
        wrkLst = FileUtils.handleSort(dir);

        int i = 0;
        boolean skip = false;

        while (true) {
            while (i < wrkLst.size()) {
                skip = true;
                File file = wrkLst.get(i);
                if (fileFilter.accept(file)) {
                    retMe.add(file);
                    i++;

                    skip = false;
                }

                //Save state. Transition to sub-directory.
                if (file.isDirectory() && (dirLstStack.size() < FileUtils.maxSearchDepth)) {
                    dirLstStack.push(new DirStackObj(wrkLst, i));
                    File wrkDir = new File(file.getPath());

                    //Make sure list is sorted files appended ahead of sorted directories.
                    wrkLst = FileUtils.handleSort(wrkDir);
                    i = 0;
                    break;
                }

                //Not valid file or directory?? skip it.
                if (skip == true) {
                    i++;
                }
            }

            //Done with current directory, return to parent.
            if (i >= wrkLst.size()) {
                //All aggregation is complete.
                if (dirLstStack.empty()) {
                    break;
                } else {
                    //Restore parent directory listing.
                    DirStackObj dirObj = dirLstStack.pop();
                    wrkLst = dirObj.d_lst;

                    //Move to next directory.
                    i = ++dirObj.index;

                }
            }

        } //while(true)

        return retMe;
    }

    /**
     * Sort a directory listing of files and directories.
     * Sort files ahead of directories.
     *
     * No recursion is performed, we're just sorting the
     * immediate contents of the current directory.
     *
     * @return sorted files and then sorted directories. Returns an empty list if a non-directory is passed as input.
     */
    private static List<File> handleSort(File dir) {

        File[] wrk = dir.listFiles();
        if (wrk == null) {
            return new ArrayList<File>();
        }

        List<File> wrk_files = new ArrayList<File>();
        List<File> wrk_dirs = new ArrayList<File>();

        for (File f : wrk) {
            if (f.isFile()) {
                wrk_files.add(f);
            } else if (f.isDirectory()) {
                wrk_dirs.add(f);
            }
        }

        Collections.sort(wrk_files, NaturalComparator.instance);
        Collections.sort(wrk_dirs, NaturalComparator.instance);

        wrk_files.addAll(wrk_dirs);

        return wrk_files;
    }

    /**
     * Comparator for files named 'part.#.log|txt', and directories with numbers for names.
     * Sort by numeric value, where '#' is a number,
     * Directory names that are only numbers are also sorted by numeric value.
     *
     */
    public static class NaturalComparator implements Comparator<File> {
        public static final Comparator<File> instance = new NaturalComparator();

        private NaturalComparator() {}

        @Override
        public int compare(File f1, File f2) {

            String s1 = f1.getName();
            String s2 = f2.getName();

            //For directories that are only numbers and files named part.*.log|txt

            Matcher m1 = FileUtils.file_Pattern.matcher(s1);
            Matcher m2 = FileUtils.file_Pattern.matcher(s2);

            boolean bothMatch = false;

            if (m1.matches() && m2.matches()) {
                bothMatch = true;
            } else {
                m1 = FileUtils.dir_Pattern.matcher(s1);
                m2 = FileUtils.dir_Pattern.matcher(s2);
                if (m1.matches() && m2.matches()) {
                    bothMatch = true;
                }
            }

            if (bothMatch) {
                return Integer.parseInt(m1.group(1)) - Integer.parseInt(m2.group(1));
            }

            //For everything else
            return s1.compareTo(s2);

        }
    }

}