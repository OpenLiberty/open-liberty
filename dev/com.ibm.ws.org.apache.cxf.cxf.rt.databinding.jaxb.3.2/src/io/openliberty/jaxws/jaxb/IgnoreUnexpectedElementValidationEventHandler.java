/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.jaxb;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class will allow us to set a custom ValidationEventHandler on the JAXB Unmashaller
 * which allow a user to ignore unknown elements in their inbound responses.
 *
 * By default CXF uses a DefaultValidationEventHandler on the client side, this performs minimal schema validation
 * while ignoring the more StAX based validation. By replacing the DefaultValidationEventHandler, we also have to ignore
 * the StAX based Validation. That's why we catch both the values of UNEXPECTED_ELEMENT_MESSAGE and UNEXPECTED_ELEMENT_MESSAGE_ID
 * 
 */
public class IgnoreUnexpectedElementValidationEventHandler implements ValidationEventHandler {
    
    
    
    private static final String UNEXPECTED_ELEMENT_MESSAGE = "unexpected element";
    
    private static final String NO_CHILD_ELEMENT_MESSAGE_ID = "cvc-complex-type.2.4.d";

    private static final TraceComponent tc = Tr.register(IgnoreUnexpectedElementValidationEventHandler.class);
    
    public boolean handleEvent(ValidationEvent event) {
        String message = event.getMessage();
        
        boolean debug = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        
        if(message.contains(NO_CHILD_ELEMENT_MESSAGE_ID) || message.contains(UNEXPECTED_ELEMENT_MESSAGE)) {
            if(debug) {
                Tr.debug(tc, "Caught unexpected element event during validation, returning true to ignore event and continue unmarhalling");
            }
            return true;
        } else {
            if(debug) {
                Tr.debug(tc, "Event is unrelated to unexpected elements, returning false to stop unmarshalling");
            }
            return false;
        }
    }

}
