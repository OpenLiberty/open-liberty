/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.util.ArrayList;
import java.util.List;

public class ActivityTimer {
    public ActivityTimer() {
        super();
    }

    protected List<Long> activityStarts = new ArrayList<Long>();
    protected List<Long> activityWindows = new ArrayList<Long>();

    public long getTime() {
        return System.currentTimeMillis();
    }

    public void startActivity() {
        activityStarts.add(Long.valueOf(getTime()));
        activityWindows.add(Long.valueOf(0L));
    }

    public long endActivity() {
        int nextDepth = activityStarts.size() - 1;

        long thisEnd = getTime();
        long thisStart = (activityStarts.remove(nextDepth)).longValue();

        long thisDuration = thisEnd - thisStart;

        long thisWindow = (activityWindows.remove(nextDepth)).longValue();

        if (nextDepth > 0) {
            activityWindows.set(nextDepth - 1,
                                Long.valueOf(activityWindows.get(nextDepth - 1).longValue() + thisDuration));
        }

        return thisDuration - thisWindow;
    }
}
