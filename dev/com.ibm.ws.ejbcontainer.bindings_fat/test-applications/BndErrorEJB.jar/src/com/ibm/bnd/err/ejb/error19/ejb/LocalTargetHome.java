/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.bnd.err.ejb.error19.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

public interface LocalTargetHome extends EJBLocalHome {
    public LocalCompBiz create() throws CreateException;
}
