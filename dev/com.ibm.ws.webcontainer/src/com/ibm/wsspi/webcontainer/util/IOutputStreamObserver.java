/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.util;

/**
 * Observes the events of an OutputStream.
 */
public interface IOutputStreamObserver
{
/**
 * Notification that the OutputStream has been closed.
 */
    void alertClose();
/**
 * Notification that the OutputStream has been written to for the first time.
 */
    void alertFirstWrite();

/**
 * Notification that the OutputStream has been flushed to for the first time.
 */
    void alertFirstFlush();
/**
 * Notification that there has been an exception in the OutputStream.
 */
    void alertException();
}
