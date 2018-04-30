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
package com.ibm.ws.kernel.server.internal;

import java.io.File;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.kernel.server.ServerActionsMXBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.Commands;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

@Component(service = { ServerActionsMXBean.class, DynamicMBean.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM",
                        "jmx.objectname=" + ServerActionsMXBean.OBJECT_NAME })
public class ServerActionsMXBeanImpl extends StandardMBean implements ServerActionsMXBean {
    private static final TraceComponent tc = Tr.register(ServerActionsMXBeanImpl.class);

    private WsLocationAdmin locAdmin;

    public ServerActionsMXBeanImpl() throws NotCompliantMBeanException {
        super(ServerActionsMXBean.class);
    }

    @Reference(service = WsLocationAdmin.class)
    protected void setWsLocationAdmin(WsLocationAdmin ref) {
        this.locAdmin = ref;
    }

    protected void unsetWsLocationAdmin(WsLocationAdmin ref) {
        if (this.locAdmin == ref) {
            this.locAdmin = null;
        }
    }

    enum DumpType {
        ThreadDump, HeapDump, SystemDump
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String threadDump() {
        return threadDump(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String threadDump(String targetDirectory) {
        return threadDump(targetDirectory, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String threadDump(String targetDirectory, String nameToken) {
        return threadDump(targetDirectory, nameToken, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String threadDump(String targetDirectory, String nameToken, int maximum) {
        return performDump(DumpType.ThreadDump, targetDirectory, nameToken, maximum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String heapDump() {
        return heapDump(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String heapDump(String targetDirectory) {
        return heapDump(targetDirectory, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String heapDump(String targetDirectory, String nameToken) {
        return heapDump(targetDirectory, nameToken, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String heapDump(String targetDirectory, String nameToken, int maximum) {
        return performDump(DumpType.HeapDump, targetDirectory, nameToken, maximum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String systemDump() {
        return systemDump(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String systemDump(String targetDirectory) {
        return systemDump(targetDirectory, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String systemDump(String targetDirectory, String nameToken) {
        return systemDump(targetDirectory, nameToken, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String systemDump(String targetDirectory, String nameToken, int maximum) {
        return performDump(DumpType.SystemDump, targetDirectory, nameToken, maximum);
    }

    /**
     * Perform the actual dump.
     *
     * @param dumpType The type of dump
     * @param targetDirectory Target directory
     * @param nameToken Name token
     * @param maximum Maximum of these dumps
     * @return Path to dump if created
     */
    private String performDump(DumpType dumpType, String targetDirectory, String nameToken, int maximum) {
        if (targetDirectory != null) {
            targetDirectory = locAdmin.resolveString(targetDirectory);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Directory after resolving", targetDirectory);
            }
        }

        File targetDirectoryFile = null;

        if (targetDirectory != null) {
            targetDirectoryFile = new File(targetDirectory);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Absolute path", targetDirectoryFile.getAbsolutePath());
            }

            if (!targetDirectoryFile.exists()) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "invalidActionDirectory", targetDirectoryFile.getAbsolutePath()));
            }
        }

        File result = null;

        switch (dumpType) {
            case ThreadDump:
                result = Commands.threadDump(targetDirectoryFile, nameToken, maximum);
                break;
            case HeapDump:
                result = Commands.heapDump(targetDirectoryFile, nameToken, maximum);
                break;
            case SystemDump:
                result = Commands.systemDump(targetDirectoryFile, nameToken, maximum);
                break;
            default:
                throw new RuntimeException("Not implemented");
        }

        return result == null ? null : result.getAbsolutePath();
    }
}
