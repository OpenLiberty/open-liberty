/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.commonext;

import com.ibm.ws.javaee.dd.commonext.LocalTransaction;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Manual implementation of the localTran XMI element.
 */
public class EJBLocalTranXMIType extends LocalTransactionType {
    private enum Resolver {
        BEAN(ResolverEnum.APPLICATION),
        CONTAINER(ResolverEnum.CONTAINER_AT_BOUNDARY);

        final ResolverEnum value;

        private Resolver(ResolverEnum value) {
            this.value = value;
        }
    }

    public EJBLocalTranXMIType() {
        super(true);
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("boundary".equals(localName)) {
                this.boundary = parser.parseEnumAttributeValue(index, LocalTransaction.BoundaryEnum.class);
                return true;
            }
            if ("resolver".equals(localName)) {
                this.resolver = parser.parseEnumAttributeValue(index, Resolver.class).value;
                return true;
            }
            if ("unresolvedAction".equals(localName)) {
                this.unresolved_action = parser.parseEnumAttributeValue(index, LocalTransaction.UnresolvedActionEnum.class);
                return true;
            }
        }

        // Do not delegate to super.handleAttribute.
        return false;
    }
}
