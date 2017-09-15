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
package com.ibm.ws.appclient.boot.cmdline;

import com.ibm.ws.appclient.boot.ClientLauncher;
import com.ibm.ws.kernel.boot.cmdline.EnvCheck;

/**
 * Check's the version of the Java running before starting the client or running commands,
 * if Java 5 (or below) is being used a translated error message is thrown.
 */
public class ClientEnvCheck extends EnvCheck {

    /**
     * @param args - will just get passed onto Launcher if version check is successful
     */
    public static void main(String[] args) {
        EnvCheck.main(args, new ClientLauncher());
    }
}
