/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal.openj9;

import java.io.File;

import org.eclipse.openj9.criu.CRIUSupport;
import org.eclipse.openj9.criu.JVMCRIUException;
import org.eclipse.openj9.criu.JVMCheckpointException;
import org.eclipse.openj9.criu.RestoreException;
import org.eclipse.openj9.criu.SystemCheckpointException;

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
    @FFDCIgnore({ JVMCheckpointException.class, SystemCheckpointException.class, RestoreException.class, JVMCRIUException.class, RuntimeException.class })
    public void dump(Runnable prepare, Runnable restore, File imageDir, String logFileName, File workDir, File envProps, boolean unprivileged) throws CheckpointFailedException {
        CRIUSupport criuSupport = new CRIUSupport(imageDir.toPath());
        criuSupport.registerPreSnapshotHook(prepare);
        criuSupport.registerPostRestoreHook(restore);
        criuSupport.setShellJob(true);
        criuSupport.setFileLocks(true);
        criuSupport.setLogFile(logFileName);
        criuSupport.setWorkDir(workDir.toPath());
        criuSupport.setTCPEstablished(true);
        criuSupport.registerRestoreEnvFile(envProps.toPath());
        setCheckpointLogLevel(criuSupport);
        if (unprivileged) {
            try {
                criuSupport.setUnprivileged(true);
            } catch (NoSuchMethodError e) {
                // TODO this is a temporary message that will never happen on a released version of Open J9.  Not adding message to nlsprops
                throw new CheckpointFailedException(Type.UNKNOWN_CHECKPOINT, "JVM does not support CRIU unprivileged mode", e);
            }
        }
        try {
            criuSupport.checkpointJVM();
        } catch (JVMCheckpointException e) {
            throw new CheckpointFailedException(Type.JVM_CHECKPOINT_FAILED, e.getMessage(), e);
        } catch (SystemCheckpointException e) {
            throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, e.getMessage(), e);
        } catch (RestoreException e) {
            throw new CheckpointFailedException(Type.JVM_RESTORE_FAILED, e.getMessage(), e);
        } catch (JVMCRIUException e) {
            throw new CheckpointFailedException(checkpointImpl.getUnknownType(), e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new CheckpointFailedException(checkpointImpl.getUnknownType(), e.getMessage(), e);
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
