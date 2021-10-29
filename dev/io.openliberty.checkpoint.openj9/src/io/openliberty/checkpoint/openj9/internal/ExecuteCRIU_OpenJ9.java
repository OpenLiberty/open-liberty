/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.openj9.internal;

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
    public void dump(File imageDir, String logFileName, File workDir) throws CheckpointFailedException {
        CRIUSupport criuSupport = new CRIUSupport(imageDir.toPath());
        criuSupport.setShellJob(true);
        criuSupport.setFileLocks(true);
        criuSupport.setLogFile(logFileName);
        criuSupport.setWorkDir(workDir.toPath());
        criuSupport.setTCPEstablished(true);
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
            throw new CheckpointFailedException(Type.UNKNOWN, e.getMessage(), e, 60);
        }
    }

    @Override
    public void checkpointSupported() {
        //This service implementation is only registered after support for CRIU is confirmed
        // so do nothing here.
    }
}
