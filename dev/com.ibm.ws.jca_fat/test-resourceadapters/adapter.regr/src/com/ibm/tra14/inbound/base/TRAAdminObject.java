/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tra14.inbound.base;

import java.io.Serializable;

import javax.jms.Destination;
import javax.jms.Message;
import javax.naming.Referenceable;

public interface TRAAdminObject extends Serializable, Referenceable, Destination {

    public void putMsg(Message msg);

    public Message getMsg();

}
