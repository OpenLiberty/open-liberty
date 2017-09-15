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
package com.ibm.websphere.filetransfer;

import java.io.IOException;
import java.util.List;

/**
 * This MBean exposes remote file transfer capabilities and must be accessed
 * <b>only</b> within IBM's JMX REST Connector. Accessing this MBean by any
 * other means will result in a {@link java.lang.UnsupportedOperationException}.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * <p>
 * For the remote file parameters (ie: remoteSourceFile for download/delete
 * and remoteTargetFile for upload) the following characteristics apply:
 * <ul>
 * <li> the remote file will be either on the connected host or on the routing host (if one is setup).
 * <li> all file paths need to be either absolute or prepend a Liberty-defined symbol (described on &lt;wlp&gt;/README.txt) that resolves to an absolute path.
 * <li> all read and write operations need to be within the configured (or defaulted) read/write regions. See {@link com.ibm.websphere.filetransfer.FileServiceMXBean} for details.
 * </ul>
 * <p>
 * For the local file parameters (ie: localTargetFile for download and and
 * localSourceFile for upload) the following characteristics apply:
 * <ul>
 * <li> the local file will be in a folder that contains the appropriate read/write permissions.
 * <li> the file path is either absolute or relative to the current working directory.
 * </ul>
 *
 * @ibm-api
 */
public interface FileTransferMBean {

    /**
     * A string representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=restConnector,type=FileTransfer,name=FileTransfer";

    /**
     * Download a file from the specified remote source location and write it
     * in the specified local target location.
     * <p>
     * Directories are not supported as the remote source file. To download a
     * directory, it must first be archived.
     *
     * @param remoteSourceFile the remote file location of the source to download
     * @param localTargetFile the local file location where the source contents will be written
     * @throws IOException if there are any issues handling the source or target files
     */
    void downloadFile(String remoteSourceFile, String localTargetFile) throws IOException;

    /**
     * Download part of a file from the specified remote source location using the specified start and
     * end offset byte values and write it in the specified local target location.
     * <p>
     * Directories are not supported as the remote source file. To download a
     * directory, it must first be archived.
     * <p>
     * This partial download feature is currently available only in non-routing scenarios.
     *
     * @param remoteSourceFile the remote file location of the source to download
     * @param localTargetFile the local file location where the source contents will be written
     * @param startOffset index of the first byte to copy (zero-based and inclusive)
     * @param endOffset index of the last byte to copy (zero-based and inclusive).
     *            Specify -1 to copy until the end of file.
     *
     * @return Index to use as start offset for next partial file download request.
     *         If this value is greater than the <b>startOffset</b> value that was passed in, then
     *         some bytes (equivalent to the difference) were transferred by this download request.
     *         <br>
     *         <strong>Note</strong>: It is assumed that in-between the download requests, remote source file is not
     *         modified in any way except for appending contents to end of the file.
     *
     * @throws IOException if there are any issues handling the source or target files
     */
    long downloadFile(String remoteSourceFile, String localTargetFile, long startOffset, long endOffset) throws IOException;

    /**
     * Upload a file from the specified local source location and write it
     * in the specified remote target location.
     * <p>
     * This method optionally supports expanding an archive (specified as the local
     * source file) to the remote target file. The supported compression
     * formats are 'zip' and 'jar' (including war and ear) . All other format types will result in
     * undefined behaviour.
     * <p>
     * Directories are not supported as the local source file. To upload a
     * directory, it must first be archived and can then be expanded during upload using the expandOnCompletion option.
     * This option has a special behaviour: the archive will be uploaded as a regular file to the remote system, and then we
     * will make a directory that matches the filename specified by remoteTargetFile, and the contents will be expanded inside that new folder.
     *
     * <p>Example: Uploading an archive with remoteTargetFile /home/myFolder/wlp.zip and the expandOnCompletion
     * option set to true will upload the archive wlp.zip to the remote file system, create a folder called wlp.zip
     * under the directory /home/myFolder, and then expand the contents of the archive inside the folder /home/myFolder/wlp.zip.
     *
     * If expandOnCompletion flag is false,then remoteTargetFile must match a filename with an extension (unless the file has not extension), and
     * cannot be a folder.
     *
     *
     * @param localSourceFile the local path to the file that will be uploaded.
     *            The source file must be a file, since directories are not supported.
     * @param remoteTargetFile the remote path of the uploaded file.
     *            The target file must be a file, since directories are not supported.
     * @param expandOnCompletion indicates if the archive should be expanded automatically after it is uploaded.
     * @throws IOException if there are any issues handling the request
     */
    void uploadFile(String localSourceFile, String remoteTargetFile, boolean expandOnCompletion) throws IOException;

    /**
     * Delete a file located at the remote specified location.
     * <p>
     * Recursive deletion of a directory is not supported, however deletion of
     * an empty directory is supported.
     *
     * @param remoteSourceFile the location of the remote file to be deleted
     * @throws IOException if there are any issues handling the request
     */
    void deleteFile(String remoteSourceFile) throws IOException;

    /**
     * Delete files and folders (empty and non-empty) at the remote specified locations
     *
     * @param remoteArtifacts list of locations of remote files and folders (empty and non-empty) to delete
     * @throws IOException if there are any issues handling the request
     */
    void deleteAll(List<String> remoteArtifacts) throws IOException;

}
