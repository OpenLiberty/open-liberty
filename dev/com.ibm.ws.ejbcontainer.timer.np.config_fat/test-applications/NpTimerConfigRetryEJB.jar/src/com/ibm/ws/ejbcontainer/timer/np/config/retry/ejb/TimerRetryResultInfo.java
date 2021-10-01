/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.config.retry.ejb;

import java.util.ArrayList;

public class TimerRetryResultInfo {
    String beanName;
    ArrayList<Integer> attempts = new ArrayList<Integer>();
    ArrayList<Long> timestamps = new ArrayList<Long>();

    TimerRetryResultInfo(String beanName) {
        this.beanName = beanName;
    }

    void addAttemptRecord(int attempt, long timestamp) {
        attempts.add(Integer.valueOf(attempt));
        timestamps.add(Long.valueOf(timestamp));
    }

    ArrayList<Integer> getAttempts() {
        return attempts;
    }

    ArrayList<Long> getTimestamps() {
        return timestamps;
    }
}
