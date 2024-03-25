/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.cdi.internal.core;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTestAction;

public class NoBeansXmlRepeat implements RepeatTestAction {

    protected final static String ID = "NO_BEANS_XML_ACTION";

    public boolean isEnabled() {
        return true;
    }

    public void setup() throws Exception {
        //no op. Test code will react when it sees this ID on the stack
    }

    public String getID() {
        return ID;
    }

}
