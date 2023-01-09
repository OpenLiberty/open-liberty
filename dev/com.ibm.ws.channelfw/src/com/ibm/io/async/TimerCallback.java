/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package com.ibm.io.async;

/**
 * Callback used for AIO timeouts.
 */
public interface TimerCallback {

    /**
     * Called when timeout has triggered.
     * 
     * @param twi attachment object passed in when timeout work item was created
     */
    void timerTriggered(TimerWorkItem twi);

}
