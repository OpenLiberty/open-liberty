/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.client.fat.counting;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * A simple application-wide counter which fires an event when a warning level is reached.
 * <p>
 * Not thread safe so pretty useless in practice but fine for this client test.
 */
@ApplicationScoped
public class CountBean {
    private int count;
    private boolean warningLevelReached;
    private int warningLevel;

    @Inject
    private Event<CountWarning> event;

    public CountBean() {
        count = 0;
        warningLevelReached = true;
        warningLevel = -1;
    }

    public int getCount() {
        return count;
    }

    public void add(int amount) {
        count += amount;
        if (count >= warningLevel && !warningLevelReached) {
            warningLevelReached = true;
            event.fire(new CountWarning(count));
        }
    }

    public void setWarningLevel(int newWarningLevel) {
        warningLevel = newWarningLevel;
        if (warningLevel > count) {
            warningLevelReached = false;
        } else {
            warningLevelReached = true;
        }
    }

}
