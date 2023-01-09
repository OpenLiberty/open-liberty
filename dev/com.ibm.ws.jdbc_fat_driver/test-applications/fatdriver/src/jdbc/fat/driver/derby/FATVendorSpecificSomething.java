/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package jdbc.fat.driver.derby;

/**
 * This simulates a JDBC driver interface that you can unwrap your data source as,
 * in order to invoke vendor-specific API.
 */
public class FATVendorSpecificSomething {
    private int someValue;

    public FATVendorSpecificSomething(int someValue) {
        this.someValue = someValue;
    }

    public int increment() {
        return ++someValue;
    }
}
