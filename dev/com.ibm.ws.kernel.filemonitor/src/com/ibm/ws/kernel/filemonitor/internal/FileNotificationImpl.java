/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal;

import java.util.Collection;

import javax.management.DynamicMBean;
import javax.management.StandardMBean;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.filemonitor.FileNotification;
import com.ibm.ws.kernel.filemonitor.FileNotificationMBean;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = DynamicMBean.class,
           immediate = true,
           property = { "service.vendor=IBM", "jmx.objectname=WebSphere:service=com.ibm.ws.kernel.filemonitor.FileNotificationMBean" })
public class FileNotificationImpl extends StandardMBean implements FileNotificationMBean, com.ibm.websphere.filemonitor.FileNotificationMBean {

    /** required injected service */
    FileNotification notificationDelegate;

    public FileNotificationImpl() {
        super(FileNotificationMBean.class, false);
    }

    @Reference
    protected void setNotificationDelegate(FileNotification notificationDelegate) {
        this.notificationDelegate = notificationDelegate;
    }

    /** service uninjection method */
    protected void unsetNotificationDelegate(FileNotification notificationDelegate) {
        if (this.notificationDelegate == notificationDelegate)
            this.notificationDelegate = null;
    }

    /** {@inheritDoc} */
    @Override
    public void notifyFileChanges(Collection<String> createdFiles, Collection<String> modifiedFiles, Collection<String> deletedFiles) {
        try {
            notificationDelegate.notifyFileChanges(createdFiles, modifiedFiles, deletedFiles);
        } catch (NullPointerException notExpectingThis) {
            // this will FFDC because we caught it, no need to do anything else
        }
    }

    /** {@inheritDoc} */
    @Override
    public void processConfigurationChanges() {
        notificationDelegate.processConfigurationChanges();
    }

    /** {@inheritDoc} */
    @Override
    public void processApplicationChanges() {
        notificationDelegate.processApplicationChanges();

    }
}
