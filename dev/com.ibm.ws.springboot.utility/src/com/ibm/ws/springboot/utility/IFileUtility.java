/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.utility;

import java.io.File;
import java.io.PrintStream;

public interface IFileUtility {
    boolean isFile(String path);

    boolean mkDirs(File path, PrintStream stdout);

    boolean isDirectory(String path);
}
