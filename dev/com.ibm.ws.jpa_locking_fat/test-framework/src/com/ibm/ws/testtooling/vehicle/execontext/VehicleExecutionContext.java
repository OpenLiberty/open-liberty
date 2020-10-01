/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.vehicle.execontext;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;

public class VehicleExecutionContext implements java.io.Serializable {
    private static final long serialVersionUID = 6246199386391546915L;

    private TestExecutionContext testExecCtx;

    public VehicleExecutionContext() {

    }

    public VehicleExecutionContext(TestExecutionContext testExecCtx) {
        super();
        this.testExecCtx = testExecCtx;
    }

    public final TestExecutionContext getTestExecCtx() {
        return testExecCtx;
    }

    @Override
    public String toString() {
        return "VehicleExecutionContext [testExecCtx=" + testExecCtx + "]";
    }

}
