/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.commonext;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;

/**
 * Represents &lt;global-transaction>.
 */

@DDIdAttribute
public interface GlobalTransaction {

    boolean isSetSendWSATContext();

    @DDAttribute(name = "send-wsat-context", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "sendWSAT")
    boolean isSendWSATContext();

    boolean isSetTransactionTimeOut();

    @DDAttribute(name = "transaction-time-out", type = DDAttributeType.Int)
    @DDXMIAttribute(name = "componentTransactionTimeout")
    int getTransactionTimeOut();
}
