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

package fats.cxf.mustunderstand;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by Apache CXF 2.6.2
 * 2013-01-15T23:06:09.236-06:00
 * Generated source version: 2.6.2
 *
 */
@WebService(targetNamespace = "http://mustunderstand.cxf.fats", name = "MustUnderstand")
@XmlSeeAlso({ fats.cxf.mustunderstand.types.ObjectFactory.class })
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface MustUnderstand {

    @WebResult(name = "getVerResponse", targetNamespace = "http://mustunderstand.cxf.fats/types", partName = "out")
    @WebMethod
    public fats.cxf.mustunderstand.types.GetVerResponse invoke(
                                                               @WebParam(partName = "in", name = "getVer",
                                                                         targetNamespace = "http://mustunderstand.cxf.fats/types") fats.cxf.mustunderstand.types.GetVer in);
}
