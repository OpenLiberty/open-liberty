/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
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
package com.ibm.ws.cache.persistent.filemgr;

interface Instrumentation
{
    // ------------------------------------------------------------------------
    // Instrumentation
    // ------------------------------------------------------------------------
    void update_read_time(long msec);
    void update_write_time(long msec);
    void increment_read_count();
    void increment_write_count();
    // ------------------------------------------------------------------------
    
}
