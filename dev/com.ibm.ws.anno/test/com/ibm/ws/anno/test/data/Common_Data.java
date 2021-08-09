/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Common_Data {
    public static String putIntoPath(String rootPath, String childPath) {
        return rootPath + File.separator + childPath;
    }

    public static String putIntoPath(String projectPath, String dataPath, String path) {
        return Common_Data.putIntoPath(projectPath, Common_Data.putIntoPath(dataPath, path));
    }

    public static List<String> putIntoPath(String rootPath, List<String> childPaths) {
        List<String> adjustedPaths = new ArrayList<String>();

        for (String nextChildPath : childPaths) {
            adjustedPaths.add(Common_Data.putIntoPath(rootPath, nextChildPath));
        }

        return adjustedPaths;
    }

    public static List<String> putInPath(String projectPath, String dataPath, List<String> paths) {
        List<String> adjustedPaths = new ArrayList<String>();

        for (String nextPath : paths) {
            adjustedPaths.add(Common_Data.putIntoPath(projectPath, dataPath, nextPath));
        }

        return adjustedPaths;
    }
}
