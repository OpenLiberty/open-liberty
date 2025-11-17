/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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

package singleton.property.shared;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

public class TestData {
    private static CyclicBarrier svStartupCyclicBarrier = new CyclicBarrier(2);
    private static CyclicBarrier svNoStartupCyclicBarrier = new CyclicBarrier(2);

    public static void setStartupBarrierEnabled(boolean enabled) {
        svStartupCyclicBarrier = enabled ? new CyclicBarrier(2) : null;
    }

    public static void awaitStartupBarrier() {
        if (svStartupCyclicBarrier != null) {
            try {
                // Timeout long enough to allow an application to start, but less than
                // infinite to avoid hangs if the test fails.
                svStartupCyclicBarrier.await(8, TimeUnit.MINUTES);
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }
    }

    public static void setNoStartupBarrierEnabled(boolean enabled) {
        svNoStartupCyclicBarrier = enabled ? new CyclicBarrier(2) : null;
    }

    public static void awaitNoStartupBarrier() {
        if (svNoStartupCyclicBarrier != null) {
            try {
                // Timeout long enough to allow an application to start, but less than
                // infinite to avoid hangs if the test fails.
                svNoStartupCyclicBarrier.await(8, TimeUnit.MINUTES);
            } catch (Exception ex) {
                throw new Error(ex);
            }
        }
    }

}
