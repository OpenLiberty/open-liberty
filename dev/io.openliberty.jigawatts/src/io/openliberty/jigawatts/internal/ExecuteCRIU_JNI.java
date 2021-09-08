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
package io.openliberty.jigawatts.internal;

import java.io.File;
import java.io.IOException;

import org.openjdk.jigawatts.Jigawatts;
import org.osgi.service.component.annotations.Component;

import io.openliberty.checkpoint.internal.criu.CheckpointFailedException;
import io.openliberty.checkpoint.internal.criu.CheckpointFailedException.Type;
import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

// let other implementations win by using low ranking
@Component(property = "service.ranking:Integer=-100")
public class ExecuteCRIU_JNI implements ExecuteCRIU {

    @Override
    public void dump(File directory, String logFileName, File workDir) throws CheckpointFailedException {
    	try {
    		int result = Jigawatts.saveTheWorld(directory.getAbsolutePath());
    		if (result != 0) {
    			throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, "CRIU checkpoint saveTheWorld faild", null, result);
    		}
    	} catch (IOException e) {
    		throw new CheckpointFailedException(Type.SYSTEM_CHECKPOINT_FAILED, e.getMessage(), e, 50);
    	}
    }
    
    @Override
    public boolean isCheckpointSupported() {
        return true;
    }

}
