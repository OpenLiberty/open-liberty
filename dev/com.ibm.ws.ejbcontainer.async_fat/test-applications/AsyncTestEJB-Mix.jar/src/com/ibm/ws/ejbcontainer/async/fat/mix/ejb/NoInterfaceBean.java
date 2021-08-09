/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.async.fat.mix.ejb;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;

@Stateful(name = "NoInterfaceBean")
// This bean has no-interface and local-business-interface views exposed
@LocalBean
@Local(LocalInterfaceForNoInterfaceBean.class)
public class NoInterfaceBean extends NoInterfaceParent {
    private final static Logger svLogger = Logger.getLogger(NoInterfaceBean.class.getName());

    // Inject the context for the bean
    @Resource
    SessionContext context;

    @EJB(name = "componentReferenceForNoInterfaceBean4", beanName = "NoInterfaceBean4", beanInterface = NoInterfaceBean4.class)
    NoInterfaceBean4 ivNoInterfaceBean4;

    // Variable to store random text, which will be used to verify we can pass the bean instance around between method calls
    String verbiage = "";

    // This method exists to prove that we can call stuff on a local-interface view, even when there is also a no-interface view defined...and it also
    // gives us something to call out on the local-interface definition, so we can have one.
    public void methodOnLocalInterface() {
        // Update the 'lastAction' flag to indicate to prove we were actually able to
        svLogger.info("Executing NoInterfaceBean.publicMethodOnLocalInterface() API...");
    }

    public int methodNOTonAnyInterface(int originalValue) {
        // This method exists so that we can prove a public method not called out any interface is still reachable via the no-interface view.
        //  We update a passed in value and return the new value to prove to the caller that we actually executed the method code.
        svLogger.info("Executing NoInterfaceBean.methodNOTonAnyInterface() with input value of **" + originalValue + "**");

        // Update the original value and return it
        int newValue = originalValue + 1;
        return newValue;
    }

    @Asynchronous
    public Future<Integer> invokeMethodOnNoInterfaceStyleBeanReference_ReturnFutureResults() {
        final String methodName = "invokeMethodOnNoInterfaceStyleBeanReference_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean." + methodName + " with NO input parm **");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceParent.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceParent.beanThreadId);

        // The point here is to verify that even after being passivated and then re-activated, this bean still maintains a
        // valid no-interface style BeanReference.
        //
        // The passed in '1' will get incremented to be '2' and returned, which will be our proof that the BeanReference
        // was actually valid.
        int newValue = ivNoInterfaceBean4.methodNotExposedOnInterface(1);

        // set static variable for work completed to true
        NoInterfaceParent.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    @TransactionAttribute(REQUIRES_NEW)
    public void setVerbiage(String text) {
        verbiage = text;
    }

    @TransactionAttribute(REQUIRES_NEW)
    public String getVerbiage() {
        return verbiage;
    }

    // Yes, we actually need this...the real local interface calls out this method, and the point is to prove that we can pass in a NoInterface instance
    // as a param to a local interface wrapper....
    @Asynchronous
    public Future<NoInterfaceBean> methodOnLocalInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults(NoInterfaceBean noInterfaceBean) {
        final String methodName = "methodOnLocalInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults";

        // This method exists so that we can prove a public method not called out any interface, but defined on the parent of the bean, is still reachable via
        // the no-interface view.
        // We update a passed in value and return the new value to prove to the caller that we actually executed the method code.
        svLogger.info("Executing NoInterfaceBean." + methodName);
        // save threadId value to static variable for verification method executed on different thread
        NoInterfaceParent.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceParent.beanThreadId);

        // Get the current 'verbiage' value
        String originalVerbiage = noInterfaceBean.getVerbiage();

        // Update it to be some new value
        String newVerbiage = originalVerbiage + originalVerbiage;

        // Set the new value into the passed in bean
        noInterfaceBean.setVerbiage(newVerbiage);

        // set static variable for work completed to true
        NoInterfaceParent.asyncWorkDone = true;

        // Return the updated bean
        return new AsyncResult<NoInterfaceBean>(noInterfaceBean);
    }
}