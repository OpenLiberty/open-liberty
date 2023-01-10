/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.artifacts;

import java.io.Serializable;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class PartitionIDAnalyzer extends AbstractPartitionAnalyzer {

    @Inject
    JobContext jobCtx;

    @Override
    public void analyzeCollectorData(Serializable data) throws Exception {
        jobCtx.setExitStatus(jobCtx.getExitStatus() + data);
    }

}