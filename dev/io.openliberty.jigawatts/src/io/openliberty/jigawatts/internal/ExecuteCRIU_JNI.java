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

import io.openliberty.checkpoint.internal.criu.ExecuteCRIU;

// let other implementations win by using low ranking
@Component(property = "service.ranking:Integer=-100")
public class ExecuteCRIU_JNI implements ExecuteCRIU {

    @Override
    public int dump(File directory, String logFileName, File workDir) throws IOException {
        return Jigawatts.saveTheWorld(directory.getAbsolutePath());
    }
    
    @Override
    public boolean isCheckpointSupported() {
        return true;
    }

}
