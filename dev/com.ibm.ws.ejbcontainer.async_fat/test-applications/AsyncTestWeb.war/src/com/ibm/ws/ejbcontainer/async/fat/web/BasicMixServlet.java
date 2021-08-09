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
package com.ibm.ws.ejbcontainer.async.fat.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.BasicMixedLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.BasicSingletonMixedBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.BasicStatefulMixedBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.BasicStatelessMixedBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceAnnLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceSingletonAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceSingletonXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatefulAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatefulXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatelessAnnBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceStatelessXMLBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.InheritanceXMLLocal;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.LocalInterfaceForNoInterfaceBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceBean;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceBean2;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceBean3;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceParent;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb.NoInterfaceParent.Event;
import com.ibm.ws.ejbcontainer.async.fat.mix.ejb30.Basic30Bean;

import componenttest.app.FATServlet;

/**
 * Tests the basic fire-and-forget async method behavior for singleton,
 * stateful, stateless, and noInterface session beans. <p>
 *
 * This test has been derived from different suites in the r80-asynch FAT
 * bucket originally implemented for traditional WAS : noInterface.
 */
@WebServlet("/BasicMixServlet")
@SuppressWarnings("serial")
public class BasicMixServlet extends FATServlet {
    public final static String CLASS_NAME = BasicMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB(beanName = "BasicSingletonMixedBean")
    BasicMixedLocal basicSingletonMixedBean;

    @EJB(beanName = "InheritanceSingletonAnnBean")
    InheritanceAnnLocal inheritanceSingletonAnnBean;

    @EJB(beanName = "InheritanceSingletonXMLBean")
    InheritanceXMLLocal inheritanceSingletonXMLBean;

    @EJB(beanName = "BasicStatefulMixedBean")
    BasicMixedLocal basicStatefulMixedBean;

    @EJB(beanName = "InheritanceStatefulAnnBean")
    InheritanceAnnLocal inheritanceStatefulAnnBean;

    @EJB(beanName = "InheritanceStatefulXMLBean")
    InheritanceXMLLocal inheritanceStatefulXMLBean;

    @EJB(beanName = "BasicStatelessMixedBean")
    BasicMixedLocal basicStatelessMixedBean;

    @EJB(beanName = "InheritanceStatelessAnnBean")
    InheritanceAnnLocal inheritanceStatelessAnnBean;

    @EJB(beanName = "InheritanceStatelessXMLBean")
    InheritanceXMLLocal inheritanceStatelessXMLBean;

    private BasicMixedLocal lookupBasicSingletonMixedBean() throws Exception {
        return basicSingletonMixedBean;
    }

