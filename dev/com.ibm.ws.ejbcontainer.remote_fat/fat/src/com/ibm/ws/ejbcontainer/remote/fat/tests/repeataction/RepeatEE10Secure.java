/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.JakartaEE10Action;

public class RepeatEE10Secure extends JakartaEE10Action {

    public static final String ID_EE10_SECURE = JakartaEE10Action.ID + "_Secure";

    public RepeatEE10Secure() {
        withID(ID_EE10_SECURE);
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID_EE10_SECURE);
    }

    /**
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepat
     */
    @Override
    public String getID() {
        return ID_EE10_SECURE;
    }
}
