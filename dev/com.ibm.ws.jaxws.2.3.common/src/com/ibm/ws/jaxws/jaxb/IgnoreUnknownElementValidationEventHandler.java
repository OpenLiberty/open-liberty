/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.jaxb;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is just a skeleton class for now -
 *
 * This class will allow us to set a custom ValidationEventHandler on the JAXB Unmashaller
 * which allow a user to ignore unknown elements in their inboound resonses.
 *
 * TODO: Complete Implementation
 */
public class IgnoreUnknownElementValidationEventHandler implements ValidationEventHandler {

    private static final TraceComponent tc = Tr.register(IgnoreUnknownElementValidationEventHandler.class);
    
    public boolean handleEvent(ValidationEvent event) {
        boolean debug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        
        if(debug) {
            Tr.debug(tc, "handling event - " + event);
        }
        
        if(event.getMessage().contains("unexpected element")) {
            if(debug) {
                Tr.debug(tc, "The event contains an unexpected element, returning true to allow unmarshaller to continue. ");
            }
            return true;
        } else {

            if(debug) {
                Tr.debug(tc, "The event doesn't contain an unexpected element, will return false stopping the unmarshaller. ");
            }
            return false;
        }
    }

}
