/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import java.io.Serializable;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PartitionPropertyAnalyzer extends AbstractPartitionAnalyzer {

    @Inject
    StepContext stepCtx;

    @Override
    public void analyzeCollectorData(Serializable data) throws Exception {
        stepCtx.setPersistentUserData(stepCtx.getPersistentUserData() + (String) data);
    }
}