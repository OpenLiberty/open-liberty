/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.JakartaEE9Action;

public class RepeatEE9Secure extends JakartaEE9Action {

    public static final String ID_EE9_SECURE = JakartaEE9Action.ID + "_Secure";

    public RepeatEE9Secure() {
        withID(ID_EE9_SECURE);
    }

    public static boolean isActive() {
        return RepeatTestFilter.isRepeatActionActive(ID_EE9_SECURE);
    }

    /**
     * Used to identify the RepeatTestAction and used in conjunction with @SkipForRepat
     */
    @Override
    public String getID() {
        return ID_EE9_SECURE;
    }
}
