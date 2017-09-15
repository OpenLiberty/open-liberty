/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

class ConfigurationInfo {

    private static final TraceComponent tc = Tr.register(ConfigurationInfo.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    final ConfigElement configElement;
    final ExtendedConfiguration config;
    final Map<String, ExtendedAttributeDefinition> metaTypeAttributes;
    final boolean deleted;
    final RegistryEntry registryEntry;
    final List<ExtendedConfiguration> superTypeConfigs = new ArrayList<ExtendedConfiguration>();

    ConfigurationInfo(ConfigElement configElement, ExtendedConfiguration config, RegistryEntry registryEntry, boolean deleted) {
        this.configElement = configElement;
        this.config = config;
        this.registryEntry = registryEntry;
        this.metaTypeAttributes = registryEntry != null ? registryEntry.getAttributeMap() : null;
        this.deleted = deleted;
    }

    void addSuperTypeConfig(ExtendedConfiguration superTypeConfig) {
        superTypeConfigs.add(superTypeConfig);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(configElement.getFullId());
        builder.append((deleted) ? ":d" : ":m");
        return builder.toString();
    }

    public boolean matches(ConfigurationInfo other) {
        boolean b = false;
        if (other != null) {
            b = this.config == other.config && this.configElement.equalsIgnoreIdAttr(other.configElement) &&
                this.deleted == other.deleted &&
                ((metaTypeAttributes == null && other.metaTypeAttributes == null) || (metaTypeAttributes != null && metaTypeAttributes.equals(other.metaTypeAttributes))) &&
                ((registryEntry == null && other.registryEntry == null) || (registryEntry != null && registryEntry.equals(other.registryEntry)));
        }
        return b;
    }

    /**
     * @param futures
     */
    public void fireEvents(Collection<Future<?>> futures) {
        config.lock();
        try {
            if (deleted) {
                config.fireConfigurationDeleted(futures);
            } else {
                config.fireConfigurationUpdated(futures);
            }
        } catch (Exception e) {
            String fullId = configElement.getFullId();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while trying to fire configuration update events for " + fullId + ". Exception message = " + e.getMessage());
            }
            Tr.error(tc, "error.config.update.event", new Object[] { fullId, e.getMessage() });
        } finally {
            config.unlock();
        }

        for (ExtendedConfiguration superTypeConfig : superTypeConfigs) {
            superTypeConfig.lock();
            try {
                //TODO if (deleted) send deleted event?
                superTypeConfig.fireConfigurationUpdated(futures);
            } catch (Exception e) {
                String fullId = superTypeConfig.getFullId().toString();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception while trying to fire configuration update events for " + fullId + ". Exception message = " + e.getMessage());
                }
                Tr.error(tc, "error.config.update.event", new Object[] { fullId, e.getMessage() });
            } finally {
                superTypeConfig.unlock();
            }
        }
    }
}