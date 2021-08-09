/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Util for the dump command
 */
public class ServerDumpUtil {

    /**
     *  In general find another way to do what you are trying, this is meant as a 
     *  VERY VERY last resort and agreed to by Gary.
     */
    public static boolean isZos() {

        String os = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("os.name");
            }
        });

        return os != null && (os.equalsIgnoreCase("OS/390") || os.equalsIgnoreCase("z/OS"));
    }
}
