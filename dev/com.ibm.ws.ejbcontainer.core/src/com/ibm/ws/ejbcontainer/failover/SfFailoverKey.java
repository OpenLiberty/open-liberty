/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.failover;

import java.io.Serializable;

/**
 * SfFailoverKey is used to provide a serializable object that can be used
 * as a failover key. The key must provide a proper equals and hashcode
 * method so the this object can be used as a key in a HashMap implementation.
 */
public interface SfFailoverKey extends Serializable
{

}
