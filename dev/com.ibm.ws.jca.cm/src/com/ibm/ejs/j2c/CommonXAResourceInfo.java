/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.io.Serializable;

/**
 * This interface contains elements of the XAResourceInfo that are common between
 * the server and the Embeddable EJB Container.
 */
public interface CommonXAResourceInfo extends Serializable {
    String getCfName();

    CMConfigData getCmConfig();

}
