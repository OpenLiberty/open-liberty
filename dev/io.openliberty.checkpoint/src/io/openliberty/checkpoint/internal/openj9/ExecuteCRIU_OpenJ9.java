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

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class ExecuteCRIU_OpenJ9 implements ExecuteCRIU {

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
        if (unprivileged) {
            try {
                criuSupport.setUnprivileged(true);
            } catch (NoSuchMethodError e) {
                throw new CheckpointFailedException(Type.UNKNOWN, "JVM does not support CRIU unprivileged mode", e);
            }
        }
        try {
            criuSupport.checkpointJVM();
        } catch (JVMCheckpointException e) {
            throw new CheckpointFailedException(Type.JVM_CHECKPOINT_FAILED, e.getMessage(), e, e.getErrorCode());
        } catch (SystemCheckpointException e) {
            throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, e.getMessage(), e, e.getErrorCode());
        } catch (RestoreException e) {
            throw new CheckpointFailedException(Type.JVM_RESTORE_FAILED, e.getMessage(), e, e.getErrorCode());
        } catch (JVMCRIUException e) {
            throw new CheckpointFailedException(Type.UNKNOWN, e.getMessage(), e, e.getErrorCode());
        } catch (RuntimeException e) {
            throw new CheckpointFailedException(Type.UNKNOWN, e.getMessage(), e);
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
