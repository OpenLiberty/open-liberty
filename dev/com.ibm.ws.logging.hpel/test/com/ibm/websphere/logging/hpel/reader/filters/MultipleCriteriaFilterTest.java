/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader.filters;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.websphere.logging.hpel.reader.LogQueryBean;
import com.ibm.websphere.logging.hpel.reader.LogRecordFilter;
import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;

public class MultipleCriteriaFilterTest {
    @Test
    public void testAcceptNullRecordName() {
        LogQueryBean lqb = new LogQueryBean();
        lqb.setIncludeLoggers(new String[] { "my.*" });
        LogRecordFilter lrf = new MultipleCriteriaFilter(lqb);
        RepositoryLogRecord r = new RepositoryLogRecordImpl();
        Assert.assertFalse(lrf.accept(r));
    }

    @Test
    public void testAcceptExcludeMessage() {
        LogQueryBean lqb = new LogQueryBean();
        lqb.setExcludeMessages(new String[] { "*Hello*" });
        LogRecordFilter lrf = new MultipleCriteriaFilter(lqb);
        RepositoryLogRecordImpl r = new RepositoryLogRecordImpl();
        r.setMessage("Hello world!");
        Assert.assertFalse(lrf.accept(r));
    }
}
