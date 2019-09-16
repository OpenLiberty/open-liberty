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

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceParent.Event;

@Stateful(name = "NoInterfaceBean2")
// The only view exposed for this bean is the no-interface view.
//  The @Stateful annotation defines this class as a bean.
//
//  The lack of any interface declaration - no @Local, @Remote, nothing about this bean in the XML descriptor,
//  nothing on the 'implements' clause below, etc - cause us to create a NoInterface style view for it.
//
// Do a class level injection of the NoInterfaceBean bean
//  This will result in the NoInterfaceBean getting stored into the component namespace.
//  Because this is a class level injection, it will not get loaded into any instance variable.
//  The 'name' token tells us the location in the component namespace in which to store the bean.
//  The 'beanName' token tells us which bean to store into the namespace...it maps to the name called out in the "@Stateful(name=NoInterfaceBean)" annotation on the NoInterfaceBean
//  The 'beanInterface' tells us the class type of what we are storing in the namespace....and in this case, since we are dealing with the no-interface flavor, its the bean class itself and not some interface.
@EJB(name = "ejb/NoInterfaceBean", beanName = "NoInterfaceBean", beanInterface = NoInterfaceBean.class)
public class NoInterfaceBean2 {
    public final static String CLASSNAME = NoInterfaceBean2.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Static variable for checking if work was done asynchronously **/
    public static boolean asyncWorkDone = false;

    /** Static variable for thread bean is executing on for comparison to caller thread **/
    public static long beanThreadId = 0;

    public static final long MAX_ASYNC_WAIT = 3 * 60 * 1000; // 3 minutes

    // Do an instance level injection of the SessionContext
    // This will cause the SessionContext, which contains the environment for this bean, to get loaded into the variable.
    @Resource
    SessionContext context;

    // Do an instance level injection of the NoInterfaceBean
    // Unlike the class level annotation above, this should actually cause an instance of a NoInterfaceBean wrapper to get stuck into the specified variable.
    //
    // We specify a JNDI lookup key (via the 'name' attribute) that is different from the one specified in the class level annotation, and so I presume this
    // saves us from a naming conflict.  I presume this will result into two different References - both for the same underlying bean type of NoInterfaceBean -
    // getting stuck into the component namespace.
    //
    // This will force us down the EJBLink flow, because the 'name' parm does NOT map to a global binding, and because we do
    // specify the 'beanName'.
    //
    @EJB(name = "ejb/anotherNoInterfaceBean", beanName = "NoInterfaceBean", beanInterface = NoInterfaceBean.class)
    NoInterfaceBean ivReferenceFromEJBLink;

    // This instance variable is used to verify that we can obtain a no-interface style BeanReference from the global namespace.
    //  The 'name' attribute represents the component-namespace-lookup-key.  It maps to an entry in the XML bindings file, which in turn calls out
    //  a global-jndi-lookup-key, which should map to a Factory in the global jndi that will give us the needed no-interface style BeanReference.
    //
    //  The component-namespace-lookup-key is NOT specified in the XML deployment descriptor file...but this is fine, because we specify it via the
    //  'name' attribute on the annotation, which is the functional equivalent.
    @EJB(name = "componentLookupThatMapsToGlobalLookupForNoInterfaceBean")
    NoInterfaceBean ivReferenceFromGlobalNameSpace;

    // This instance variable is used to verify that we can obtain a no-interface style BeanReference via the AutoLink flow, using both a valid beanInterface and a precise variable type.
    //  The 'name' attribute does not map to any global binding, so we don't do a globalMapping flow.
    //  The 'beanName' attribute is omitted, so we don't do an EJBLink flow.
    //  Thus, we default to doing the EJBLink flow.
    @EJB(name = "ejbLinkWithValidBeanInterfaceAndVariableType", beanInterface = NoInterfaceBean.class)
    NoInterfaceBean ivReferenceFromEJBLinkWithValidBeanInterfaceAndPreciseVariableType;

    // This instance variable is used to verify that we can obtain a no-interface style BeanReference via the AutoLink flow, using a valid beanInterface and a generic variable type.
    //  The 'name' attribute does not map to any global binding, so we don't do a globalMapping flow.
    //  The 'beanName' attribute is omitted, so we don't do an EJBLink flow.
    //  Thus, we default to doing the EJBLink flow.
    @EJB(name = "ejbLinkWithValidBeanInterfaceAndGenericVariableType", beanInterface = NoInterfaceBean.class)
    Object ivReferenceFromEJBLinkWithValidBeanInterfaceAndGenericVariableType;

