/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.jndi.internal;

import java.util.Properties;
import java.util.Vector;

import javax.naming.CompoundName;
import javax.naming.InvalidNameException;

@SuppressWarnings("serial")
public class MockNameImpl extends CompoundName {
    private static Properties PROPS = new Properties() {
        {
            put("jndi.syntax.direction", "left_to_right");
            put("jndi.syntax.separator", "/");
        }
    };

    MockNameImpl() {
        super(new Vector<String>().elements(), PROPS);
    }

    MockNameImpl(String name) throws InvalidNameException {
        super(name, PROPS);
    }
}
