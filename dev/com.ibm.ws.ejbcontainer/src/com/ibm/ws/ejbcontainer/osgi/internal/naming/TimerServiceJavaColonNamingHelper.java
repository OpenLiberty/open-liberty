/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.naming;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.TimerService;
import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Provides the support for a TimerService to be looked up on a JNDI lookup.
 */
@Component(service = JavaColonNamingHelper.class)
public class TimerServiceJavaColonNamingHelper implements JavaColonNamingHelper {

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    private final static String TIMERSERVICE_NAME = "TimerService";

    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {

        if (JavaColonNamespace.COMP == namespace && TIMERSERVICE_NAME.equals(name) && isTimerServiceActive()) {

            return getTimerService();
        }
        return null;
    }

    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {

        return namespace == JavaColonNamespace.COMP && name.isEmpty() && isTimerServiceActive();
    }

    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {

        if (namespace == JavaColonNamespace.COMP && nameInContext.isEmpty() && isTimerServiceActive()) {
            List<NameClassPair> retVal = Collections.singletonList(
                            new NameClassPair(TIMERSERVICE_NAME, TimerService.class.getName()));

            return retVal;
        }

        return Collections.emptyList();

    }

    protected boolean isTimerServiceActive() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        return cmd instanceof BeanMetaData;
    }

    protected TimerService getTimerService() {
        return EJSContainer.getCallbackBeanO();
    }

}
