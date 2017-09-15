/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejbbnd;

import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIFlatten;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIType;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;message-driven>.
 */
@DDXMIType(name = "MessageDrivenBeanBinding", namespace = "ejbbnd.xmi")
public interface MessageDriven extends EnterpriseBean {

    /**
     * @return &lt;listener-port>, or null if unspecified
     *         Must have either a ListenerPort or a JCAAdapter - but not both.
     *         Test ListenerPort for null, then JCAAdapter for null.
     */
    @LibertyNotInUse
    @DDElement(name = "listener-port")
    ListenerPort getListenerPort();

    /**
     * @return &lt;jca-adapter>, or null if unspecified
     */
    @DDElement(name = "jca-adapter")
    @DDXMIFlatten
    JCAAdapter getJCAAdapter();

}
