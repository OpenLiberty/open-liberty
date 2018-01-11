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
package com.ibm.ws.cdi.web.liberty;

import java.util.Map;

import javax.servlet.Filter;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.web.factories.WeldListenerFactory;
import com.ibm.ws.cdi.web.interfaces.CDIWebConstants;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfigurator.ConfigItem;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.ServletConfiguratorHelperFactory;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.FilterMapping;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;

/*
 * This provides a temporary solution for the "CDI Conversation Filter" mapping.
 * The CDI specification allows a web descriptor to specify a filter mapping with the
 * special name "CDI Conversation Filter" that refers to no filter by the same name.
 * The container is supposed to map the implementation to the filter impl of the
 * CDI implementation.  IN this case that is the org.jboss.weld.servlet.ConversationFilter
 * class.
 *
 * This class works hand in hand with the com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper
 * class to get the ConversationFilter configured.
 *
 * TODO it is not clear by the specification, but this implementation only supports the
 * "CDI Conversation Filter" mapping specified in the web.xml.  Not sure if it is valid to specify
 * this in a web fragment.
 */
@Component(service = ServletConfiguratorHelperFactory.class, property = { "service.vendor=IBM" })
public class WeldConfiguratorHelperFactory implements ServletConfiguratorHelperFactory {

    /** {@inheritDoc} */
    @Override
    public ServletConfiguratorHelper createConfiguratorHelper(ServletConfigurator configurator) {
        return new WeldConfigHelper(configurator);
    }

    static class WeldConfigHelper implements ServletConfiguratorHelper {
        private final ServletConfigurator configurator;

        public WeldConfigHelper(ServletConfigurator configurator) {
            this.configurator = configurator;
        }

        /** {@inheritDoc} */
        @Override
        public void configureInit() throws UnableToAdaptException {
            WebApp webApp = configurator.getModuleContainer().adapt(WebApp.class);
            if (webApp != null) {
                for (FilterMapping filterMapping : webApp.getFilterMappings()) {
                    if (CDIWebConstants.CDI_CONVERSATION_FILTER.equals(filterMapping.getFilterName())) {
                        // Found that the app has a "CDI Conversation Filter" filter mapping.
                        // Need to create a placeholder ConfigItem<IFilterConfig>> so that a proper FilterConfig
                        // can get created later by
                        // com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper.createCdiConversationFilter(DeploymentDescriptor)
                        Map<String, ConfigItem<IFilterConfig>> filterMap = configurator.getConfigItemMap("filter");
                        ConfigItem<IFilterConfig> cdiConversationFilter = filterMap.get("CDI Conversation Filter");
                        if (cdiConversationFilter == null) {
                            cdiConversationFilter = configurator.createConfigItem(null);
                            filterMap.put(CDIWebConstants.CDI_CONVERSATION_FILTER, cdiConversationFilter);
                        }
                    }
                }
            }
        }

        /** {@inheritDoc} */
        @Override
        public void configureFromWebApp(WebApp webApp) throws UnableToAdaptException {
            Map<String, ConfigItem<IFilterConfig>> filterMap = configurator.getConfigItemMap("filter");
            ConfigItem<IFilterConfig> existedFilter = filterMap.get(CDIWebConstants.CDI_CONVERSATION_FILTER);
            IFilterConfig filterConfig = existedFilter == null ? null : existedFilter.getValue();
            if (filterConfig != null) {
                // found the placeholder created by com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper.createCdiConversationFilter(DeploymentDescriptor)
                // now fill in the implementation details
                Class<? extends Filter> filterClass = WeldListenerFactory.getConversationFilter();
                filterConfig.setFilterClassName(filterClass.getName());
                filterConfig.setFilterClass(filterClass);
                filterConfig.setFilterClassLoader(filterClass.getClassLoader());
            }
        }

        /** {@inheritDoc} */
        @Override
        public void configureFromWebFragment(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
            // nothing
        }

        /** {@inheritDoc} */
        @Override
        public void configureFromAnnotations(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
            // nothing
        }

        /** {@inheritDoc} */
        @Override
        public void configureDefaults() throws UnableToAdaptException {
            // nothing
        }

        /** {@inheritDoc} */
        @Override
        public void configureWebBnd(WebBnd webBnd) throws UnableToAdaptException {
            // Nothing
        }

        /** {@inheritDoc} */
        @Override
        public void configureWebExt(WebExt webExt) throws UnableToAdaptException {
            // Nothing
        }

        /** {@inheritDoc} */
        @Override
        public void finish() throws UnableToAdaptException {
            // nothing
        }
    }

}
