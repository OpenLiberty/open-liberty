/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.io.File;
import java.io.IOException;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;

/**
 * This is very different from the Kernel definition list used at runtime.
 * This aggregates the SubsystemFeatureDefintionImpls (one per file),
 * so they can be printed as one section...
 */
public class KernelFeatureListDefinition extends SubsystemFeatureDefinitionImpl {

    /**
     * @param f
     * @throws IOException
     */
    public KernelFeatureListDefinition(File f) throws IOException {
        super(ExtensionConstants.CORE_EXTENSION, f);
    }

    @Override
    public boolean isKernel() {
        return true;
    }
}
