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
package com.ibm.ws.security.authentication.internal.cache;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.security.authentication.cache.AuthCacheService;
import com.ibm.ws.security.authentication.cache.RevokeAuthCacheUsers;

/**
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL,
           service = { RevokeAuthCacheUsers.class, DynamicMBean.class },
           immediate = true,
           property = { "service.vendor=IBM", "jmx.objectname=" + RevokeAuthCacheUsers.INSTANCE_NAME })
public class RevokeAuthCacheUsersImpl extends StandardMBean implements RevokeAuthCacheUsers {

    /**
     * @param mbeanInterface
     * @throws NotCompliantMBeanException
     */
    protected RevokeAuthCacheUsersImpl() throws NotCompliantMBeanException {
        super(RevokeAuthCacheUsers.class);
    }

    @Override
    public void removeAllEntries() {
        AuthCacheService auth = new AuthCacheServiceImpl();
        auth.removeAllEntries();
    }

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(BundleContext bContext) throws Exception {
        this.bContext = bContext;
    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate() {
        this.bContext = null;
    }

}
