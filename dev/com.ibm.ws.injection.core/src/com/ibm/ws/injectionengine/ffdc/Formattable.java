/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.injectionengine.ffdc;

import com.ibm.ws.ffdc.IncidentStream;

/**
 * This interface specifies how a class can customize the display
 * of it's instances within an ffdc incident report.
 */
public interface Formattable
{
    /**
     * Emit the customized human readable text to represent this object
     *
     * @param is the incident stream, the data will be written here
     */
    void formatTo(IncidentStream is);
}
