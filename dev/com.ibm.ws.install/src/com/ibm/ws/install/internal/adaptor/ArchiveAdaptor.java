/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.internal.adaptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.internal.InstallUtils.FileWriter;
import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;

public class ArchiveAdaptor {

    static final String WLP_USR = "wlp/usr";

    static boolean write(boolean tmpFile, List<File> installedFiles, File fileToWrite, InputStream inputStream, ExistsAction existsAction,
                         String fileToWriteChecksum)
                    throws IOException, InstallException {
        return write(tmpFile, installedFiles, fileToWrite, null, new InputStreamFileWriter(inputStream), existsAction, fileToWriteChecksum);
    }

    static boolean write(boolean tmpFile, List<File> installedFiles, File fileToWrite, String toFileEncoding, FileWriter fileWriter,
                         ExistsAction existsAction, String fileToWriteChecksum)
                    throws IOException, InstallException {

        if (!!!tmpFile && fileToWrite.exists() && existsAction != ExistsAction.replace) {
            /* The file already exists, do we need to fail? Not if it only exists because we have already installed it (i.e. through a dependency to another feature) */
            if (existsAction == ExistsAction.fail) {
                if (installedFiles.contains(fileToWrite) || InstallUtils.isFileSame(fileToWrite, null, null, fileToWriteChecksum)) {
                    // It was us that put it there so that's ok!
                    return false;
                } else {
                    throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.file.exists", fileToWrite));
                }
            } else if (existsAction == ExistsAction.ignore) {
                Logger.getLogger(InstallConstants.LOGGER_NAME).log(Level.FINEST, fileToWrite.getAbsolutePath() + " was not installed because it already exists.");
                return false;
            }
        }

        if (InstallUtils.mkdirs(installedFiles, fileToWrite.getParentFile())) {
            if (installedFiles != null) {
                installedFiles.add(fileToWrite);
            }

            // Call the implementation to write to the file
            fileWriter.writeToFile(fileToWrite, toFileEncoding);
        } else {
            throw new InstallException(Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.dir.create.fail", fileToWrite.getParentFile()), InstallException.IO_FAILURE);
        }

        return true;
    }
}
