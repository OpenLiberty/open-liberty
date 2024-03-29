/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package com.ibm.ws.testtooling.vehicle.ejb;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;

public interface EJBTestVehicle {
    /**
     * Returns the bean's name as defined by the <env-entry> entry keyed by the name "beanName".
     * Defining this is optional, and returns an empty String for beans that did not define it.
     *
     * @return
     */
    public String getEnvDefinedBeanName();

    public void executeTestLogic(TestExecutionContext ctx);

    public void release();
}
