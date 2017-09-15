/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.ibm.jbatch.container.jsl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import com.ibm.jbatch.container.RASConstants;

public class JSLValidationEventHandler implements ValidationEventHandler, RASConstants {
    
	private static final String CLASSNAME = JSLValidationEventHandler.class.getName();

    private final static Logger logger = Logger.getLogger(CLASSNAME, BATCH_MSG_BUNDLE);
    
    private boolean eventOccurred = false;
    
    public boolean handleEvent(ValidationEvent event) {
    	StringBuilder buf = new StringBuilder(250);
        buf.append("\nMESSAGE: " + event.getMessage());
        buf.append("\nSEVERITY: " + event.getSeverity());
        buf.append("\nLINKED EXC: " + event.getLinkedException());
        buf.append("\nLOCATOR INFO:\n------------");
        
        buf.append("\n  COLUMN NUMBER:  " + event.getLocator().getColumnNumber());
        buf.append("\n  LINE NUMBER:  " + event.getLocator().getLineNumber());
        buf.append("\n  OFFSET:  " + event.getLocator().getOffset());
        buf.append("\n  CLASS:  " + event.getLocator().getClass());
        buf.append("\n  NODE:  " + event.getLocator().getNode());
        buf.append("\n  OBJECT:  " + event.getLocator().getObject());
        buf.append("\n  URL:  " + event.getLocator().getURL());
        
        logger.log(Level.SEVERE, "jsl.schema.invalid", new Object[] {event.getLocator().getURL(), buf} );
        
        eventOccurred = true;
        
        // Return 'false' to abort parsing rather than continue.
        //
        // This limits the verbosity of the messages here.  The user probably wants to be using a tool
        // to figure out the problem, not the error message here.
        return false;
    }

    public boolean eventOccurred() {
        return eventOccurred;
    }

}
