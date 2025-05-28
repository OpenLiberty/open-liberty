/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal.openj9;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.openj9.criu.CRIUSupport;
import org.eclipse.openj9.criu.JVMCRIUException;
import org.eclipse.openj9.criu.JVMCheckpointException;
import org.eclipse.openj9.criu.JVMRestoreException;
import org.eclipse.openj9.criu.SystemCheckpointException;
import org.eclipse.openj9.criu.SystemRestoreException;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.CheckpointImpl;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class ExecuteCRIU_OpenJ9 implements ExecuteCRIU {
    private final CheckpointImpl checkpointImpl;

    public ExecuteCRIU_OpenJ9(CheckpointImpl checkpointImpl) {
        this.checkpointImpl = checkpointImpl;
    }

    @Override
    @FFDCIgnore({ JVMCheckpointException.class, SystemCheckpointException.class, JVMRestoreException.class, SystemRestoreException.class, JVMCRIUException.class })
    public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps, boolean unprivileged) throws CheckpointFailedException {
        CRIUSupport criuSupport = getCRIUSupport(imageDir.toPath());

        criuSupport.registerPreCheckpointHook(prepare);
        criuSupport.registerPostRestoreHook(restore);
        criuSupport.setShellJob(true);
        criuSupport.setFileLocks(true);
        criuSupport.setLogFile(logFileName);
        criuSupport.setWorkDir(workDir.toPath());
        criuSupport.setTCPEstablished(true);
        criuSupport.registerRestoreEnvFile(envProps.toPath());
        setCheckpointLogLevel(criuSupport);
        criuSupport.setUnprivileged(unprivileged);

        try {
            criuSupport.checkpointJVM();
        } catch (JVMCheckpointException e) {
            throw new CheckpointFailedException(Type.JVM_CHECKPOINT_FAILED, e.getMessage(), e);
        } catch (SystemCheckpointException e) {
            throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, e.getMessage(), e);
        } catch (JVMRestoreException e) {
            throw new CheckpointFailedException(Type.JVM_RESTORE_FAILED, e.getMessage(), e);
        } catch (SystemRestoreException e) {
            throw new CheckpointFailedException(Type.SYSTEM_RESTORE_FAILED, e.getMessage(), e);
        } catch (JVMCRIUException e) {
            throw new CheckpointFailedException(checkpointImpl.getUnknownType(), e.getMessage(), e);
        }
    }

    @FFDCIgnore(Throwable.class)
    private CRIUSupport getCRIUSupport(Path path) {
        try {
            // first try the new static method
            return CRIUSupport.getCRIUSupport().setImageDir(path);
        } catch (Throwable t) {
            // fall back to old way
            return new CRIUSupport(path);
        }
    }

    /**
     * @param criuSupport
     */
    private void setCheckpointLogLevel(CRIUSupport criuSupport) {
        String logLevelProp = System.getProperty("io.openliberty.checkpoint.criu.loglevel");
        if (logLevelProp != null) {
            int logLevel = Integer.valueOf(logLevelProp);
            criuSupport.setLogLevel(logLevel);
        }
    }

    @Override
    public void checkpointSupported() throws CheckpointFailedException {
        if (CRIUSupport.isCRIUSupportEnabled()) {
            return;
        }
        throw new CheckpointFailedException(Type.UNSUPPORTED_DISABLED_IN_JVM, CRIUSupport.getErrorMessage(), null);
    }
}
