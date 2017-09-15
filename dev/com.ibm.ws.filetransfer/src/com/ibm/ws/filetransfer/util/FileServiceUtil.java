/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.filetransfer.util;

/**
 * File Transfer archive utilities
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.filetransfer.routing.archiveExpander.ArchiveExpander;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class FileServiceUtil {

    private static final TraceComponent tc = Tr.register(FileServiceUtil.class);
    private String _parentOfOriginalSourcePath = null;

    /**
     * Archive the specified sourcePath entity to the specified targetPath location.
     * If sourcePath is a file, then it is archived to the target location.
     * If sourcePath is a directory, then all subordinate files and directories are archived to the target location.
     * <p>
     * 
     * @param sourcePath entity to be archived
     * @param targetPath location where srcPath is archived
     *            <p>
     * @returns true if the operation is successful, false otherwise
     */
    public boolean createArchive(String sourcePath, String targetPath) {

        try {

            // make sure we're working with absolute canonical paths
            File source = new File(sourcePath).getCanonicalFile();

            // check for the existence of source
            if (!FileUtils.fileExists(source)) {
                return false;
            }

            File target = new File(targetPath).getCanonicalFile();

            // determine parent of source path
            _parentOfOriginalSourcePath = source.getParent();

            if (_parentOfOriginalSourcePath.endsWith(File.separator)) {

                // parent of source path is the root - so we need to truncate the trailing slash
                // for (later) relative path construction.
                _parentOfOriginalSourcePath = _parentOfOriginalSourcePath.substring(0, _parentOfOriginalSourcePath.length() - 1);
            }

            // make sure parent dir of target archive exists
            FileUtils.fileMkDirs(target.getParentFile());

            // if target already exists, we'll just overwrite
            createNewFile(target);

            // Create the ZIP file
            ZipOutputStream out = new ZipOutputStream(getOutputStream(target));

            if (FileUtils.fileIsFile(source)) {

                archiveFile(source, out);

            } else if (FileUtils.fileIsDirectory(source)) {

                archiveDirectory(source, out);
            }

            // Complete the ZIP file
            out.close();

        } catch (Exception e) {

            FFDCFilter.processException(e, getClass().getName(), "createArchive");
            return false;
        }

        return true;
    }

    private void archiveFile(File src, ZipOutputStream out) throws Exception {

        byte[] buf = new byte[1024];

        FileInputStream in = (FileInputStream) FileUtils.getInputStream(src);

        // construct the relative path for the zipEntry (ie. relative to the specified sourcePath)
        String relativePath = src.getPath().substring(_parentOfOriginalSourcePath.length() + 1);

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(relativePath));

        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0) {

            out.write(buf, 0, len);
        }

        // Complete the entry
        out.closeEntry();

        // close archived file
        in.close();
    }

    private void archiveDirectory(File src, ZipOutputStream out) throws Exception {

        String[] entities = FileUtils.list(src);

        for (int i = 0; i < entities.length; i++) {

            File entity = new File(src, entities[i]);

            if (FileUtils.fileIsFile(entity)) {

                archiveFile(entity, out);

            } else if (FileUtils.fileIsDirectory(entity)) {

                archiveDirectory(entity, out);
            }
        }
    }

    /**
     * Expand the specified archive to the specified location
     * <p>
     * 
     * @param sourcePath path of the archive to be expanded.
     * @param targetPath location to where the archive is to be expanded.
     *            <p>
     * @returns true if the archive was successfully expanded, false otherwise.
     */
    public boolean expandArchive(String sourcePath, String targetPath) {
        try {
            return ArchiveExpander.coreExpandArchive(sourcePath, targetPath);
        } catch (IOException e) {
            FFDCFilter.processException(e, getClass().getName(), "expandArchive");
        }
        return false;
    }

    private OutputStream getOutputStream(final File target) throws FileNotFoundException {

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<OutputStream>() {
                @Override
                public OutputStream run() throws FileNotFoundException {
                    return new FileOutputStream(target);
                }
            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof FileNotFoundException)
                throw (FileNotFoundException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        }
    }

    private boolean createNewFile(final File target) throws IOException {

        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws IOException {
                    return target.createNewFile();
                }
            });
        } catch (PrivilegedActionException e) {
            Exception e2 = e.getException();
            if (e2 instanceof IOException)
                throw (IOException) e2;
            if (e2 instanceof RuntimeException)
                throw (RuntimeException) e2;
            throw new UndeclaredThrowableException(e);
        }
    }

    private static boolean isWindowsRootDirectory(String path) {
        if (path.length() == 3 &&
            (isAlpha(path.charAt(0)) &&
             (path.charAt(1) == ':') &&
            (path.charAt(2) == '/'))) {
            return true;
        }

        return false;
    }

    @Trivial
    private static final boolean isAlpha(char c) {
        return isLowerAlpha(c) || isUpperAlpha(c);
    }

    @Trivial
    private static final boolean isLowerAlpha(char c) {
        return c >= 'a' && c <= 'z';
    }

    @Trivial
    private static final boolean isUpperAlpha(char c) {
        return c >= 'A' && c <= 'Z';
    }

    /**
     * Returns true if the targetPath is contained within one of the allowedPaths.
     * 
     * Assumption: the "allowedPaths" point to directories, and the "targetPath" is already normalized.
     */
    public static boolean isPathContained(List<String> allowedPaths, String targetPath) {

        if (allowedPaths == null || allowedPaths.isEmpty() || targetPath == null) {
            return false;
        }

        //Remove trailing slashes, if applicable
        if (!targetPath.isEmpty() &&
            targetPath.charAt(targetPath.length() - 1) == '/' &&
            targetPath.length() > 1 &&
            !isWindowsRootDirectory(targetPath)) {
            targetPath = targetPath.substring(0, targetPath.length() - 1);
        }

        while (targetPath != null) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Target path: " + targetPath);
            }

            for (int i = 0; i < allowedPaths.size(); i++) {

                //String allowedPath = it.next();
                String allowedPath = allowedPaths.get(i);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Checking path: " + allowedPath);
                }

                //When we have a configuration that explicitly sets an empty read or write list, then we get a non-empty set
                //with a single empty string.  So we must catch empty cases here, otherwise the comparisons below might be incorrect.
                if ("".equals(allowedPath)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Skipping an empty path");
                    }

                    continue;
                }

                //We are always doing case-insensitive comparisons because we can't reliably find out
                //if the windows machine has changed its registry key to allow sensitivity, nor do we
                //know if the remote host is a certain OS.  For the purposes of this method (checking for access
                //to a directory) it is safe to assume case insensitivity.
                if (allowedPath.equalsIgnoreCase(targetPath)) {
                    return true;
                }

            }

            // We'll check 'up' the target path's parent chain to see if that's
            // covered by the allowed paths.
            targetPath = getNormalizedParent(targetPath);
        }

        return false;
    }

    /*
     * Returns the normalized parent of the specified path.
     * If a parent doesn't exist, return null.
     * 
     * Assumption: path is normalized.
     */
    private static String getNormalizedParent(String filePath) {
        if (new File(filePath).getParent() == null) {
            // parent doesn't exist .. return null
            return null;
        }

        String parentDir = filePath.substring(0, filePath.lastIndexOf("/"));

        if (!parentDir.contains("/")) {
            //catch cases where filePath is something like C:/temp.zip or /home.zip
            parentDir = parentDir + "/";
        }

        return parentDir;
    }

}
