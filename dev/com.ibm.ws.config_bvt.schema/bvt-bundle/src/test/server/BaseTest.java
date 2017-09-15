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
package test.server;

public abstract class BaseTest {

    public static final long TIMEOUT = 30 * 1000;

    protected String name;

    public BaseTest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String[] getServiceClasses();

}
