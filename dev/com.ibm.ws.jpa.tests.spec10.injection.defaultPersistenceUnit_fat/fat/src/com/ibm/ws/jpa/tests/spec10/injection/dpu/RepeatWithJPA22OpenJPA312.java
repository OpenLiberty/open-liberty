/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.tests.spec10.injection.dpu;

import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA22OpenJPA312 implements RepeatTestAction {
    public static final String ID = "JPA22_OPENJPA312";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 2.2 feature and use OpenJPA 3.1.2 for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "openjpa30-cfg.xml";
    }

    @Override
    public String getID() {
        return ID;
    }
}
