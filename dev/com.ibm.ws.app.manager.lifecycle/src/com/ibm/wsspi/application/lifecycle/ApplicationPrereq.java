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
package com.ibm.wsspi.application.lifecycle;

/**
 * This a marker interface for Service components that must be present before wsspi.applications are started.  
 * 
 * Implementors must also provide a config element in their defaultInstances.xml as follows:
 * {@code
 *   <com.ibm.wsspi.application.lifecycle.ApplicationPrereq className="fully.qualified.ClassName" />
 * }
 */
public interface ApplicationPrereq {
}
