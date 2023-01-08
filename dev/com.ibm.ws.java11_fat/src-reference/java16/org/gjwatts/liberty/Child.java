/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.gjwatts.liberty;

// Child class to test out basic sealed classes in Java 15
public non-sealed class Child extends Parent {

    public Child() {
        super();
    }

    @Override
    public String greetings() {
        return "Hello from the child";
    }
}
