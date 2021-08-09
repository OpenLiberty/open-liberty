/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.monitor.PMI;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.wsspi.pmi.factory.StatisticActions;
import com.ibm.wsspi.pmi.factory.StatsFactory;
import com.ibm.wsspi.pmi.factory.StatsFactoryException;
import com.ibm.wsspi.pmi.factory.StatsGroup;
import com.ibm.wsspi.pmi.factory.StatsInstance;

public class PMITest extends StatisticActions {

    String template = "/com/ibm/websphere/pmi/xml/samplestats.xml";
    static StatsInstance st;
    static StatsGroup stgroup;

    @Test
    public void testCreateAndRemoveStatsInstance() throws StatsFactoryException {

        if (PmiRegistry.isDisabled()) {
            PmiRegistry.enable();
        }
        String expectedName = "myStatsInstanceExampleONE";
        st = StatsFactory.createStatsInstance(expectedName, template, null, this);
        String statsName = st.getName();

        assertEquals("StatsInstance should return the correct name", expectedName, statsName);

        StatsFactory.removeStatsInstance(st);

        assertEquals("Created StatsInstance should not be there/removed", StatsFactory.getStatsInstance(new String[] { expectedName }), null);

    }

    @Test
    public void testCreateAndRemoveStatsGroup() throws StatsFactoryException {
        String groupName = "MyStatsGroup";

        if (PmiRegistry.isDisabled()) {
            PmiRegistry.enable();
        }
        st = StatsFactory.createStatsInstance("myStatsInstanceExampleTWO", template, null, this);
        stgroup = StatsFactory.createStatsGroup(groupName, template, st, null, this);

        String statsName = stgroup.getName();
        assertEquals("Creating a stats group should return the correct name", groupName, statsName);

        StatsFactory.removeStatsGroup(stgroup);

        assertEquals("Created statsGroup should not be there/removed", StatsFactory.getStatsGroup(new String[] { "myStatsInstanceExampleTWO", groupName }), null);

        StatsFactory.removeStatsInstance(st);

        assertEquals("Created statsInstance should not be there/removed", StatsFactory.getStatsInstance(new String[] { "myStatsInstanceExampleTWO" }), null);

    }

}
