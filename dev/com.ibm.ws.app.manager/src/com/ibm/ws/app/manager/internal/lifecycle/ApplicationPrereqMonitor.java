package com.ibm.ws.app.manager.internal.lifecycle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;

/**
 * Non configurable immediate component to track prereqs as they appear.
 */
@Trivial
@Component(immediate=true,
    configurationPid = "com.ibm.ws.app.prereqmonitor",
    configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationPrereqMonitor {
    public static final TraceComponent tc = Tr.register(ApplicationPrereqMonitor.class);
    private final Set<String> declaredPrereqs = new TreeSet<>();
    private final Set<String> realisedPrereqs = new TreeSet<>();

    @Activate
    @Modified
    synchronized void recordDeclaredPrereqs(Map<String, Object> properties) {
        String[] pids = (String[]) properties.get("applicationPrereqDeclarations");
        declaredPrereqs.clear();
        for (String pid: pids) declaredPrereqs.add(pid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known configured application prereqs:" + declaredPrereqs);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    synchronized void setApplicationPrereq(ApplicationPrereq applicationPrereq) {
        // TODO: check for unexpected prereqs here and do FFDC and throw something
        realisedPrereqs.add(applicationPrereq.getClass().getName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Prereq added:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }
    }

    synchronized void unsetApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realisedPrereqs.remove(applicationPrereq.getClass().getName());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Prereq removed:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }
    }
}
