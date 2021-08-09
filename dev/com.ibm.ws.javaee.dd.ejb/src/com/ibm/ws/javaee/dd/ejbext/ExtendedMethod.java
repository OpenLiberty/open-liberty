/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.javaee.dd.ejbext;

import com.ibm.ws.javaee.dd.commonext.Method;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRefElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;extended-method>.
 */
@LibertyNotInUse
public interface ExtendedMethod extends Method {

    @DDAttribute(name = "ejb", type = DDAttributeType.String)
    @DDXMIRefElement(name = "enterpriseBean", referentType = com.ibm.ws.javaee.dd.ejb.EnterpriseBean.class, getter = "getName")
    String getEJB();
}
