/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb;

import javax.ejb.EJBLocalObject;

/**
 * EJB 2.1 Local interface for Enterprise Beans
 */
public interface LocalComponent extends EJBLocalObject {
    public String localMethod(String text);
}
