/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;

import com.ibm.ws.jaxws.wsat.Constants;

/**
 *
 */
public class WSATAssertionPolicyProvider extends AbstractPolicyInterceptorProvider {
    /**  */

    private static final long serialVersionUID = 1888067784675731010L;

    public static final Collection<QName> ASSERTION_TYPES;

    static {
        ASSERTION_TYPES = new ArrayList<QName>();
        ASSERTION_TYPES.add(Constants.AT_ASSERTION_QNAME);
    }

    public WSATAssertionPolicyProvider() {
        super(ASSERTION_TYPES);
        //      getInInterceptors().add(new WSATPolicyAwareInterceptor(Phase.RECEIVE, false));
    }
}
