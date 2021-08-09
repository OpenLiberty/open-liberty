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
package com.ibm.ws.filetransfer.internal.mbean;

import java.io.IOException;
import java.util.List;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.filetransfer.FileTransferMBean;

/**
 * This MBean is registered on the MBeanServer but it is only allowed to be accessed through the JMX REST Connector. All other
 * accesses will trigger an exception.
 */
@Component(immediate = true,
           property = "jmx.objectname=" + FileTransferMBean.OBJECT_NAME)
public class FileTransfer extends StandardMBean implements FileTransferMBean {

    public FileTransfer() throws NotCompliantMBeanException {
        super(FileTransferMBean.class, false);
    }

    /** {@inheritDoc} */
    @Override
    public void downloadFile(String remoteSourceFile, String localTargetFile) throws IOException {
        throw createException("downloadFile(String remoteSourceFile, String localTargetFile)");
    }

    /** {@inheritDoc} */
    @Override
    public long downloadFile(String remoteSourceFile, String localTargetFile, long startOffset, long endOffset) throws IOException {
        throw createException("downloadFile(String remoteSourceFile, String localTargetFile, long startOffset, long endOffset)");
    }

    /** {@inheritDoc} */
    @Override
    public void uploadFile(String localSourceFile, String remoteTargetFile, boolean expandOnCompletion) throws IOException {
        throw createException("uploadFile");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteFile(String remoteSourceFile) throws IOException {
        throw createException("deleteFile");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAll(List<String> remoteArtifacts) throws IOException {
        throw createException("deleteAll");
    }

    private IOException createException(String methodName) {
        String param = FileTransferMBean.class.getName() + "#" + methodName;
        Object[] params = new String[] { param };
        return new IOException(TraceNLS.getFormattedMessage(
                                                            this.getClass(),
                                                            TraceConstants.MESSAGE_BUNDLE,
                                                            "UNSUPPORTED_MBEAN_OPERATION_ERROR",
                                                            params,
                                                            "CWWKX7910W: Operation " + param + " is only avaible when this MBean is accessed through IBM's JMX REST Connector."));
    }

    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        //Using switches/conditions so that we always default to the same thing (pN) in unexpected scenarios
        if ("downloadFile".equals(op.getName())) {
            switch (sequence) {
                case 0:
                    return "remoteSourceFile";
                case 1:
                    return "localTargetFile";
            }
        } else if ("uploadFile".equals(op.getName())) {
            switch (sequence) {
                case 0:
                    return "localSourceFile";
                case 1:
                    return "remoteTargetFile";
                case 2:
                    return "expandOnCompletion";
            }
        } else if ("deleteFile".equals(op.getName())) {
            if (sequence == 0) {
                return "remoteSourceFile";
            }
        }

        return "p" + sequence;
    }

}
