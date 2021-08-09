/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jaxws.MTOM;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(targetNamespace = "http://MTOMService/")
public interface MTOMInter {

    @WebMethod
    public byte[] getAttachment();

    @WebMethod
    public String sendAttachment(byte[] att);
}
