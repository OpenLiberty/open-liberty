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
package com.ibm.ws.org.apache.felix.scr;

import java.util.Dictionary;

import org.apache.felix.scr.impl.Activator;
import org.apache.felix.scr.impl.ComponentRegistry;
import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.ComponentMethodsImpl;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.logger.ScrLogger;
import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.ConfigurableComponentHolder;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

public class NonReflectiveActivator extends Activator {

	@Override
	protected ComponentRegistry createComponentRegistry(ScrConfiguration scrConfiguration, ScrLogger logger) {
	    if (System.getProperty("scr.normal") != null) {
	        return super.createComponentRegistry(scrConfiguration, logger);
	    }
		return new NonReflectiveComponentRegistry(scrConfiguration, logger);
	}

	private static class NonReflectiveComponentRegistry extends ComponentRegistry {

		public NonReflectiveComponentRegistry(ScrConfiguration scrConfiguration, ScrLogger logger) {
			super(scrConfiguration, logger);
		}

		@Override
		public <S> ComponentHolder<S> createComponentHolder(ComponentActivator activator, ComponentMetadata metadata,
				ComponentLogger logger) {
			return new NonReflectiveComponentHolder<>(activator, metadata, logger);
		}
	}

	private static class NonReflectiveComponentHolder<S> extends ConfigurableComponentHolder<S> {

        public NonReflectiveComponentHolder(ComponentActivator activator, ComponentMetadata metadata, ComponentLogger logger)
        {
            super(activator, metadata, logger);
        }

        @Override
        protected ComponentMethods<S> createComponentMethods()
        {
            Bundle bundle = getActivator().getBundleContext().getBundle();
            String helperClassName = bundle.getHeaders("").get("SCR-NoReflectionHelper-Class");
            StaticBundleComponentFactory helper = null;
            if (helperClassName != null) {
                getActivator().getLogger().log(LogService.LOG_DEBUG,
                "BundleComponentActivator : No-Reflection Helper class {0}", null,
                helperClassName);
                try {
                    helper = (StaticBundleComponentFactory) bundle.loadClass(helperClassName).newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    getLogger().log(LogService.LOG_DEBUG, "Unable to create helper class " + helperClassName + " using reflection instead", ex);
                }
            }

            if (helper != null) {
                StaticComponentManager componentManager = helper.createStaticComponentManager(getComponentMetadata().getImplementationClassName());
                if (componentManager != null) {
                    return new NoReflectionComponentMethodsImpl<>(componentManager);
                }
            } 
            return new ComponentMethodsImpl<>();
        }
	}
}
