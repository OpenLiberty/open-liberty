/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.build;

import static com.ibm.ws.test.image.build.BuildProperties.*;
import static com.ibm.ws.test.image.util.FileUtils.*;
import static com.ibm.ws.test.image.util.ProcessRunner.runJar;

import java.io.File;
import java.util.List;

import com.ibm.ws.test.image.Timeouts;
import com.ibm.ws.test.image.util.FileUtils;

public class BuildProfiles {
    public static final String CLASS_NAME = BuildProfiles.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    // open-liberty/dev/build.image/profiles/
    //
    // javaee8
    // webProfile8
    // microProfile4
    //
    // jakartaee9
    // webProfile9
    // microProfile5
}