    // This instance variable is used to verify that we can obtain a no-interface style BeanReference via the AutoLink flow, without a beanInterface, but with a precise variable type.
    //  The 'name' attribute does not map to any global binding, so we don't do a globalMapping flow.
    //  The 'beanName' attribute is omitted, so we don't do an EJBLink flow.
    //  Thus, we default to doing the EJBLink flow.
    @EJB(name = "ejbLinkWithoutBeanInterfaceButWithPreciseVariableType")
    NoInterfaceBean ivReferenceFromEJBLinkWithoutBeanInterfaceButWithPreciseVariableType;

    // Variable to hold the list of callbacks
    public ArrayList<Event> ivEvents = new ArrayList<Event>();

    @PostConstruct
    public void postConstructMethod() {
        // Increment the 'counter' variable inherited from the parent to prove we got into this method
        svLogger.info("Executing NoInterfaceBean2.postConstruct method...");
        ivEvents.add(Event.POST_CONSTRUCT);
    }

    @PreDestroy
    public void preDestroyMethod() {
        // Increment the 'counter' variable inherited from the parent to prove we got into this method
        svLogger.info("Executing NoInterfaceBean2.preDestroy method...");
        ivEvents.add(Event.PRE_DESTROY);
    }

    @PrePassivate
    public void prePassivateMethod() {
        // Increment the 'counter' variable inherited from the parent to prove we got into this method
        svLogger.info("Executing NoInterfaceBean2.prePassivate method...");
        ivEvents.add(Event.PRE_PASSIVATE);
    }

    @PostActivate
    public void postActivateMethod() {
        // Increment the 'counter' variable inherited from the parent to prove we got into this method
        svLogger.info("Executing NoInterfaceBean2.postActivate method...");
        ivEvents.add(Event.POST_ACTIVATE);
    }

    @AroundInvoke
    public Object aroundInvokeMethod(InvocationContext ivc) throws Exception {
        svLogger.info("Executing NoInterfaceBean2.aroundInvoke method for invoked API **" + ivc.getMethod().getName() + "**");
        ivEvents.add(Event.AROUND_INVOKE);

        return ivc.proceed();
    }

    @Asynchronous
    public Future<Integer> getBeanFromEJBContextAndUseItToUpdateValue_ReturnFutureResults(int originalValue) throws Exception {
        final String methodName = "getBeanFromEJBContextAndUseItToUpdateValue_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with input value of **" + originalValue + "**");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Lookup the NoInterfaceBean instance from the context
        // The JNDI lookup key that we specify here matches the one that we assigned into the @EJB annotation above.
        // The bean exists in the SessionContext to be found because it got added stuck into the component namespace when the bean was fully initialized, due to the class level annotation.
        NoInterfaceBean beanInstance = (NoInterfaceBean) context.lookup("ejb/NoInterfaceBean");

        // Invoke method on it
        // We send in a value, and the bean method should change that value and give it back to us
        int newValue = beanInstance.methodNOTonAnyInterface(originalValue);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    @Asynchronous
    public Future<Integer> invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults(int originalValue) throws Exception {
        final String methodName = "invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with input value of **" + originalValue + "**");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Invoke method on variable, which should be populated with an instance of a NoInterfaceBean BeanReference, due to the instance level injection.
        // We send in a value, and the bean method should change that value and give it back to us
        int newValue = ivReferenceFromEJBLink.methodNOTonAnyInterface(originalValue);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    // The purpose of this method is to verify that we were actually able to obtain a valid no-interface style BeanReference based on a global binding.
    @Asynchronous
    public Future<Integer> invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults() {
        final String methodName = "invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with NO input parm **");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Invoke the method on the NoInterfaceBean BeanReference.
        // We pass in a value, and if we are actually able to talk to the underlying bean-instance, then the returned value should be incremented by 1...
        // and this will prove that we did obtain a valid BeanReference from the global mapping.
        int newValue = ivReferenceFromGlobalNameSpace.methodNOTonAnyInterface(1);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    // The purpose of this method is to verify that we were actually able to obtain a valid no-interface style BeanReference using the EJBLink flow, and that we can
    // do this when we have a 'beanInterface' and a precise variable type.
    @Asynchronous
    public Future<Integer> invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndPreciseVariableType_ReturnFutureResults() {
        final String methodName = "invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndPreciseVariableType_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with NO input parm **");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Invoke the method on the NoInterfaceBean BeanReference.
        // We pass in a value, and if we are actually able to talk to the underlying bean-instance, then the returned value should be incremented by 1...
        // and this will prove that we did obtain a valid BeanReference using EJBLink, with a beanInterface and a precise variable type.
        int newValue = ivReferenceFromGlobalNameSpace.methodNOTonAnyInterface(1);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    // The purpose of this method is to verify that we were actually able to obtain a valid no-interface style BeanReference using the EJBLink flow, and that we can
    // do this when we have a 'beanInterface' and a generic variable type.
    @Asynchronous
    public Future<Integer> invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndGenericVariableType_ReturnFutureResults() {
        final String methodName = "invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndGenericVariableType_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with NO input parm **");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Cast our instance variable into NoInterfaceBean BeanReference
        NoInterfaceBean beanReference = (NoInterfaceBean) ivReferenceFromEJBLinkWithValidBeanInterfaceAndGenericVariableType;

