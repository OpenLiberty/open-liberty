/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.test.util;

import static org.junit.Assert.fail;

import com.ibm.ws.microprofile.faulttolerance.spi.AsyncRequestContextController;

public class MockAsyncRequestContextController implements AsyncRequestContextController {

    public int activateContextCount = 0;
    public int deactivateContextCount = 0;

    @Override
    public ActivatedContext activateContext() {
        activateContextCount++;
        return new MockActivatedContextImpl();
    }

    private class MockActivatedContextImpl implements ActivatedContext {

        boolean isDeactivated = false;

        @Override
        public void deactivate() {
            // ActivatedContext should only ever deactivated once per method attempt
            if (!isDeactivated) {
                isDeactivated = true;
                deactivateContextCount++;
            } else {
                fail("Activated context deactivated twice");
            }
        }

    }

    public int getActivateContextCount() {
        return activateContextCount;
    }

    public int getDeactivateContextCount() {
        return deactivateContextCount;
    }

}