    private InheritanceAnnLocal lookupInheritanceSingletonAnnLocalBean() throws Exception {
        return inheritanceSingletonAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceSingletonXMLLocalBean() throws Exception {
        return inheritanceSingletonXMLBean;
    }

    private BasicMixedLocal lookupBasicStatefulMixedBean() throws Exception {
        return basicStatefulMixedBean;
    }

    private InheritanceAnnLocal lookupInheritanceStatefulAnnLocalBean() throws Exception {
        return inheritanceStatefulAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceStatefulXMLLocalBean() throws Exception {
        return inheritanceStatefulXMLBean;
    }

    private BasicMixedLocal lookupBasicStatelessMixedBean() throws Exception {
        return basicStatelessMixedBean;
    }

    private InheritanceAnnLocal lookupInheritanceStatelessAnnLocalBean() throws Exception {
        return inheritanceStatelessAnnBean;
    }

    private InheritanceXMLLocal lookupInheritanceStatelessXMLLocalBean() throws Exception {
        return inheritanceStatelessXMLBean;
    }

    private final String VERBIAGE = "blah";

    // Constants used by test
    private final String Application = "AsyncTestApp";
    private final String Module = "AsyncTestEJB-Mix";
    private final String Module30 = "AsyncTestEJB30-Mix";

    // Get the NoInterfaceBean from the java:global namespace
    private NoInterfaceBean lookupNoInterfaceBeanFromEJBLocalNamespace() throws Exception {
        return (NoInterfaceBean) FATHelper.lookupDefaultBindingEJBJavaGlobal(NoInterfaceBean.class.getName(), Application, Module, "NoInterfaceBean");
    }

    // Get the NoInterfaceBean2 from the java:global namespace
    private NoInterfaceBean2 lookupNoInterfaceBean2FromEJBLocalNamespace() throws Exception {
        return (NoInterfaceBean2) FATHelper.lookupDefaultBindingEJBJavaGlobal(NoInterfaceBean2.class.getName(), Application, Module, "NoInterfaceBean2");
    }

    // Get the NoInterfaceBean3 from the java:global namespace
    private NoInterfaceBean3 lookupNoInterfaceBean3FromEJBLocalNamespace() throws Exception {
        return (NoInterfaceBean3) FATHelper.lookupDefaultBindingEJBJavaGlobal(NoInterfaceBean3.class.getName(), Application, Module, "NoInterfaceBean3");
    }

    // Get the LocalInterfaceForNoInterfaceBean from the java:global namespace
    private LocalInterfaceForNoInterfaceBean lookupLocalInterfaceForNoInterfaceBeanFromEJBLocalNamespace() throws Exception {
        return (LocalInterfaceForNoInterfaceBean) FATHelper.lookupDefaultBindingEJBJavaGlobal(LocalInterfaceForNoInterfaceBean.class.getName(), Application, Module,
                                                                                              "NoInterfaceBean");
    }

    // Get the Basic30Bean from the java:global namespace
    private Basic30Bean lookupBasic30BeanFromEJBLocalNamespace() throws Exception {
        return (Basic30Bean) FATHelper.lookupDefaultBindingEJBJavaGlobal(Basic30Bean.class.getName(), Application, Module30, "Basic30Bean");
    }

    // Verify that the correct entry shows up in the correct place in the list
    private void verifyCorrectCallbackEntryShowsUpInCorrectPlaceInTheList(ArrayList<Event> lifecycleCallbackHistory, int elementToGet, Event expectedValue) throws Exception {
        // Validate ArrayList is not null
        if (lifecycleCallbackHistory == null) {
            fail("Unable to verify the lifecycle callback history.  The specified ArrayList was null.");
        }

        // Validate array list has the specified element in it
        // Elements are numbered from 0, but the size is numbered from 1, so we need to increment the 'elementToGet' value by 1 when comparing with size.
        // For example:  An array with 4 elements has a size of '4', but the last element is numbered '3'
        int sizeMustBeAtLeastThisBig = elementToGet + 1;
        int actualSize = lifecycleCallbackHistory.size();
        if (actualSize < sizeMustBeAtLeastThisBig) {
            fail("Unable to verify lifcycle callback history.  The expected element did not exist in the list. We need element **" + elementToGet
                 + "**, but the list only has a size of **" + actualSize + "**");
        }

        // Inspect the contents of the element to see if it actually matches what we are looking for
        Event actualValue = lifecycleCallbackHistory.get(elementToGet);
        assertSame("The callback entry was not what we expected.", actualValue, expectedValue);
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the class level, test invoking a public method on a no-interface
     * view that is defined on the bean parent, and that does not show up in the local or remote interfaces.
     */
    @Test
    public void testPublicMethodDefinedOnParentOfBeanInvokableViaNoInterfaceView() throws Exception {
        long currentThreadId = 0;

        // Lookup bean instance
        NoInterfaceBean beanInstance = lookupNoInterfaceBeanFromEJBLocalNamespace();

        // Invoke a asynchronous public method on the no-interface view that is defined on a parent of the bean.
        // Again, this method doesn't exist on either the remote or local interfaces...so if we can invoke it, we must be
        // getting at it via the no-interface view.
        beanInstance.public_FireAndForget_MethodOnParentClassButNotOnInterfaces();

        // wait for async work to complete
        NoInterfaceBean.svBeanLatch.await(NoInterfaceBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean.beanThreadId);

        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean.asyncWorkDone = false;
        NoInterfaceBean.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level, test invoking a public method on a no-interface
     * view, that we can explicitly invoke the following lifecycle callback methods:
     * a) postConstruct
     * b) preDestroy
     * c) postActivate
     * d) prePassivate
     */
    @Test
    public void testPublicLifecycleMethodsDefinedOnBeanAndParentInvokableViaNoInterfaceView() throws Exception {
        long currentThreadId = 0;
        Future<ArrayList<Event>> future = null;

        // Define variables to hold current lifecycle callback records
        ArrayList<Event> lifecycleCallbackHistory = new ArrayList<Event>();

        // Lookup bean instance
        // We use NoInterfaceBean2 (instead of NoInterfaceBean) to make our lives easier.
        //
        // The NoInterfaceBean should be configured with an 'activation strategy' of 'transaction', which means every time an explicit business method invocation occurs
        // (such as the method call to get the contents of the callback data, or the invocation of the callback methods themselves), we are going to generate three
        // callback events....the 'aroundInvoke' before the method executes, the 'prePassivate' after the method is done executing, and another 'postActivate' when
        // the bean is re-activated for the next business method invocation.  So, the net is that it would be possible for us to use the NoInterfaceBean for this test...
        // but that would mean we'd have to sift through more callback records.
        //
        // To make things simpler, we use the NoInterfaceBean2, which should be configured to have an 'activation strategy' of 'none', which means it
        // should not get passivated after each business method call (and thus not need to get re-activated either), which should eliminate some of the
        // callback entries.
        NoInterfaceBean2 beanInstance2 = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Invoke the postConstruct lifecycle callback method, and then verify we have the correct callback history
        // For the series of lifecycle methods we are invoking here, they are all defined on the bean class or the
        // bean's parent class, and then take no argument.  If they were defined in a separate interceptor class, then
        // they would need to take an InvocationContext argument.
        //
        // When we invoke each of these lifecycle methods, the methods should update an internal list, which we then
        // check to ensure it is set to the proper value.
        //
        // So, the theory is that we know the method was successfully invoked and executed because:
        // 1) we didn't get an exception back
        // 2) our internal list has the correct data in the correct place, which means things must have behaved as expected, including actually
        //    running the method
        //
        // We should have the following callbacks in our list, in this order, after this invocation:
        // 1) PostConstruct......from the original construction of the bean instance, as a result of the JNID lookup above
        // 2) AroundInvoke.......from the explicit invocation of a method on the bean (in this case, the PostConstruct method)
        // 3) PostConstruct......from our explicit invocation of the PostConstruct method
        //
        // Notes:
        // 1) There should be another AroundInvoke at the end of these lists as a result of us calling the API to get the list back, but we aren't
        //    accounting for that because its not directly related to the function we are trying to test here, and we don't have to get past that list
        //    entry to reach the list entry directly related to the function we are trying to test.
        //
        // 2) The API brings back the current list also wipes it out, so when we invoke our next business method, we should be starting with a clean slate again.
        svLogger.info("Invoking the postConstruct lifecycle callback method.");
        beanInstance2.postConstructMethod();
        // call bean asynchronous method using Future<V> object to receive results
        future = beanInstance2.getCurrentLifecycleCallbackListAndThenResetIt_ReturnFutureResults();
        lifecycleCallbackHistory = future.get();
        verifyCorrectCallbackEntryShowsUpInCorrectPlaceInTheList(lifecycleCallbackHistory, 2, Event.POST_CONSTRUCT);

        // Invoke the PreDestroy lifecycle callback method
        // We expect the following callbacks to be in the list that gets returned:
        // 1) AroundInvoke....from our explicit invocation of a business method
        // 2) PreDestroy......from our explicit invocation of the preDestroy method
        svLogger.info("Invoking the preDestroy lifecycle callback method...");
        beanInstance2.preDestroyMethod();
        // call bean asynchronous method using Future<V> object to receive results
        future = beanInstance2.getCurrentLifecycleCallbackListAndThenResetIt_ReturnFutureResults();
        lifecycleCallbackHistory = future.get();
        verifyCorrectCallbackEntryShowsUpInCorrectPlaceInTheList(lifecycleCallbackHistory, 1, Event.PRE_DESTROY);

        // Invoke the PrePassivate lifecycle callback method
        // We expect the following callbacks to be in the list that gets returned:
        // 1) AroundInvoke....from our explicit invocation of a business method
        // 2) PrePassivate......from our explicit invocation of the prePassivate method
        svLogger.info("Invoking the prePassivate lifecycle callback method...");
        beanInstance2.prePassivateMethod();
        // call bean asynchronous method using Future<V> object to receive results
        future = beanInstance2.getCurrentLifecycleCallbackListAndThenResetIt_ReturnFutureResults();
        lifecycleCallbackHistory = future.get();
        verifyCorrectCallbackEntryShowsUpInCorrectPlaceInTheList(lifecycleCallbackHistory, 1, Event.PRE_PASSIVATE);

        // Invoke the PostActivate lifecycle callback method
        // We expect the following callbacks to be in the list that gets returned:
        // 1) AroundInvoke....from our explicit invocation of a business method
        // 2) PostActivate......from our explicit invocation of the postActivate method
        svLogger.info("Invoking the postActivate lifecycle callback method...");
        beanInstance2.postActivateMethod();
        // call bean asynchronous method using Future<V> object to receive results
        future = beanInstance2.getCurrentLifecycleCallbackListAndThenResetIt_ReturnFutureResults();
        lifecycleCallbackHistory = future.get();
        verifyCorrectCallbackEntryShowsUpInCorrectPlaceInTheList(lifecycleCallbackHistory, 1, Event.POST_ACTIVATE);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * Verify that we can lookup a no-interface instance using the SessionContext
     */
    @Test
    public void testNoInterfaceCanBeLookedUpFromEJBContext() throws Exception {
        long currentThreadId = 0;

        // Lookup bean instance
        NoInterfaceBean2 beanInstance2 = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Lookup a NoInterfaceBean from the SessionContext and invoke method on it
        // The NoInterfaceBean2 that we are using here does a class level injection of the NoInterfaceBean, which should cause the NoInterfaceBean
        // to get added to the component namespace for the NoInterfaceBean2 bean.
        //
        // Thus, the BeanInterface2 method can get its SessionContext, and do a lookup on that SessionContext and find the NoInterfaceBean (because it exists
        // in the component namespace).
        //
        // This method will use the SessionContext to get an instance of the NoInterfaceBean, and then send the passed in value into a method on that bean,
        // which should update it and return the new value.
        //
        // Finally, back here in the test, we can verify that the returned value has changed....which proves we got into the NoInterfaceBean method that changes it...
        // which proves that we were able to lookup the NoInterfaceBean from the SessionContext associated with the NoInterfaceBean2.
        Future<Integer> future = beanInstance2.getBeanFromEJBContextAndUseItToUpdateValue_ReturnFutureResults(1);
        int updatedValue = future.get().intValue();

        // Verify the method was actually run
        assertEquals("Failed to lookup a no-interface bean from the SessionContext and execute a method on it.", 2, updatedValue);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can obtain a no-interface style BeanReference via injection.
     * 2) Verify that we can obtain a no-interface style BeanReference via the EJBLink flow.
     */
    @Test
    public void testNoInterfaceCanBeObtainedViaInjection() throws Exception {
        long currentThreadId = 0;

        // Lookup bean instance
        NoInterfaceBean2 beanInstance2 = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Use the NoInterfaceBean obtained via injection and invoke a method on it
        // The NoInterfaceBean2 that we are using here does an instance level injection of the NoInterfaceBean, and so the NoInterfaceBean2 instance should
        // have an instance variable that points to a NoInterfaceBean instance.
        //
        // We invoke a method on that NoInterfaceBean instance, which should change the passed in value, and send back the updated value, which is then
        // returned back to this test.
        //
        // If we get the updated value, then that means we executed the method on NoInterfaceBean...and that means we were able to successfully inject the bean.
        Future<Integer> future = beanInstance2.invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults(1);
        int updatedValue = future.get().intValue();

        // Verify the method was actually run
        assertEquals("Failed to obtain a no-interface bean via injection and execute a method on it.", 2, updatedValue);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can pass a no-interface instance as a param to a method on a Local interface.
     * 2) Verify that we can return a no-interface instance from a method on a Local interface.
     */
    @Test
    public void testNoInterfaceInstanceCanBePassedAndReturnedFromMethodOnLocalInterface() throws Exception {
        long currentThreadId = 0;

        // Get both the no-interface wrapper and local-business-interface wrappers
        // Both of these wrappers point to underlying instances of the NoInterfaceBean type.
        LocalInterfaceForNoInterfaceBean localBusinessInstance = lookupLocalInterfaceForNoInterfaceBeanFromEJBLocalNamespace();
        NoInterfaceBean noInterfaceInstance = lookupNoInterfaceBeanFromEJBLocalNamespace();

        // Set the initial 'switch' value on the NoInterfaceBean.
        // This will get updated as part of the .updateDataOnNoInterfaceBean() method we are about to invoke.
        noInterfaceInstance.setVerbiage(VERBIAGE);

        // Invoke method on Local interface, passing in NoInterfaceBean instance as a parameter
        // The point here is to verify that we can successfully pass in a no-interface instance as a param to a method called out on a local-business-interface,
        // and also that we can return a no-interface instance from a method called out on a local-business-interface.
        //
        // The method is going to update the instance variable on the passed in no-interface instance, and then return that no-interface instance.
        // The assumption is that if we aren't allowed to pass or return the no-interface instance, then we'll get an exception...and if we can't actually
        // use the no-interface instance from inside of the method, then we won't see the expected update on it.
        Future<NoInterfaceBean> future = localBusinessInstance.methodOnLocalInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults(noInterfaceInstance);
        noInterfaceInstance = future.get();

        // Retrieve the 'switch' from the no-interface bean, and verify its been updated
        // The bean should have simply repeated the originally specified verbiage to come up with the new value that it assigned.
        String updatedVerbiage = noInterfaceInstance.getVerbiage();
        assertEquals("The no-interface instance was not successfully passed and returned from a local-interface method.", updatedVerbiage, VERBIAGE + VERBIAGE);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceParent.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceParent.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceParent.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceParent.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceParent.asyncWorkDone = false;
        NoInterfaceParent.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can pass a no-interface instance as a param to a method on a no-interface.
     * 2) Verify that we can return a no-interface instance from a method on a no-interface.
     */
    @Test
    public void testNoInterfaceInstanceCanBePassedAndReturnedFromMethodOnNoInterface() throws Exception {
        long currentThreadId = 0;

        // Get no-interface wrappers to both beans
        NoInterfaceBean noInterfaceBean = lookupNoInterfaceBeanFromEJBLocalNamespace();
        NoInterfaceBean2 noInterfaceBean2 = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Set the initial 'switch' value on the NoInterfaceBean.
        // This will get updated as part of the .updateDataOnNoInterfaceBean() method we are about to invoke.
        noInterfaceBean.setVerbiage(VERBIAGE);

        // Invoke method on the NoInterfaceBean2 bean, passing in the NoInterfaceBean bean as the param
        // Again, as with the previous test, the idea is to verify that we can both pass and return a no-interface instance...and again, we verifying this
        // by setting an original value on our no-interface bean, passing it into a method that should update that value on the bean and then returning the
        // updated bean, and then inspecting the bean to make sure the update really did occur.
        //
        // The only difference between this test and the previous one is that this time we are using a method exposed through a no-interface view, as oppossed to one exposed through the local-business-interface view.
        Future<NoInterfaceBean> future = noInterfaceBean2.methodOnNoInterfaceThatTakesNoInterfaceParamAndUpdatesIt_ReturnFutureResults(noInterfaceBean);
        noInterfaceBean = future.get();

        // Retrieve the 'switch' from the no-interface bean, and verify its been updated
        String updatedVerbiage = noInterfaceBean.getVerbiage();
        assertEquals("The no-interface instance was not successfully passed and returned from a local-interface method.", updatedVerbiage, VERBIAGE + VERBIAGE);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can obtain a no-interface style BeanReference via a component-lookup-key---TO---global-lookup-key mapping.
     */
    @Test
    public void testCanObtainNoInterfaceReferenceViaGlobalMapping() throws Exception {
        long currentThreadId = 0;

        // Get a NoInterfaceBean2 BeanReference
        NoInterfaceBean2 bean = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Exercise the no-interface style BeanReference that should have been obtained via the globalMapping flow
        // The NoInterfaceBean2 bean instance should have been injected with a NoInterfaceBean BeanReference, via the globalMapping flow.
        //
        // This method will invoke a method on that NoInterfaceBean BeanReference, which should update the passed in value (1) and return a new value (2).
        Future<Integer> future = bean.invokeMethodOnReferenceObtainedViaGlobalMapping_ReturnFutureResults();
        int result = future.get().intValue();

        // Verify that the BeanReference was valid
        assertEquals("Checking to see if we obtain a valid no-interface style BeanReference via the globalMapping flow...", result, 2);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can obtain a no-interface style BeanReference via the EJBLink flow.
     * 2) Verify that we can use EJBLink flow when specifying both a beanInterface, and a precise variable type.
     */
    @Test
    public void testCanObtainNoInterfaceReferenceViaEJBLinkWithBeanInterfaceAndSpecificVariableType() throws Exception {
        long currentThreadId = 0;

        // Get a NoInterfaceBean2 BeanReference
        NoInterfaceBean2 bean = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Exercise the no-interface style BeanReference that should have been obtained via the EJBLink flow
        // The NoInterfaceBean2 bean instance should have been injected with a NoInterfaceBean BeanReference, via the EJBLink flow, with both a
        // valid beanInterface and a precise variable type used in the annotation.
        //
        // This method will invoke a method on that NoInterfaceBean BeanReference, which should update the passed in value (1) and return a new value (2).
        Future<Integer> future = bean.invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndPreciseVariableType_ReturnFutureResults();
        int result = future.get().intValue();

        // Verify that the BeanReference was valid
        assertEquals("Checking to see if we obtain a valid no-interface style BeanReference via the EJBLink flow, with a beanInterface and a precise variable type", result, 2);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can obtain a no-interface style BeanReference via the EJBLink flow.
     * 2) Verify that we can use EJBLink flow when specifying a valid beanInterface, and a generic variable type.
     */
    @Test
    public void testCanObtainNoInterfaceReferenceViaEJBLinkWithBeanInterfaceAndGenericVariableType() throws Exception {
        long currentThreadId = 0;

        // Get a NoInterfaceBean2 BeanReference
        NoInterfaceBean2 bean = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Exercise the no-interface style BeanReference that should have been obtained via the EJBLink flow
        // The NoInterfaceBean2 bean instance should have been injected with a NoInterfaceBean BeanReference, via the EJBLink flow, with a
        // valid beanInterface and a generic variable type used in the annotation.
        //
        // This method will invoke a method on that NoInterfaceBean BeanReference, which should update the passed in value (1) and return a new value (2).
        Future<Integer> future = bean.invokeMethodOnReferenceObtainedViaEJBLinkWithValidBeanInterfaceAndGenericVariableType_ReturnFutureResults();
        int result = future.get().intValue();

        // Verify that the BeanReference was valid
        assertEquals("Checking to see if we obtain a valid no-interface style BeanReference via the EJBLink flow, with a beanInterface and a generic variable type", result, 2);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that we can obtain a no-interface style BeanReference via the EJBLink flow.
     * 2) Verify that we can use EJBLink flow without specifying a valid beanInterface, but a generic variable type.
     */
    @Test
    public void testCanObtainNoInterfaceReferenceViaEJBLinkWithoutBeanInterfaceButWithPreciseVariableType() throws Exception {
        long currentThreadId = 0;

        // Get a NoInterfaceBean2 BeanReference
        NoInterfaceBean2 bean = lookupNoInterfaceBean2FromEJBLocalNamespace();

        // Exercise the no-interface style BeanReference that should have been obtained via the EJBLink flow
        // The NoInterfaceBean2 bean instance should have been injected with a NoInterfaceBean BeanReference, via the EJBLink flow, without a
        // beanInterface, but with a precise variable type used in the annotation.
        //
        // This method will invoke a method on that NoInterfaceBean BeanReference, which should update the passed in value (1) and return a new value (2).
        Future<Integer> future = bean.invokeMethodOnReferenceObtainedViaEJBLinkWithoutBeanInterfaceButWithPreciseVariableType_ReturnFutureResults();
        int result = future.get().intValue();

        // Verify that the BeanReference was valid
        assertEquals("Checking to see if we obtain a valid no-interface style BeanReference via the EJBLink flow, without a beanInterface, but with a precise variable type",
                     result, 2);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean2.asyncWorkDone);
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean2.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean2.beanThreadId);
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean2.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean2.asyncWorkDone = false;
        NoInterfaceBean2.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that you can invoke an asynchronous method that has been exposed via a no-interface style BeanReference.
     */
    @Test
    public void testInvokeAsynchronousMethodExposedOnNoInterfaceView() throws Exception {
        long currentThreadId = 0;

        // Get a no-interface style BeanReference to NoInterfaceBean3
        NoInterfaceBean3 bean3 = lookupNoInterfaceBean3FromEJBLocalNamespace();

        // Invoke an asynchronous method
        // The method has been tagged with @Asynchronous annotation, so the container should invoke it asynchronously.
        bean3.test_fireAndForget();

        // wait for async work to complete
        NoInterfaceBean3.svBeanLatch.await(NoInterfaceBean3.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceBean3.asyncWorkDone);
        // update results for asynchronous work done
        assertTrue("Async NoInterface Bean method completed", NoInterfaceBean3.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();

        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceBean3.beanThreadId);

        // update results with current and bean method thread id comparison
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceBean3.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceBean3.asyncWorkDone = false;
        NoInterfaceBean3.beanThreadId = 0;
    }

    /**
     * (3.4.4) Session Bean's No-Interface View
     * (3.4.8) Asynchronous Invocations
     * With an Asynchronous annotation at the method level:
     * 1) Verify that when we have a stateful bean, and that bean contains a no-interface style BeanReference, and that
     * stateful bean is passivated and then re-activated, that the no-interface style BeanReference is still usable.
     */
    @Test
    public void testStatefulBeanCanStillUseNoInterfaceReferenceEvenAfterPassivationAndReActivation() throws Exception {
        long currentThreadId = 0;

        // The NoInterfaceBean is configured to have an activationPolicy of 'transaction'.
        //
        // Since we are not in an explicitly started transaction, this means the container will start and end a transaction
        // for us under the covers...and since we have an activationPolicy of 'transaction', this means the bean will
        // get passivated after the completion of the method, and will get re-activated when we invoke the next method on it.
        //
        // The NoInterfaceBean has an injected no-interface style BeanReference, and so if this BeanReference is still
        // usable after the passivation/re-activation, then we have the behavior we want.

        // Get a no-interface style BeanReference to NoInterfaceBean
        NoInterfaceBean bean = lookupNoInterfaceBeanFromEJBLocalNamespace();

        // Invoke a method on the bean
        // The only point here is to force the passivation of the bean after the method completes, which will then
        // force a re-activation before the next method executes.
        bean.methodNOTonAnyInterface(1);

        // Invoke a 2nd method on the bean.
        // This should force the bean to get re-activated.
        //
        // This method will use the no-interface style BeanReference, and thus verify its still valid.
        Future<Integer> future = bean.invokeMethodOnNoInterfaceStyleBeanReference_ReturnFutureResults();
        int result = future.get().intValue();

        // Verify the result
        // The method on the NoInterfaceBean that we called passes in a value of 1 to the method it calls on the 2nd
        // bean...and that 2nd bean should increment the value to 2.
        assertEquals("Checking to see if the no-interface style BeanReference was still valid, even after passivation and re-activation...", result, 2);

        svLogger.info("Asynchronous method work completed: " + NoInterfaceParent.asyncWorkDone);
        // update results for asynchronous work done
        assertTrue("Async NoInterface Bean method completed", NoInterfaceParent.asyncWorkDone);

        // get current thread Id for comparison to bean method thread id
        currentThreadId = Thread.currentThread().getId();
        svLogger.info("Test threadId = " + currentThreadId);
        svLogger.info("Bean threadId = " + NoInterfaceParent.beanThreadId);
        // update results with current and bean method thread id comparison
        assertFalse("Async NoInterface Bean method completed on separate thread", (NoInterfaceParent.beanThreadId == currentThreadId));

        // Reset the NoInterfaceParent static variables
        NoInterfaceParent.asyncWorkDone = false;
        NoInterfaceParent.beanThreadId = 0;
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 1 XML defining all methods to be asynchronous.
     *
     * Scenario: Bean class A defines method m() and annotates it with an
     * Asynchronous annotation. The bean's deployment descriptor uses Style 1
     * XML (i.e. * for method-name) to define all methods of A to be
     * asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulComboAnnMethod_XMLStyle1() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceStatefulXMLLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatefulXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous method level annotation plus Style1 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceStatefulXMLBean.svInheritanceLatch.await(InheritanceStatefulXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulXMLBean.asyncMethThreadId = " + InheritanceStatefulXMLBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceStatefulXMLBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatefulXMLBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a class level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A has a class level Asynchronous annotation and
     * defines a method m(). The bean's deployment descriptor uses Style 2 XML
     * to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulComboAnnClass_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceStatefulAnnLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatefulAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous class level annotation plus Style2 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceStatefulAnnBean.svInheritanceLatch.await(InheritanceStatefulAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatefulAnnBean.asyncMethThreadId = " + InheritanceStatefulAnnBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceStatefulAnnBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatefulAnnBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulComboAnnMeth_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatefulMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatefulMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnWithStyle2XML("Rolling Stones");

        // Wait for test_asyncMethAnnWithStyle2XML() to complete
        BasicStatefulMixedBean.svBasicLatch.await(BasicStatefulMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulMixedBean.asyncMethThreadId = " + BasicStatefulMixedBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMethThreadId = " + BasicStatefulMixedBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatefulMixedBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method just has a method level Asynchronous
     * annotation, yet other methods of the bean have Style 2 XML defining them
     * to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define other methods, NOT method m(), to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulAnnMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatefulMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatefulMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnOnly("The Beatles");

        // Wait for test_asyncMethAnnOnly() to complete
        BasicStatefulMixedBean.svBasicLatch.await(BasicStatefulMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulMixedBean.asyncMeth2ThreadId = " + BasicStatefulMixedBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + BasicStatefulMixedBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatefulMixedBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has NO method level Asynchronous
     * annotation, yet does have Style 2 XML defining it to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which does NOT have a method
     * level Asynchronous annotation. The bean's deployment descriptor uses
     * Style 2 XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulStyle2XMLMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatefulMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatefulMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethStyle2XMLOnly("Led Zeppelin");

        // Wait for test_asyncMethStyle2XMLOnly() to complete
        BasicStatefulMixedBean.svBasicLatch.await(BasicStatefulMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulMixedBean.asyncMeth3ThreadId = " + BasicStatefulMixedBean.asyncMeth3ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth3ThreadId = " + BasicStatefulMixedBean.asyncMeth3ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatefulMixedBean.asyncMeth3ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method m() is intended to be synchronous
     * (i.e. no async annotations or XML), yet other methods have explicitly
     * been defined to be asynchronous either by annotations or XML.
     *
     * Scenario: Bean class A defines a method m() which has no asynchronous
     * annotations or XML associated to it. However other methods on the bean
     * do have explicit asynchronous annotations or XML.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatefulSynchronousMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatefulMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatefulMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_syncMethod("The Who");

        // Wait for test_syncMethod() to complete
        BasicStatefulMixedBean.svBasicLatch.await(BasicStatefulMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatefulMixedBean.syncMethThreadId = " + BasicStatefulMixedBean.syncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread,
        // they should be the same since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + BasicStatefulMixedBean.syncMethThreadId
                     + ", currentThreadId = " + currentThreadId, BasicStatefulMixedBean.syncMethThreadId, currentThreadId);
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 1 XML defining all methods to be asynchronous.
     *
     * Scenario: Bean class A defines method m() and annotates it with an
     * Asynchronous annotation. The bean's deployment descriptor uses Style 1
     * XML (i.e. * for method-name) to define all methods of A to be
     * asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonComboAnnMethod_XMLStyle1() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceSingletonXMLLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceSingletonXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous method level annotation plus Style1 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceSingletonXMLBean.svInheritanceLatch.await(InheritanceSingletonXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonXMLBean.asyncMethThreadId = " + InheritanceSingletonXMLBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceSingletonXMLBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceSingletonXMLBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a class level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A has a class level Asynchronous annotation and
     * defines a method m(). The bean's deployment descriptor uses Style 2 XML
     * to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonComboAnnClass_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceSingletonAnnLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceSingletonAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous class level annotation plus Style2 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceSingletonAnnBean.svInheritanceLatch.await(InheritanceSingletonAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceSingletonAnnBean.asyncMeth2ThreadId = " + InheritanceSingletonAnnBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceSingletonAnnBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceSingletonAnnBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonComboAnnMeth_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicSingletonMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicSingletonMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnWithStyle2XML("Rolling Stones");

        // Wait for test_asyncMethAnnWithStyle2XML() to complete
        BasicSingletonMixedBean.svBasicLatch.await(BasicSingletonMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonMixedBean.asyncMethThreadId = " + BasicSingletonMixedBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMethThreadId = " + BasicSingletonMixedBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicSingletonMixedBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method just has a method level Asynchronous
     * annotation, yet other methods of the bean have Style 2 XML defining them
     * to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define other methods, NOT method m(), to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonAnnMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicSingletonMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicSingletonMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnOnly("The Beatles");

        // Wait for test_asyncMethAnnOnly() to complete
        BasicSingletonMixedBean.svBasicLatch.await(BasicSingletonMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonMixedBean.asyncMeth2ThreadId = " + BasicSingletonMixedBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + BasicSingletonMixedBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicSingletonMixedBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has NO method level Asynchronous
     * annotation, yet does have Style 2 XML defining it to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which does NOT have a method
     * level Asynchronous annotation. The bean's deployment descriptor uses
     * Style 2 XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonStyle2XMLMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicSingletonMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicSingletonMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethStyle2XMLOnly("Led Zeppelin");

        // Wait for test_asyncMethStyle2XMLOnly() to complete
        BasicSingletonMixedBean.svBasicLatch.await(BasicSingletonMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonMixedBean.asyncMeth3ThreadId = " + BasicSingletonMixedBean.asyncMeth3ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth3ThreadId = " + BasicSingletonMixedBean.asyncMeth3ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicSingletonMixedBean.asyncMeth3ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method m() is intended to be synchronous
     * (i.e. no async annotations or XML), yet other methods have explicitly
     * been defined to be asynchronous either by annotations or XML.
     *
     * Scenario: Bean class A defines a method m() which has no asynchronous
     * annotations or XML associated to it. However other methods on the bean
     * do have explicit asynchronous annotations or XML.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testSingletonSynchronousMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicSingletonMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicSingletonMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_syncMethod("The Who");

        // Wait for test_syncMethod() to complete
        BasicSingletonMixedBean.svBasicLatch.await(BasicSingletonMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicSingletonMixedBean.syncMethThreadId = " + BasicSingletonMixedBean.syncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be the same since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + BasicSingletonMixedBean.syncMethThreadId
                     + ", currentThreadId = " + currentThreadId, BasicSingletonMixedBean.syncMethThreadId, currentThreadId);
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 1 XML defining all methods to be asynchronous.
     *
     * Scenario: Bean class A defines method m() and annotates it with an
     * Asynchronous annotation. The bean's deployment descriptor uses Style 1
     * XML (i.e. * for method-name) to define all methods of A to be
     * asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessComboAnnMethod_XMLStyle1() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceXMLLocal bean = lookupInheritanceStatelessXMLLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatelessXMLBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous method level annotation plus Style1 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceStatelessXMLBean.svInheritanceLatch.await(InheritanceStatelessXMLBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatelessXMLBean.asyncMethThreadId = " + InheritanceStatelessXMLBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceStatelessXMLBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatelessXMLBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a class level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A has a class level Asynchronous annotation and
     * defines a method m(). The bean's deployment descriptor uses Style 2 XML
     * to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessComboAnnClass_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        InheritanceAnnLocal bean = lookupInheritanceStatelessAnnLocalBean();
        assertNotNull("Bean created successfully", bean);

        // Initialize the latch
        InheritanceStatelessAnnBean.svInheritanceLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_AnnAndXMLMethodAsync("@Asynchronous class level annotation plus Style2 XML");

        // Wait for test_AnnAndXMLMethodAsync() to complete
        InheritanceStatelessAnnBean.svInheritanceLatch.await(InheritanceStatelessAnnBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> InheritanceStatelessAnnBean.asyncMeth2ThreadId = " + InheritanceStatelessAnnBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + InheritanceStatelessAnnBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (InheritanceStatelessAnnBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has both a method level Asynchronous
     * annotation and Style 2 XML defining a method to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessComboAnnMeth_XMLStyle2() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatelessMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatelessMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnWithStyle2XML("Rolling Stones");

        // Wait for test_asyncMethAnnWithStyle2XML() to complete
        BasicStatelessMixedBean.svBasicLatch.await(BasicStatelessMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessMixedBean.asyncMethThreadId = " + BasicStatelessMixedBean.asyncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMethThreadId = " + BasicStatelessMixedBean.asyncMethThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatelessMixedBean.asyncMethThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method just has a method level Asynchronous
     * annotation, yet other methods of the bean have Style 2 XML defining them
     * to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which has a method level
     * Asynchronous annotation. The bean's deployment descriptor uses Style 2
     * XML to define other methods, NOT method m(), to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessAnnMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatelessMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatelessMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethAnnOnly("The Beatles");

        // Wait for test_asyncMethAnnOnly() to complete
        BasicStatelessMixedBean.svBasicLatch.await(BasicStatelessMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessMixedBean.asyncMeth2ThreadId = " + BasicStatelessMixedBean.asyncMeth2ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth2ThreadId = " + BasicStatelessMixedBean.asyncMeth2ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatelessMixedBean.asyncMeth2ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method has NO method level Asynchronous
     * annotation, yet does have Style 2 XML defining it to be asynchronous.
     *
     * Scenario: Bean class A defines a method m() which does NOT have a method
     * level Asynchronous annotation. The bean's deployment descriptor uses
     * Style 2 XML to define method m() to be asynchronous.
     *
     * Method m() should be asynchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessStyle2XMLMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatelessMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatelessMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be asynchronous
        bean.test_asyncMethStyle2XMLOnly("Led Zeppelin");

        // Wait for test_asyncMethStyle2XMLOnly() to complete
        BasicStatelessMixedBean.svBasicLatch.await(BasicStatelessMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessMixedBean.asyncMeth3ThreadId = " + BasicStatelessMixedBean.asyncMeth3ThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be different since the method was asynchronous
        assertTrue("The asynchronous bean method should run on a different thread than the test: asyncMeth3ThreadId = " + BasicStatelessMixedBean.asyncMeth3ThreadId
                   + ", currentThreadId = " + currentThreadId, (BasicStatelessMixedBean.asyncMeth3ThreadId != currentThreadId));
    }

    /**
     * Verify no issues arise when a method m() is intended to be synchronous
     * (i.e. no async annotations or XML), yet other methods have explicitly
     * been defined to be asynchronous either by annotations or XML.
     *
     * Scenario: Bean class A defines a method m() which has no asynchronous
     * annotations or XML associated to it. However other methods on the bean
     * do have explicit asynchronous annotations or XML.
     *
     * Method m() should be synchronous.
     *
     * @throws Exception
     */
    @Test
    public void testStatelessSynchronousMethod() throws Exception {
        long currentThreadId = Thread.currentThread().getId();

        BasicMixedLocal bean = lookupBasicStatelessMixedBean();
        assertNotNull("Bean created successfully", bean);

        // initialize latch
        BasicStatelessMixedBean.svBasicLatch = new CountDownLatch(1);

        // call the method that should be synchronous
        bean.test_syncMethod("The Who");

        // Wait for test_asyncMethStyle2XMLOnly() to complete
        BasicStatelessMixedBean.svBasicLatch.await(BasicStatelessMixedBean.MAX_ASYNC_WAIT, TimeUnit.MILLISECONDS);

        svLogger.info("--> BasicStatelessMixedBean.syncMethThreadId = " + BasicStatelessMixedBean.syncMethThreadId + ", currentThreadId = " + currentThreadId);

        // compare the thread the method was run in and the current thread; they should be the same since the method was synchronous
        assertEquals("The synchronous bean method should run on the same thread as the test: syncMethThreadId = " + BasicStatelessMixedBean.syncMethThreadId
                     + ", currentThreadId = " + currentThreadId, BasicStatelessMixedBean.syncMethThreadId, currentThreadId);
    }

    /**
     * Verify no issues arise when a method is annotated as asynchronous in a
     * module with a 3.0 deployment-descriptor.
     */
    @Test
    public void testStateless30Bean() throws Exception {
        Basic30Bean bean = lookupBasic30BeanFromEJBLocalNamespace();
        assertNull(bean.test().get());
    }
}