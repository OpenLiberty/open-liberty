/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.cdi.interceptors;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.enterprise.inject.Stereotype;
import javax.interceptor.InvocationContext;
import javax.transaction.Transactional;
import javax.transaction.TransactionalException;

import org.osgi.framework.FrameworkUtil;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.tx.jta.embeddable.UserTransactionController;
import com.ibm.wsspi.uow.ExtendedUOWAction;
import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

public abstract class TransactionalInterceptor implements Serializable {

    private static final long serialVersionUID = 485903803670044161L;

    private static final TraceComponent tc = Tr.register(TransactionalInterceptor.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    protected static final String THROW_CHECKED_EXCEPTIONS = "com.ibm.tx.jta.cdi.interceptors.throwCheckedExceptions";

    private static boolean _throwCheckedExceptions = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            return Boolean.getBoolean(THROW_CHECKED_EXCEPTIONS);
        }
    });

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

            if (interceptor == null) {
                CDIService cdiService = priv.getService(FrameworkUtil.getBundle(CDIService.class), CDIService.class);
                if (cdiService != null) {
                    Set<Annotation> bindings = cdiService.getInterceptorBindingsFromInvocationContext(context);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(this, tc, "bindings:", bindings);
                    if (bindings != null)
                        for (Annotation anno : bindings)
                            if (Transactional.class.equals(anno.annotationType())) {
                                interceptor = (Transactional) anno;
                                break;
                            }
                }
            }
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

    @FFDCIgnore(Exception.class)
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
        } catch (Exception e) {
            throw processException(context, e);
        } finally {
            // Reset access to UT to what it was when we started the method.
            tranCont.setEnabled(isUTEnabled);
        }
    }

    @FFDCIgnore(Exception.class)
    protected Object runUnderUOWNoEnablement(int uowType, boolean join, final InvocationContext context, String txLabel) throws Exception {

        // Get hold of the actual annotation so we can pass the lists of exceptions to runUnderUOW
        final Transactional t = getTransactionalAnnotation(context, txLabel);

        final ExtendedUOWAction a = new ExtendedUOWAction() {
            @Override
            public Object run() throws Exception {
                return context.proceed();
            }
        };

        try {
            return getUOWM().runUnderUOW(uowType, join, a, t.rollbackOn(), t.dontRollbackOn());
        } catch (Exception e) {
            throw processException(context, e);
        }
    }

    @Trivial
    private Exception processException(final InvocationContext context, Exception e) {
        if (_throwCheckedExceptions) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "processException: {0} is set.", THROW_CHECKED_EXCEPTIONS);
            return e;
        }

        for (Class<?> declaredException : context.getMethod().getExceptionTypes()) {
            if (declaredException.isAssignableFrom(e.getClass())) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "processException: {0} is assignable from {1}. We can just return it.", declaredException, e.getClass());
                return e;
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "processException: {0} is not assignable from {1}", declaredException, e.getClass());
            }
        }

        // If it's already a RuntimeException, we can throw it
        if (e instanceof RuntimeException) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "processException: {0} is already a RuntimeException. We can just return it.", e.getClass().getName());
            return e;
        }

        // So we need to wrap it in a RuntimeException
        final TransactionalException te = new TransactionalException(e.getMessage(), e);
        if (tc.isDebugEnabled())
            Tr.debug(tc, "processException: wrapping {0} in a TransactionalException", e.getClass().getName());
        return te;
    }
}