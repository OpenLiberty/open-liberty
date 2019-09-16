/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb;

/**
 * Remote interface for Session bean used for testing EJB Injection.
 **/
public interface EJBInjectionRemote {
    public void verifyEJBFieldInjection();

    public void verifyEJBMethodInjection();

    public void verifyNoEJBFieldInjection();

    public void verifyNoEJBMethodInjection();
}
