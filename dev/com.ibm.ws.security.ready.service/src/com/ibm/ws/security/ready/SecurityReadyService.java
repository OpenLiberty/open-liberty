/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.ready;

import java.util.concurrent.CountDownLatch;

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
     * Provides a CountDownLatch that can be used to wait for the
     * security service as a whole to be ready
     *
     * @return CountDownLatch that countdowns when security service is ready to
     *         process requests
     */
    public CountDownLatch getSecurityReadyCDL();

}
