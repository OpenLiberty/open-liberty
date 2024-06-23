/*******************************************************************************
 * Copyright (c) 2020,2024 IBM Corporation and others.
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

package com.ibm.adapter.work;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.work.WorkContext;
import javax.resource.spi.work.WorkContextProvider;
import javax.security.auth.Subject;

import com.ibm.adapter.endpoint.MessageEndpointTestResultsImpl;
import com.ibm.adapter.endpoint.MessageEndpointWrapper;
import com.ibm.adapter.message.FVTMessage;
import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.csi.MessageEndpointTestResults;

/**
 * This is an implementation of a JCA 1.6 work that implements the
 * WorkContextProvider. It contains a list of work contexts
 */
public class FVTComplexJCA16WorkImpl extends FVTComplexWorkImpl implements WorkContextProvider {

    private static final TraceComponent tc = Tr.register(FVTComplexJCA16WorkImpl.class);
    private static final long serialVersionUID = -499677469005204317L;

    private List<WorkContext> contexts = null;

    public FVTComplexJCA16WorkImpl(String workName, FVTMessage message,
                                   FVTWorkDispatcher workDispatcher, List<WorkContext> workCtxs) {
        super(workName, message, workDispatcher);
        if (workCtxs == null) {
            contexts = new ArrayList<WorkContext>();
        } else {
            contexts = workCtxs;
        }
    }

    @Override
    public List<WorkContext> getWorkContexts() {
        return contexts;
    }

    /**
     * This method will call the messageCall method of its parent and after that
     * set the caller subject in the testResult object
     *
     * @param message
     *                    the string which contains the message call information
     */
    @Override
    protected void messageCall(String message) {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "messageCall", new Object[] { this, message });

        String str[] = parseMessage(message);
        String endpointName = str[0];
        String msg = str[1];
        String instanceId = str[2];

        String key = endpointName + instanceId;
        super.messageCall(message);
        MessageEndpointWrapper endpoint = getEndpointWrapper(key);
        MessageEndpointTestResults testResult = endpoint.getTestResult();
        AccessControlContext acc = AccessController.getContext();
        ((MessageEndpointTestResultsImpl) testResult).setCallerSubject(getCurrentSubject());
        if (tc.isEntryEnabled())
            Tr.exit(tc, "MessageCall");
    }

    /**
     * In Java 23, the Subject.getSubject() method always throws an UnsupportedOperationException
     * So for versions before Java 23, we can still use Subject.getSubject(), but for Java 23 and beyond,
     * we need to switch to using Subject.current(), which was introduced in Java 18.
     *
     * So for Java 22 and earlier, this returns Subject.getSubject()
     * For Java 23 and later, this method returns Subject.current()
     *
     * @return
     */
    private Subject getCurrentSubject() {
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D"); // split on non-digits

        // Pre-JDK 9 the java.version is 1.MAJOR.MINOR
        // Post-JDK 9 the java.version is MAJOR.MINOR
        int i = Integer.parseInt(versionElements[0]) == 1 ? 1 : 0;
        final int MAJOR = Integer.parseInt(versionElements[i++]);

        if (MAJOR <= 22) {
            // return Subject.getSubject()
            return Subject.getSubject(AccessController.getContext());
        } else {
            // return Subject.current()
            try {
                final MethodType subjectMethodType = MethodType.methodType(Subject.class);
                MethodHandle getCurrentMethodHandle = MethodHandles.lookup().findStatic(Subject.class, "current", subjectMethodType);
                return (Subject) getCurrentMethodHandle.invoke();
            } catch (Throwable e) {
                return null;
            }
        }
    }
}
