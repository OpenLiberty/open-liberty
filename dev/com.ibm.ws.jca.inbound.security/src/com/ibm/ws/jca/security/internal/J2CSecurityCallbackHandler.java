/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jca.security.internal;

import java.io.IOException;
import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * This class implements the {@link javax.security.auth.callback.CallbackHandler} interface and provides
 * support for handling the following types of callbacks
 * 1) CallerPrincipalCallback
 * 2) GroupPrincipalCallback
 * 3) PasswordPasswordValidationCallback
 * The other callbacks mentioned in the JCA 1.6 spec (Section 16.4.1) are supported as no-ops and
 * anything other than that will result in an UnsupportedCallbackException. The no-ops supported are
 * 1) CertStoreCallback
 * 2) TrustStoreCallback
 * 3) PrivateKeyCallback
 * 4) SecretKeyCallback
 * When the application server calls the setupSecurityContext method of the resource adapter's implementation
 * of {@link javax.resource.spi.work.SecurityContext}, it passes an instance of the J2CSecurityCallbackHandler
 * to the resource adapter as a parameter. The resource adapter then proceeds to call handle with an array
 * of Callback objects.
 * 
 * @author jroast
 * 
 */
public class J2CSecurityCallbackHandler implements CallbackHandler {

    final static TraceComponent tc = Tr.register(J2CSecurityCallbackHandler.class, "WAS.j2c.security", "com.ibm.ws.jca.security.resources.J2CAMessages");

    private Invocation[] _invocations;
    private Subject _executionSubject;
    private Hashtable<String, Object> _addedCred;
    private String _realmName;
    private String _unauthenticated;

    private J2CSecurityCallbackHandler() {}

    public J2CSecurityCallbackHandler(Subject executionSubject, String realm, String unauthenticated) {
        _executionSubject = executionSubject;
        _addedCred = new Hashtable<String, Object>();
        _invocations = new Invocation[] { null, null, null };
        _realmName = realm;
        _unauthenticated = unauthenticated;
    }

    /**
     * This method is invoked by the resource adapter after passing the list
     * of callbacks that it needs the application server to handle. The behaviour
     * of the handler for each of the callbacks is given below
     * 
     * 1) CallerPrincipalCallback: The handler gets the caller principal that
     * this callback returns and sets it on the subject that is provided by
     * the same callback after converting it into a principal object internal
     * to WebSphere application server. In the case of the resource adapter also
     * using the application server's security domain the principals returned from the
     * CallerPrincipalCallback can be used as is without any translation. In case the
     * security policy domain of the resource adapter is different from that of the
     * application server the Identity that is set in the given Subject should be the
     * corresponding mapped identity in the Application Server's security domain.
     * Note that the subject mentioned above will finally be used to set the caller
     * context for the inbound Work. Thus this subject must be the same subject that
     * the application server passes as the second parameter in the setupSecurityContext
     * method of the {@link javax.resource.spi.work.SecurityContext} class. If the callback
     * returns a null Principal on getCallerPrincipal, then the handler will set WebSphere
     * Application Server's representation of the UNAUTHENTICATED principal on the given
     * Subject.
     * 2) GroupPrincipalCallback: A resource adapter might use the GroupPrincipalCallback to establish the
     * container�s representation of the corresponding group principals within the
     * Subject. When a null value is passed to the groups argument, the handler will
     * establish the container�s representation of no group principals within the Subject.
     * Otherwise, the handler�s processing of this callback is additive, yielding the
     * union (without duplicates) of the principals existing within the Subject, and
     * those created with the names occurring within the argument array. The
     * CallbackHandler will define the type of the created principals.
     * 3) PasswordValidationCallback: A resource adapter might use the PasswordValidationCallback to employ
     * the password validation facilities of its containing runtime. Valid only if both RA and the
     * Application Server are in the same security domain.
     * 
     * @param callbacks The array of callbacks that this handler must handle
     * @return void
     * @throws IOException if name is not a valid property name
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException,
                    UnsupportedCallbackException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "handle");
        }
        if (callbacks == null || callbacks.length == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "handle", "No Callbacks received, do nothing.");
            }
            return;
        }
        arrangeCallbacks(callbacks); // 675924
        try {

            for (Callback callback : callbacks) {
                if (callback instanceof CallerPrincipalCallback) {
                    J2CSecurityHelper.handleCallerPrincipalCallback((CallerPrincipalCallback) callback, _executionSubject, _addedCred, _realmName, _unauthenticated, _invocations);
                    // TODO Mapping to principals in the same domain in case the inflown
                    // principal is not in the same security domain as the application server.
                } else if (callback instanceof GroupPrincipalCallback) {
                    // Names of group principals to be added to the subject
                    J2CSecurityHelper.handleGroupPrincipalCallback((GroupPrincipalCallback) callback, _executionSubject, _addedCred, _realmName, _invocations);
                } else if (callback instanceof PasswordValidationCallback) {
                    J2CSecurityHelper.handlePasswordValidationCallback((PasswordValidationCallback) callback, _executionSubject, _addedCred, _realmName, _invocations);
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
            J2CSecurityHelper.addSubjectCustomData(_executionSubject, _addedCred);

        } catch (Exception ex) {
            Tr.error(tc, "ERROR_HANDLING_CALLBACK_J2CA0672", new Object[] { ex.getClass().getName(), ex.getMessage() });
            FFDCFilter.processException(ex, getClass().getName() + ".handle", "153");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The exception is " + ex);
                //ex.printStackTrace(System.out);
            }
            if (ex instanceof IOException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "handle");
                }
                throw (IOException) ex;
            } else if (ex instanceof UnsupportedCallbackException) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "handle");
                }
                throw (UnsupportedCallbackException) ex;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "handle");
                }
                throw new IOException(ex);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "handle");
        }
    }

    public Invocation[] getInvocations() {
        Invocation[] invocations = new Invocation[] { null, null, null };
        for (int i = 0; i < _invocations.length; ++i)
            invocations[i] = _invocations[i];
        return invocations;
    }

    protected String getCacheKey() {
        return (String) _addedCred.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
    }

    // Begin 675924
    /**
     * This method is called to ensure that the first callback is always a CallerPrincipalCallback
     * irrespective of the order in which the callbacks are passed in by the resource adapter.
     * If we need to return false to the PasswordValidationCallback when the name passed in by the
     * CallerPrincipalCallback is different from the one set in the PasswordValidationCallback, the
     * PasswordValidationCallback should always be processed after the CallerPrincipalCallback.
     * 
     * @param callbacks
     */
    private void arrangeCallbacks(Callback[] callbacks) {
        if (callbacks[0] instanceof CallerPrincipalCallback)
            return;

        int length = callbacks.length;
        for (int i = 0; i < length; i++) {
            if (callbacks[i] instanceof CallerPrincipalCallback) {
                Callback callback = callbacks[0];
                callbacks[0] = callbacks[i];
                callbacks[i] = callback;
                break;
            }
        }
    }
    // End 675924
}

enum Invocation {
    CALLERPRINCIPALCALLBACK,
    GROUPPRINCIPALCALLBACK,
    PASSWORDVALIDATIONCALLBACK
}
