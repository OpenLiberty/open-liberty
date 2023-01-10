/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.jpa.management;

import java.util.Map;

/**
 * 
 * DS components who implement this class will be invoked by the 
 * JPAComponentImpl.addIntegrationProperties method when initializing
 * the JPA provider.  Implementors will then have a chance to add
 * properties that are passed to the provider.
 *
 */
public interface JPAEMFPropertyProvider {

    public void updateProperties(Map<String,Object> props, ClassLoader applicationClassLoader);
}