        // Invoke the method on the NoInterfaceBean BeanReference.
        // We pass in a value, and if we are actually able to talk to the underlying bean-instance, then the returned value should be incremented by 1...
        // and this will prove that we did obtain a valid BeanReference using EJBLink, with a beanInterface and a precise variable type.
        int newValue = beanReference.methodNOTonAnyInterface(1);;

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    // The purpose of this method is to verify that we were actually able to obtain a valid no-interface style BeanReference using the EJBLink flow, and that we can
    // do this when we do not have a 'beanInterface', but we do have a precise variable type.
    @Asynchronous
    public Future<Integer> invokeMethodOnReferenceObtainedViaEJBLinkWithoutBeanInterfaceButWithPreciseVariableType_ReturnFutureResults() {
        final String methodName = "invokeMethodOnReferenceObtainedViaEJBLinkWithoutBeanInterfaceButWithPreciseVariableType_ReturnFutureResults";

        svLogger.info("Executing NoInterfaceBean2." + methodName + " with NO input parm **");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Invoke the method on the NoInterfaceBean BeanReference.
        // We pass in a value, and if we are actually able to talk to the underlying bean-instance, then the returned value should be incremented by 1...
        // and this will prove that we did obtain a valid BeanReference using EJBLink, with a beanInterface and a precise variable type.
        int newValue = ivReferenceFromEJBLinkWithoutBeanInterfaceButWithPreciseVariableType.methodNOTonAnyInterface(1);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the new value
        return new AsyncResult<Integer>(new Integer(newValue));
    }

    // This method should actually be re-named to be .methodOn*NoInterface*ThatTakesNoInterfaceParmAndUpdatesIt....the fact that this method is exposed on a NoInterface wrapper
    // (instead of a Local wrapper) is the only thing the differentiates it from another test, so we should be clear about this in the name to avoid confusion
    @Asynchronous
    public Future<NoInterfaceBean> methodOnNoInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults(NoInterfaceBean noInterfaceBean) {
        final String methodName = "methodOnNoInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults";

        // This method exists so that we can prove a public method not called out any interface, but defined on the parent of the bean, is still reachable via
        // the no-interface view.
        // We update a passed in value and return the new value to prove to the caller that we actually executed the method code.
        svLogger.info("Executing NoInterfaceBean2." + methodName);
        // save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Get the current 'verbiage' value
        String originalVerbiage = noInterfaceBean.getVerbiage();

        // Update it to be some new value
        String newVerbiage = originalVerbiage + originalVerbiage;

        // Set the new value into the passed in bean
        noInterfaceBean.setVerbiage(newVerbiage);

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the updated bean
        return new AsyncResult<NoInterfaceBean>(noInterfaceBean);
    }

    @SuppressWarnings("unchecked")
    @Asynchronous
    public Future<ArrayList<Event>> getCurrentLifecycleCallbackListAndThenResetIt_ReturnFutureResults() {
        svLogger.info("Returning current list of callback records for NoInterfaceBean2 and then clearing it out...");
        // Save threadId value to static variable for verification method executed on different thread
        NoInterfaceBean2.beanThreadId = Thread.currentThread().getId();
        svLogger.info("threadId: " + NoInterfaceBean2.beanThreadId);

        // Make a copy of the current list so that we can return it
        ArrayList<Event> copiedList = (ArrayList<Event>) ivEvents.clone();

        // Clear out the current list so we have a 'clean slate' on which to track the next round of callback calls
        ivEvents.clear();

        // set static variable for work completed to true
        NoInterfaceBean2.asyncWorkDone = true;

        // Return the copied list with the callback records in a Future<V> object
        // The copied ArrayList that we are returning should persist, even though the original in the 'ivEvents' variable has now been wiped out.
        return new AsyncResult<ArrayList<Event>>(copiedList);
    }

    public int methodNOTOnAnyInterface(int originalValue) {
        int newValue = originalValue + 1;
        return newValue;
    }
}