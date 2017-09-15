/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejbext;

import java.util.concurrent.TimeUnit;

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyDurationType;

/**
 * Represents the &lt;time-out> element of Session.
 */
@DDIdAttribute
public interface TimeOut {

    /**
     * @return value="..." attribute value -- use is required!
     */
    @LibertyDurationType(timeUnit = TimeUnit.SECONDS)
    @DDAttribute(name = "value", type = DDAttributeType.Int, required = true)
    @DDXMIAttribute(name = "timeout")
    int getValue();

}
