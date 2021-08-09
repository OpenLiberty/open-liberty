/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.UserTransactionWrapper;
import com.ibm.tx.jta.embeddable.UserTransactionDecorator;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * Implementation of the UserTransactionDecorator interface that allows the EJB Container
 * to override the following two aspects of UserTransaction access in JNDI:
 * 
 * <ol>
 * <li> The UserTransaction interface should not be available to container managed
 * transaction beans. Attempts to lookup UserTransaction from a CMT will result
 * in NameNotFoundException.
 * <li> The UserTransaction implementation that is returned to an EJB must provide a
 * mechanism for the EJB Container to intercept the interface calls, so that it
 * may properly transition the bean to and from the UserTransaction.
 * </ol>
 */
@Component(service = UserTransactionDecorator.class)
public class EJBUserTransactionDecorator implements UserTransactionDecorator {

    @Reference(service = LibertyProcess.class, target = "(wlp.process.type=server)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @Override
    public UserTransaction decorateUserTransaction(UserTransaction ut, boolean injection, Object injectionContext) throws NamingException {
        BeanMetaData bmd = null;
        if (injection) {
            if (injectionContext instanceof InjectionTargetContext) {
                BeanO beanO = ((InjectionTargetContext) injectionContext).getInjectionTargetContextData(BeanO.class);
                if (beanO != null) {
                    bmd = beanO.getHome().getBeanMetaData();
                }
            }
        } else {
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            if (cmd instanceof BeanMetaData) {
                bmd = (BeanMetaData) cmd;
            }
        }

        if (bmd != null) {
            if (bmd.usesBeanManagedTx) {
                return UserTransactionWrapper.INSTANCE;
            }
            J2EEName j2eeName = bmd.getJ2EEName();
            throw new NameNotFoundException("The UserTransaction interface is not available to enterprise beans with container-managed transaction demarcation." +
                                            "The " + j2eeName.getComponent() + " bean in the " + j2eeName.getModule() + " of the " + j2eeName.getApplication() +
                                            " application uses container-managed transactions.");
        }

        return ut;
    }
}
