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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

public class ExecuteCRIU_OpenJ9 implements BundleActivator, ExecuteCRIU {

    @Override
    @FFDCIgnore(ClassNotFoundException.class)
    public void start(BundleContext bc) {
        try {
            Class.forName("org.eclipse.openj9.criu.CRIUSupport");
            bc.registerService(ExecuteCRIU.class, this, null);
        } catch (ClassNotFoundException e) {
            // do nothing; not on open j9 that supports CRIU
        }
    }

    @Override
    public void stop(BundleContext bc) {

    }

    @Override
    @FFDCIgnore({ JVMCheckpointException.class, SystemCheckpointException.class, RestoreException.class, JVMCRIUException.class, RuntimeException.class })
    public void dump(File imageDir, String logFileName, File workDir) throws CheckpointFailedException {
        if (!CRIUSupport.isCRIUSupportEnabled()) {
            // TODO log appropriate message
            System.out.println("Must set the JVM option: -XX:+EnableCRIUSupport");
            throw new CheckpointFailedException(Type.UNSUPPORTED, "Must set the JVM option: -XX:+EnableCRIUSupport", null, 50);
        }
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
    public boolean isCheckpointSupported() {
        return CRIUSupport.isCRIUSupportEnabled();
    }

}
