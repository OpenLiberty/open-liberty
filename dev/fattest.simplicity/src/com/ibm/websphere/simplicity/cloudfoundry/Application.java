/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.cloudfoundry;

import java.io.File;

public final class Application {

    private final String name;
    private final String directory;

    public Application(String appName, String appDir) {
        this.name = appName;
        this.directory = appDir;
    }

    public String getName() {
        return this.name;
    }

    public String getDirectory() {
        return this.directory;
    }

    public File getApplicationPath() {
        String absolutePath = System.getProperty("user.dir") + "/build/push";
        File appLocation = new File(absolutePath, this.directory);
        return appLocation;
    }

}
