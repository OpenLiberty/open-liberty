/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.xml.ejbx;

/**
 * Remote interface with methods to verify Environment Injection.
 **/
public interface EJBMethodRemote extends EJBRemote, EJBMethod {
    /**
     * Verify injected EJB is the expected bean
     **/
    public String getBeanName();
}
