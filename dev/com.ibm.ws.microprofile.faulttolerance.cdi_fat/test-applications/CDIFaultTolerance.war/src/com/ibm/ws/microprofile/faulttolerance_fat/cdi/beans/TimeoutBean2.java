/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance_fat.cdi.beans;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.ws.microprofile.faulttolerance_fat.util.ConnectException;
import com.ibm.ws.microprofile.faulttolerance_fat.util.Connection;

@RequestScoped
/**
 * Set the Timeout value to 5 seconds - which would lead to test failure - but this bean's config
 * will be overridden to 500 millis in microprofile-config.properties so that the bean will
 * generate a TimeoutException as expected by the test.
 */
@Timeout(5000)
public class TimeoutBean2 {

    public Connection connectA() throws ConnectException {
        try {
            Thread.sleep(2000);
            throw new ConnectException("Timeout did not interrupt");
        } catch (InterruptedException e) {
            //expected
            System.out.println("TimeoutBean Interrupted");
        }
        return null;
    }
}
