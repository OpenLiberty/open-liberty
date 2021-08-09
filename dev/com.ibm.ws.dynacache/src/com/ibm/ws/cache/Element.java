/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

final class Element extends QueueElement
{

    Element(Object key, Object object)
    {
	this.key = key;
	this.object = object;
    }

    public String toString()
    {
	return "Element: " + key.toString() + " " + object.toString();
    }

    final Object key;
    final Object object;
}
