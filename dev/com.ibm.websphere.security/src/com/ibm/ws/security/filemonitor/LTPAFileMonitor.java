/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.security.filemonitor;

import java.io.File;
import java.util.Collection;

/**
 * The LTPA file monitor gets notified through the scanComplete method
 * of the creation, modification, or deletion of the file(s) being monitored.
 * It will tell the actionable to perform its action if an action is needed.
 */
public class LTPAFileMonitor extends SecurityFileMonitor {

    /**
     * @param fileBasedActionable
     */
    public LTPAFileMonitor(FileBasedActionable fileBasedActionable) {
        super(fileBasedActionable);
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {
    }

    /** {@inheritDoc} */
    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        actionable.performFileBasedAction(createdFiles, modifiedFiles, deletedFiles);
    }
}
