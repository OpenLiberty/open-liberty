/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.apps.deltaspikeWar;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/TestDeltaspike")
@SuppressWarnings("serial")
public class DeltaspikeTestServlet extends FATServlet {

    @Inject
    private GlobalResultHolder resultHolder;

    @Test
    public void testSchedulingJob() throws InterruptedException {
        int count = 0;
        for (int i = 0; i < 6; i++) {
            count = resultHolder.getCount();
            if (count != 0) {
                break;
            }
            Thread.sleep(1000);
        }
        assertThat(count, not(equalTo(0)));
    }

}
