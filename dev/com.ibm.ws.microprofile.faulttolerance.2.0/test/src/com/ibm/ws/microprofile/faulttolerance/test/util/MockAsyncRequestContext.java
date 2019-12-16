/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;

public class MockAsyncRequestContext implements AsyncRequestContextController {

    public int activateContextCount = 0;
    public int deactivateContextCount = 0;

    @Override
    public void activateContext() {
        activateContextCount++;
    }

    @Override
    public void deactivateContext() {
        deactivateContextCount++;
    }

    public int getActivateContextCount() {
        return activateContextCount;
    }

    public int getDeactivateContextCount() {
        return deactivateContextCount;
    }

}
