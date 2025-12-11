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
 * API client for Boordcomputer (onboard computer) service.
 * This class creates VARYING URLs for each request, which is the KEY to reproducing the thread leak.
 * 
 * In the customer's scenario:
 * - Each vehicle has a different IP address/port
 * - Each request calls setBoordcomputerServiceHost() with a different IP
 * - This causes getServiceHost() to return a different URL each time
 * - Different URLs -> Different Bus instances in CXF -> Thread leak with old code
 */
@ApplicationScoped
public class ComputerApiRestClient extends ApiRestClient {

    @Inject
    @ComputerRestClient
    private RestClient restClient;

    /**
     * The service host is dynamic, it's the IP address of the onboard computer.
     * This must be set correctly before executing a request.
     */
    private String computerServiceHost;

    /**
     * Set the service host dynamically based on vehicle IP and port.
     * This is called before each request with a DIFFERENT IP/port combination.
     * This is what causes the thread leak - each unique URL creates a new Bus in CXF.
     */
    public void setComputerServiceHost(String ip, int port) {
        this.computerServiceHost = String.format("http://%s:%s", ip, port);
    }

    @Override
    protected String getServiceHost() {
        return computerServiceHost;
    }

    @Override
    protected String getContextPath() {
        return "/api/external/v1";
    }

    @Override
    protected RestClient getClient() {
        return restClient;
    }

    /**
     * Get student data from the onboard computer via inspection endpoint.
     *
     */
    public Student getStudentData(String ip, int port) {
        setComputerServiceHost(ip, port);
        
        try {
            String result = doGet("/inspection");
            
            // For testing, just return a simple Student object
            Student student = new Student();
            student.setId(ip); // Use IP as student number for simplicity
            student.setStatus("OK");
            return student;
        } catch (RuntimeException e) {
            throw new RuntimeException("Could not get data from IP: " + ip + " port: " + port, e);
        }
    }
}

