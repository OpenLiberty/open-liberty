/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.lifecycle;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

/**
 * Enable Declarative Services to enforce start sequencing.
 * Application handlers should depend on this service.
 * Services should implement {@link ApplicationPrereq}
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationStartBarrierImpl implements ApplicationStartBarrier {
    private static final TraceComponent tc = Tr.register(ApplicationStartBarrierImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    void setApplicationPrereq(ApplicationPrereq applicationPrereq) {}

    @Activate
    public void activate() {}

    /**
     * Non configurable immediate component to track prereqs as they appear.
     */
    @Trivial
    @Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
    public static class PrereqWatcher implements ConfigurationListener {
        public static final TraceComponent tc = Tr.register(PrereqWatcher.class);
        private final ConfigurationAdmin configurationAdmin;
        private final ArrayList<String> realisedPrereqs = new ArrayList<>();

        /**
         * Use constructor parameter references to ensure that mandatory references are supplied first.
         *
         * @param configurationAdmin must be supplied before any calls to setApplicationPrereq
         */
        @Activate
        public PrereqWatcher(@Reference ConfigurationAdmin configurationAdmin) {
            this.configurationAdmin = configurationAdmin;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Known configured application prereqs:" + listConfiguredPrereqs());
            }
        }

        public void configurationEvent(ConfigurationEvent event) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Configuration changed: "
                             + "\nConfigured:" + listConfiguredPrereqs()
                             + "\n  Realised:" + realisedPrereqs);
            }
        }

        private String listConfiguredPrereqs() {
            Configuration[] configs;
            try {
                configs = this.configurationAdmin.listConfigurations("(service.factoryPid=" + ApplicationPrereq.class.getName() + ")");
            } catch (IOException | InvalidSyntaxException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                return "Error listing prereqs:" + sw;
            }
            if (configs == null)
                return null;

            String[] prereqClassNames = new String[configs.length];
            for (int i = 0; i < configs.length; i++) {
                prereqClassNames[i] = (String) configs[i].getProperties().get("className");
            }

            Arrays.sort(prereqClassNames);
            return Arrays.toString(prereqClassNames);
        }

        @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
        void setApplicationPrereq(ApplicationPrereq applicationPrereq) {
            realisedPrereqs.add(applicationPrereq.getClass().getName());
            Collections.sort(realisedPrereqs);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Prereq added:" + applicationPrereq.getClass().getName()
                             + "\nConfigured:" + listConfiguredPrereqs()
                             + "\n  Realised:" + realisedPrereqs);
            }
        }

        void unsetApplicationPrereq(ApplicationPrereq applicationPrereq) {
            realisedPrereqs.remove(applicationPrereq.getClass().getName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Prereq removed:" + applicationPrereq.getClass().getName()
                             + "\nConfigured:" + listConfiguredPrereqs()
                             + "\n  Realised:" + realisedPrereqs);
            }
        }
    }
}

