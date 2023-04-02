/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.ready;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public interface SecurityReadyService {
    /**
     * Answers if the security service as a whole is ready to process requests.
     *
     * @return boolean indiciating if the security service is ready to process
     *         requests.
     */
    public boolean isSecurityReady();

    /**
     * Provides a method that can be used to wait for the
     * security service as a whole to be ready
     *
     * @return boolean, true if wait was successful, false if time limit was reached
     * @throws InterruptedException
     */
    public boolean awaitSecurityReady(long timeout, TimeUnit unit) throws InterruptedException;

}
