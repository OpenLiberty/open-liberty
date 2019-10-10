/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.test.data;

import com.ibm.ws.anno.test.data.sub.SubBase;

/**
 *
 */
public class DerivedBase extends SubBase {
    protected int public1;
    public int protected2;
    private int private2;

    int package1;
}
