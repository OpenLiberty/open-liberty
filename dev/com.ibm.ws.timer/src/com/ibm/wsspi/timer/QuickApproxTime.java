/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.wsspi.timer;


/**
 * Static utility for getting quick approx time
 */
public class QuickApproxTime {
    /**
     * Get the time which is set according to the time interval.
     * 
     * @return time
     */
    public static long getApproxTime() {

        // Testing has shown that currentTimeMillis is now not such a performance drag, as it was years ago.
        // For 2Q 2015 we will go straight to currentTimeMillis.
        // In 3Q 2015, assuming no issues with this fix, we can get rid of the Approx Timer service altogether
        //QuickApproxTimeImpl impl = QuickApproxTimeImpl.instance.get();

        //if (impl == null) {
        return System.currentTimeMillis();
        //}

        //return impl.getApproxTime();

    }
}
