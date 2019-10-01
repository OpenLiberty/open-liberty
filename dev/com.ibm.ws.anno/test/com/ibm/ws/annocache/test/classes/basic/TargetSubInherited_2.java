/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.classes.basic;

public class TargetSubInherited_2 extends TargetInherited {
    @SuppressWarnings("hiding")
	public int protected2; // change 'protected2' to from protected to public.

    @SuppressWarnings("hiding")
    protected int public1; // change 'public1' from public to protected.

    @SuppressWarnings("hiding")
    int package1; // Overlay 'package1'

    @SuppressWarnings("unused")
	private int private2; // Overlay 'private2'
}
