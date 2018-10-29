/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.context.location;

import org.eclipse.microprofile.concurrent.spi.ThreadContextController;

/**
 * Example third-party thread context restorer, to be used for testing purposes.
 * This context associates a US state name with a thread, such that the applicable
 * sales tax rate for the corresponding state is included when either of the
 * getStateSalesTax() or getTotalSalesTax() methods is invoked from the thread.
 */
public class StateContextRestorer implements ThreadContextController {
    private boolean restored = false;
    private final String stateNameToRestore;

    StateContextRestorer(String stateNameToRestore) {
        this.stateNameToRestore = stateNameToRestore;
    }

    @Override
    public void endContext() {
        if (restored)
            throw new IllegalStateException("thread context was already restored");
        StateContextProvider.stateName.set(stateNameToRestore);
        restored = true;
    }

    @Override
    public String toString() {
        return "StateContextRestorer@" + Integer.toHexString(hashCode()) + "(" + stateNameToRestore + ")";
    }
}
