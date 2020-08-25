/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
