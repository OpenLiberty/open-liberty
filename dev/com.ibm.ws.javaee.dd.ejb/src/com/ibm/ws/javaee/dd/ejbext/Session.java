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

import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIFlatten;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIType;

/**
 * Represents a &lt;enterpriseBean> object of type session.
 */
@DDXMIType(name = "SessionExtension", namespace = "ejbext.xmi")
public interface Session extends EnterpriseBean {

    /**
     * @returns &lt;time-out> element, return null if not specified.
     */
    @DDElement(name = "time-out")
    @DDXMIFlatten
    TimeOut getTimeOut();

}
