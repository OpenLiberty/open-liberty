/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.actions;

import com.ibm.ws.security.jwt.fat.builder.FATSuite;

import componenttest.rules.repeater.RepeatTestAction;

public class RunAsCollection implements RepeatTestAction {

    boolean setFlag = false;
    protected String currentID = null;

    RunAsCollection(boolean flag, String id) {

        setFlag = flag;
        currentID = id;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {

        FATSuite.runAsCollection = setFlag;
    }

    /*
     * (non-Javadoc)
     *
     * @see componenttest.rules.repeater.RepeatTestAction#getID()
     */
    @Override
    public String getID() {
        if (currentID != null) {
            return currentID;
        } else {
            return toString();
        }
    }
}
