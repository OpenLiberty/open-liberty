/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.info.internal;

import java.util.ArrayList;
import java.util.List;

public class ActivityTimerImpl {
    public ActivityTimerImpl() {
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
