/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
