/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package collector.manager_fat;

import java.io.IOException;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.rest.handler.RESTHandler;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { RESTHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM",
                        RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=/test/tracelogger" })
public class RESTHandlerTraceLogger implements RESTHandler {
    private static final TraceComponent tc = Tr.register(RESTHandlerTraceLogger.class, "TraceHandler",
                                                         "collector.manager_fat.RESTHandlerTraceLoggerMessages");

    @Activate
    protected synchronized void activate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Activating " + this);
        }
    }

    @Deactivate
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    @Modified
    protected void modified(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, " Modified");
    }

    /** {@inheritDoc} */
    @Override
    public void handleRequest(RESTRequest req, RESTResponse resp) throws IOException {
        String messageKey = req.getParameter("messageKey");
        if (messageKey == null) {
            messageKey = "new_message$" + System.currentTimeMillis();
        }

//      Tr.audit(tc, msgKey, objs);
//      Tr.debug(tc, msg, objs);
//      Tr.dump(tc, msg, objs);
//      Tr.entry(tc, methodName, objs);
//      Tr.error(tc, msgKey, objs);
//      Tr.event(tc, msg, objs);
//      Tr.exit(tc, methodName);
//      Tr.fatal(tc, msgKey, objs);
//      Tr.info(tc, msgKey, objs);
//      Tr.warning(tc, msgKey, objs);

        System.out.println("sysout_log_" + messageKey);
        if (TraceComponent.isAnyTracingEnabled()) {
            Tr.audit(tc, "audit_log_" + messageKey);
            Tr.debug(tc, "debug_log_" + messageKey);
            Tr.dump(tc, "dump_log_" + messageKey);
            Tr.entry(tc, "entry_log_" + messageKey);
            Tr.error(tc, "error_log_" + messageKey);
            Tr.event(tc, "event_log_" + messageKey);
            Tr.exit(tc, "exit_log_" + messageKey);
            Tr.fatal(tc, "fatal_log_" + messageKey);
            Tr.info(tc, "info_log_" + messageKey);
            Tr.warning(tc, "warning_log_" + messageKey);

            Tr.debug(tc, "debug_log_2_" + messageKey);
            Tr.dump(tc, "dump_log_2_" + messageKey);
            Tr.entry(tc, "entry_log_2_" + messageKey);
            Tr.event(tc, "event_log_2_" + messageKey);
            Tr.exit(tc, "exit_log_2_" + messageKey);
        }

        resp.getWriter().write("All logs written");
    }

}