/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.spec10.injection.common;

import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA22 implements RepeatTestAction {
    public static final String ID = "JPA22";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "JPA 2.2";
    }

    @Override
    public void setup() throws Exception {
        RepeaterInfo.repeatPhase = "jpa22.xml";
    }

    @Override
    public String getID() {
        return ID;
    }
}
