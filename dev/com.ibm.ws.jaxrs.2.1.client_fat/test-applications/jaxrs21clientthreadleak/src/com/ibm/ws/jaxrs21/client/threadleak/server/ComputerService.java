/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.server;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Service layer for computer operations.
 * This is the business logic layer that calls the API client.
 */
@ApplicationScoped
public class ComputerService {

    @Inject
    private ComputerApiRestClient client;

    /**
     * Get student data from remote computer.
     * This method is called for each student with a different IP address.
     */
    public void getStudentData(String id, String ip, int port) {
        
        Student student = client.getStudentData(ip, port);
    }
}

