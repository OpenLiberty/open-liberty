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
package com.ibm.tx.jta.cdi.interceptors;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import javax.enterprise.inject.Stereotype;
import javax.interceptor.InvocationContext;
import javax.transaction.Transactional;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.tx.jta.embeddable.UserTransactionController;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

public abstract class TransactionalInterceptor implements Serializable {

    private static final long serialVersionUID = 485903803670044161L;

    private static final TraceComponent tc = Tr.register(TransactionalInterceptor.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /*
     * Find the Transactional annotation being processed
     */
    protected Transactional getTransactionalAnnotation(InvocationContext context, String txLabel) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getTransactionalAnnotation", new Object[] { context, this });

        // Check for method annotation
        Transactional interceptor = findTransactionalInterceptor(context.getMethod());

        if (interceptor == null) {
            // Check for class annotation
            // Getting the class of the target only gives us a WELD proxy that won't have the annotations
            // if they're not defined as @Inherited, so we need to go a level higher in the class hierarchy.
            interceptor = findTransactionalInterceptor(context.getTarget().getClass().getSuperclass());
        }

        if (interceptor == null) {
            // We can't find the annotation we're processing so bail out
            throw new Exception("Can't find @Transactional annotation in AroundInvoke for TxType." + txLabel);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getTransactionalAnnotation", interceptor);
        return interceptor;
    }

    private Transactional findTransactionalInterceptor(AnnotatedElement element) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "findTransactionalInterceptor", new Object[] { element, this });

        Transactional t = element.getAnnotation(Transactional.class);

        if (t == null) {
            // Look for a stereotype
            t = findInterceptorFromStereotype(element.getAnnotations());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "findTransactionalInterceptor", t);
        return t;
    }

    // Look through a list of annotations, and see if one or more of them is annotated with
    // @Stereotype. If so, check for whether that stereotype contains @Transactional.
    // Recurses appropriately through the tree of stereotypes. Returns the first
    // Transactional interceptor it finds, as multiple interceptors of the same type
    // are disallowed.

    // NB: we don't currently check for cycles in stereotypes (which could cause
    // infinite recursion). At time of writing WELD causes a stack overflow if you
    // do this, and presumably any fix for that will involve disallowing it, given
    // that it doesn't make sense to do. As a result have chosen not to worry about
    // it here.
    private Transactional findInterceptorFromStereotype(Annotation[] anns) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "findInterceptorFromStereotype", new Object[] { anns, this });

        Transactional ret = null;
        for (Annotation ann : anns) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Examining annotation: " + ann.toString());

            Class<? extends Annotation> annType = ann.annotationType();

            if (annType.getAnnotation(Stereotype.class) != null) {
                // we've found a stereotype. Check if it has an interceptor,
                // and if not recurse!
                ret = findTransactionalInterceptor(annType);
                if (ret != null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "findInterceptorFromStereotype", ret);

                    return ret;
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "findInterceptorFromStereotype", null);
        return null;
    }

    protected UOWManager getUOWM() {
        return UOWManagerFactory.getUOWManager();
    }

    protected Object runUnderUOWManagingEnablement(int uowType, boolean join, final InvocationContext context, String txLabel) throws Exception {

        // Get hold of the actual annotation so we can pass the lists of exceptions to runUnderUOW
        final Transactional t = getTransactionalAnnotation(context, txLabel);

        final UserTransactionController tranCont = ((UserTransactionController) getUOWM());

        final boolean isUTEnabled = tranCont.isEnabled();

        final ExtendedUOWAction a = new ExtendedUOWAction() {
            @Override
            public Object run() throws Exception {
                //disable access to UserTransaction while we're running the user code
                tranCont.setEnabled(false);
                try {
                    return context.proceed();
                } finally {
                    // reenable access to the UT after we're done. Allows WAS UOW code to
                    // access UT if it needs to.
                    tranCont.setEnabled(true);
                }
            }
        };

        try {
            // allow access to UT while we're within App Server code.  Allows WAS UOW code to use
            // UT methods if it needs to.
            tranCont.setEnabled(true);

            return getUOWM().runUnderUOW(uowType, join, a, t.rollbackOn(), t.dontRollbackOn());
        } finally {
            // Reset access to UT to what it was when we started the method.
            tranCont.setEnabled(isUTEnabled);
        }
    }

    protected Object runUnderUOWNoEnablement(int uowType, boolean join, final InvocationContext context, String txLabel) throws Exception {

        // Get hold of the actual annotation so we can pass the lists of exceptions to runUnderUOW
        final Transactional t = getTransactionalAnnotation(context, txLabel);

        final ExtendedUOWAction a = new ExtendedUOWAction() {
            @Override
            public Object run() throws Exception {
                return context.proceed();
            }
        };

        return getUOWM().runUnderUOW(uowType, join, a, t.rollbackOn(), t.dontRollbackOn());

    }
}