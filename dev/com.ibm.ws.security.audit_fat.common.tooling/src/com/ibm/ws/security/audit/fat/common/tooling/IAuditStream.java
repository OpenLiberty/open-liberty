/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.security.audit.fat.common.tooling;

/**
 * Implement this interface in order to return a single line from a stream of audit data.
 */
public interface IAuditStream {
	
    /**
     * Returns the next line from a stream of audit data.
     * Returns null on normal end of stream.
     * Throws on error reading from stream.
     * 
     *  @return next line (or null)
     */
 
	String readNext() throws Exception;
	

}
