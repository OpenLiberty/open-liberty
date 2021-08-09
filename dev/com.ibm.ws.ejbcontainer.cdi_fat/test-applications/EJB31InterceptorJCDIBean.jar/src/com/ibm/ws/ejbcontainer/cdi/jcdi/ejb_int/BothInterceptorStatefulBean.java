/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.interceptor.Interceptors;

/**
 * Stateful bean that has an EJB interceptor, and a CDI interceptor.
 **/
@Stateful(name = "BothInterceptorStateful")
@Local(InterceptorStatefulLocal.class)
@Interceptors(EJBInterceptor.class)
@CDIInterceptorBinding
@SFCDIInterceptorBinding
public class BothInterceptorStatefulBean implements InterceptorAccess {
    private static final String CLASS_NAME = BothInterceptorStatefulBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final List<String> svPreDestroyStack = new ArrayList<String>();

    @Resource(name = "EJBName")
    private String ivEJBName = "BothInterceptorStateful";

    @Resource(name = "EJBContext")
    protected SessionContext ivContext;

    private final List<String> ivPostConstructStack = new ArrayList<String>();
    private final List<String> ivPostActivateStack = new ArrayList<String>();
    private final List<String> ivPrePassivateStack = new ArrayList<String>();

    @PostConstruct
    void initialize() {
        svLogger.info("  " + ivEJBName + ".initialize");
        ivPostConstructStack.add(ivEJBName + ".initialize");
    }

    @PreDestroy
    void destroy() {
        svLogger.info("  " + ivEJBName + ".destroy");
        svPreDestroyStack.add(ivEJBName + ".destroy");
    }

    @PostActivate
    void activate() {
        svLogger.info("  " + ivEJBName + ".activate");
        ivPostActivateStack.add(ivEJBName + ".activate");
    }

    @PrePassivate
    void passivate() {
        svLogger.info("  " + ivEJBName + ".passivate");
        ivPrePassivateStack.add(ivEJBName + ".passivate");
    }

    /**
     * Returns the current PostConstruct call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPostConstructStack() {
        return ivPostConstructStack;
    }

    /**
     * Returns the current PreDestroy call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPreDestroyStack() {
        return svPreDestroyStack;
    }

    /**
     * Returns the current PostActivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPostActivateStack() {
        return ivPostActivateStack;
    }

    /**
     * Returns the current PrePassivate call stack, so an interceptor may
     * add itself to the stack.
     **/
    @Override
    public List<String> getPrePassivateStack() {
        return ivPrePassivateStack;
    }

    /**
     * Verifies that the interceptors were properly called per the
     * configuration of the bean.
     *
     * @param callStack list to be updated by interceptors
     **/
    public void verifyInterceptorCalls(List<String> callStack) {
        svLogger.info("> " + ivEJBName + ".verifyInterceptorCalls()");

        assertEquals("Unexpected PostConstruct interceptor calls : " + ivPostConstructStack,
                     3, ivPostConstructStack.size());

        String stackEntry = ivPostConstructStack.get(0);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     "EJBInterceptor.postConstruct", stackEntry);

        stackEntry = ivPostConstructStack.get(1);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     "CDIInterceptor.postConstruct", stackEntry);

        stackEntry = ivPostConstructStack.get(2);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     ivEJBName + ".initialize", stackEntry);

        assertEquals("Unexpected AroundInvoke interceptor calls : " + callStack,
                     3, callStack.size());

        stackEntry = callStack.get(0);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "EJBInterceptor.aroundInvoke", stackEntry);

        stackEntry = callStack.get(1);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "CDIInterceptor.aroundInvoke", stackEntry);

        stackEntry = callStack.get(2);
        assertEquals("Unexpected AroundInvoke interceptor calls : " + stackEntry,
                     "SFCDIInterceptor.aroundInvoke", stackEntry);

        assertEquals("Unexpected PreDestroy interceptor calls : " + svPreDestroyStack,
                     2, svPreDestroyStack.size());

        stackEntry = svPreDestroyStack.get(0);
        assertEquals("Unexpected PreDestroy interceptor call : " + stackEntry,
                     "SFCDIInterceptor.preDestroy", stackEntry);

        stackEntry = svPreDestroyStack.get(1);
        assertEquals("Unexpected PreDestroy interceptor call : " + stackEntry,
                     ivEJBName + ".destroy", stackEntry);

//    assertEquals( "Unexpected PrePassivate interceptor calls : " + ivPrePassivateStack,
//                  2, ivPrePassivateStack.size() );

//    stackEntry = ivPrePassivateStack.get( 0 );
//    assertEquals( "Unexpected PrePassivate interceptor call : " + stackEntry,
//                  "SFCDIInterceptor.prePassivate", stackEntry );

//    stackEntry = ivPrePassivateStack.get( 1 );
//    assertEquals( "Unexpected PrePassivate interceptor call : " + stackEntry,
//                  ivEJBName + ".passivate", stackEntry );

//    assertEquals( "Unexpected PostActivate interceptor calls : " + ivPostActivateStack,
//                  2, ivPostActivateStack.size() );

//    stackEntry = ivPostActivateStack.get( 0 );
//    assertEquals( "Unexpected PostActivate interceptor call : " + stackEntry,
//                  "SFCDIInterceptor.postActivate", stackEntry );

//    stackEntry = ivPostActivateStack.get( 1 );
//    assertEquals( "Unexpected PostActivate interceptor call : " + stackEntry,
//                  ivEJBName + ".activate", stackEntry );

        svLogger.info("< " + ivEJBName + ".verifyInterceptorCalls()");
    }

    /**
     * Removes the Stateful bean instance to test PreDestroy
     */
    @Remove
    public void remove(List<String> callStack) {
        svLogger.info("  " + ivEJBName + ".remove");
        svPreDestroyStack.clear();
    }

    /**
     * Method that forces passivation
     */
    public void forcePassivateAndActivate(List<String> callStack) {
        svLogger.info("  " + ivEJBName + ".forcePassivateAndActivate");
        // Nothing needed here... default tran behavior will do it.
    }

}
