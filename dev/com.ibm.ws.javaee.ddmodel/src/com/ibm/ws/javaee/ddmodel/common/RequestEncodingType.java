/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.web.common.RequestEncoding;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;

/*
<xsd:complexType name="request-encodingType">
<xsd:simpleContent>
<xsd:extension base="javaee:xsdTokenType">
</xsd:extension>
</xsd:simpleContent>
</xsd:complexType>
*/

public class RequestEncodingType extends XSDTokenType implements RequestEncoding {

    public static class ListType extends ParsableListImplements<RequestEncodingType, RequestEncoding> {
        @Override
        public RequestEncodingType newInstance(DDParser parser) {
            return new RequestEncodingType();
        }
    }

}
