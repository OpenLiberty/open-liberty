/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.application;

import java.util.List;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.tasks.ApplicationTask;

public class InstallWrapper extends ApplicationOptions {

    private RemoteFile earFile;
    private ArchiveType originalArchiveType;

    protected InstallWrapper(RemoteFile earFile, List<ApplicationTask> tasks, Scope scope, ArchiveType archiveType) throws Exception {
        super(tasks, scope);
        this.earFile = earFile;
        this.originalArchiveType = archiveType;
    }

    public RemoteFile getEarFile() {
        return earFile;
    }

    public void setEarFile(RemoteFile earFile) {
        this.earFile = earFile;
    }

    public ArchiveType getOriginalArchiveType() {
        return this.originalArchiveType;
    }

    public boolean validate() throws Exception {
        return true;
    }

}
