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
package com.ibm.ws.sip.container;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.logging.Introspector;

/**
 * This service is used to introspect the SIP state details.
 * 
 * The introspector generates the server dump and print it to the SipContainerIntrospector.txt.
 * The verbosity level (succinct or verbose) can be defined in the server.xml.
 * 
 */
@Component(service = Introspector.class,
           configurationPid = "com.ibm.ws.sip.introspector")
public class SipContainerIntrospector implements Introspector {

    private static final TraceComponent tc = Tr.register(SipContainerIntrospector.class);

    /** A dumping mode to call when the introspect invoked */
    private final String SUCCINCT_DUMP_METHOD = "SUCCINCT";
    private final String VERBOSE_DUMP_METHOD = "VERBOSE";

    private String _dumpMethodType;
  
  
    @Activate
    public void activate(ComponentContext ctx, Map<String, Object> properties) {
        Object value = properties.get("method");
        if (value != null) {
            _dumpMethodType = value.toString();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "activate SIP introspect for " + value);
        }
    }

    @Override
    public void introspect(PrintWriter out) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "IntrospectableService invoked in sip container");
        }

        if (_dumpMethodType == null || _dumpMethodType.isEmpty()) {
            out.println("There is no configuration for dumping. Please define a dumping mode in the server.xml");
            return;
        }
        out.println(_dumpMethodType);

        DumpActivator dumpActivator = new DumpActivator();
        StringBuilder strToPrint = new StringBuilder();

        if (_dumpMethodType.equals(SUCCINCT_DUMP_METHOD)) {
            dumpActivator.startDumpingListOfSessions(DumpActivator.DUMP_SAS_IDs, strToPrint);
            dumpActivator.startDumpingListOfSessions(DumpActivator.DUMP_TU_IDs, strToPrint);
        }
        else if (_dumpMethodType.equals(VERBOSE_DUMP_METHOD)) {
            dumpActivator.startDumpingListOfSessions(DumpActivator.DUMP_SAS_DETAILs, strToPrint);
            dumpActivator.startDumpingListOfSessions(DumpActivator.DUMP_TU_DETAILs, strToPrint);
        }

        out.println(strToPrint);

        //Output indication of end of dump.
        out.println("--- End of Dump ---");
    }

	@Override
	public String getIntrospectorName() {
		return "SipContainerIntrospector";
	}

	@Override
	public String getIntrospectorDescription() {
		return "SIP state details";
	}
}
