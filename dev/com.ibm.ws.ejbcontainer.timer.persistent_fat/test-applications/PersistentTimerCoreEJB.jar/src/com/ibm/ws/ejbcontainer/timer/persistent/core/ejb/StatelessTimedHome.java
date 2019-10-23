/*******************************************************************************
 * Copyright (c) 2003, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.timer.persistent.core.ejb;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

/**
 * Home interface for a basic Stateless Session that implements the
 * TimedObject interface. It contains methods to test TimerService access.
 **/
public interface StatelessTimedHome extends EJBLocalHome {

    /**
     * @return StatelessTimedObject The StatelessTimedBean EJB object.
     * @exception javax.ejb.CreateException StatelessTimedBean EJB object was not
     *                created.
     */
    public StatelessTimedObject create() throws CreateException;

}
