/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package batch.fat.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Used to verify sets of values across all partitions
 */
public class PartitionSetChecker {

    private final static Logger logger = Logger.getLogger("test");

    Set<String> actualValues = new HashSet<String>();
    String[] expectedValues = null;

    public PartitionSetChecker() {}

    /**
     * @param expectedValues
     */
    public PartitionSetChecker(String[] expectedValues) {
        this.expectedValues = expectedValues;
    }

    public void assertExpected() {

        logger.fine("Have seen property values: " + traceSet(actualValues));

        assertEquals("Wrong number of unique property values", expectedValues.length, actualValues.size());

        for (String next : expectedValues) {
            assertTrue("Haven't seen expected property value: " + next, actualValues.contains(next));
        }
    }

    /**
     * Adds in a thread-safe manner
     * 
     * @param str
     */
    public void add(String str) {
        synchronized (actualValues) {
            logger.fine("Adding to actual set (may already be contained): " + str);
            actualValues.add(str);
        }
    }

    private String traceSet(Set<String> set) {
        StringBuilder buf = new StringBuilder();
        if (set == null) {
            return "<empty set>";
        } else {
            for (String s : set) {
                buf.append(s).append(",");
            }
        }
        return buf.toString();
    }

    public void setExpectedValues(String[] expectedValues) {
        this.expectedValues = expectedValues;
    }

}
