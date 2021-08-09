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

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Bulkhead;

import com.ibm.ws.microprofile.faulttolerance_fat.cdi.TestConstants;

@ApplicationScoped
public class BulkheadMultiRequestBean {

    @Bulkhead(2)
    public Boolean connectC(String data) throws InterruptedException {
        Thread.sleep(TestConstants.WORK_TIME);
        return Boolean.TRUE;
    }

}
