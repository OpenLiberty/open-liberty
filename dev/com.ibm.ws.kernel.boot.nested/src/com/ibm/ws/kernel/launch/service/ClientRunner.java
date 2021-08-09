/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.service;

public interface ClientRunner {
    /**
     * A registered service of this interface will run application's main() method in a client module. If no ClientRunner is found
     * after the framework is ready (see {@link FrameworkReady}), then an error will be issued, and the client process will exit.
     * 
     * If an exception occurs while a registered service is executing main(), ReturnCode.CLIENT_RUNNER_EXCEPTION, or (int) 35
     * is returned as the exit code.
     */
    void run();
}
