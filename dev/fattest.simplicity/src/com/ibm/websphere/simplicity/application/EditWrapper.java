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

import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.tasks.ApplicationTask;

public class EditWrapper extends ApplicationOptions {

    private Application application;
    private boolean appEdit;

    public EditWrapper(Application app, List<ApplicationTask> tasks, Scope cell) throws Exception {
        super(tasks, cell);
        this.application = app;
    }

    public Application getApplication() {
        return this.application;
    }

    public boolean isFullApplicationEdit() {
        return appEdit;
    }

}
