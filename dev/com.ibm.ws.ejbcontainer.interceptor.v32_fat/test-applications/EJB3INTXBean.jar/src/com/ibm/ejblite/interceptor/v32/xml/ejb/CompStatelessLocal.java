/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.xml.ejb;

import javax.ejb.EJBLocalObject;

/**
 * Class CompStatefulLocal
 */
public interface CompStatelessLocal extends EJBLocalObject {
    public boolean doNothing();

    public void discard();

    public boolean doNothing(CompStatelessLocal bean);

}
