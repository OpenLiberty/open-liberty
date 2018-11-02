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

import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

/**
 * Example third-party thread context provider, to be used for testing purposes.
 * This context associates a US state name with a thread, such that the applicable
 * sales tax rate for the corresponding state is included when either of the
 * getStateSalesTax() or getTotalSalesTax() methods is invoked from the thread.
 */
public class StateContextProvider implements ThreadContextProvider {
    static ThreadLocal<String> stateName = ThreadLocal.withInitial(() -> "");

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return new StateContextSnapshot("");
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return new StateContextSnapshot(stateName.get());
    }

    @Override
    public String getThreadContextType() {
        return TestContextTypes.STATE;
    }
}
