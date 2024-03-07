/*******************************************************************************
 * Copyright (c) 2016,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.util;

import java.io.File;
import java.io.FilenameFilter;

public class ScriptFilter implements FilenameFilter {
    public static String[] list(boolean isWindows, String path) {
        return new File(path).list( new ScriptFilter(isWindows) );
    }

    public static String[] list(String path) {
        return new File(path).list( new ScriptFilter() );
    }

    //

    public ScriptFilter() {
        this( FileUtils.IS_WINDOWS );
    }
    
    public ScriptFilter(boolean isWindows) {
        this.isWindows = isWindows;
    }

    private final boolean isWindows;

    public boolean isWindows() {
        return isWindows;
    }

    @Override
    public boolean accept(File dir, String name) {
        File file = new File(dir, name);
        return ( (name.endsWith(".bat") == isWindows()) && file.isFile() );
    }
}
