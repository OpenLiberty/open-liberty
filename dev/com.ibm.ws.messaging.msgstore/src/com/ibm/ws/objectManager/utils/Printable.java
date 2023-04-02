package com.ibm.ws.objectManager.utils;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
/**
 * @author Andrew_Banks
 * 
 *         Formatted dump Object state.
 */
public interface Printable
{
    /**
     * Print a dump of the Objects state.
     * 
     * @param printWriter where state is to be printed.
     */
    public abstract void print(java.io.PrintWriter printWriter);
} // interface Printable.
