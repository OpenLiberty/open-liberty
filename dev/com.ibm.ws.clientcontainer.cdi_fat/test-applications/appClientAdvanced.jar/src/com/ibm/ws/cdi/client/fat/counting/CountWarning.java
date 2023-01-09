/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.client.fat.counting;


/**
 * The event fired by {@link CountBean} when it's warning level is reached.
 */
public class CountWarning {

    private final int count;

    public CountWarning(int count) {
        this.count = count;
    }

    /**
     * Returns the count at the time the warning was fired.
     * 
     * @return count
     */
    public int getCount() {
        return count;
    }

}
