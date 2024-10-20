/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package fats.cxf.basicssl.wssec;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2012-11-09T22:47:08.452-06:00
 * Generated source version: 2.6.2
 * 
 */
@WebService(targetNamespace = "http://wssec.basicssl.cxf.fats", name = "FVTVersionBA")
@XmlSeeAlso({fats.cxf.basicssl.wssec.types.ObjectFactory.class})
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface FVTVersionBA {

    @WebResult(name = "responseString", targetNamespace = "http://wssec.basicssl.cxf.fats/types", partName = "getVersionReturn")
    @WebMethod
    public fats.cxf.basicssl.wssec.types.ResponseString invoke(
        @WebParam(partName = "getVersion", name = "requestString", targetNamespace = "http://wssec.basicssl.cxf.fats/types")
        fats.cxf.basicssl.wssec.types.RequestString getVersion
    );
}
