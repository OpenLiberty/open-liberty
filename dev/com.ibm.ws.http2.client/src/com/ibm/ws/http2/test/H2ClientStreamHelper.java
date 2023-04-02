/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.http2.test;

public class H2ClientStreamHelper {

    //client should use odd numbers, init to -1 so that the first call to nextStreamID returns 1
    public static int lastStreamID = -1;

    //return the next ID to use
    public static int nextStreamID() {
        return lastStreamID += 2;
    }

}