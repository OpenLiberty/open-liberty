/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.test.shared;

import javax.enterprise.context.ApplicationScoped;

// have a static counter and an instance counter

// call the class from two different applications
// check that the static counter is incremented (shows that the class is loaded by the same class loader)
// check that the instance loader is not (shows that you don't get the same instance)

@ApplicationScoped
public class InjectedHello {

    public static final String PREFIX = "Hello from an InjectedHello, I am here: ";

    public String areYouThere(String name) {
        return PREFIX + name;
    }
}
