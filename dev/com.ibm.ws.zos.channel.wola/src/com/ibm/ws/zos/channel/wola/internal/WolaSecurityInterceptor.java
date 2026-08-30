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
package com.ibm.ws.zos.channel.wola.internal;

import java.util.Hashtable;

import javax.security.auth.Subject;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Sets up the J2EE Subject for an inbound WOLA request. The WOLA request
 * message contains an mvsUserId field, which represents the client identity.
 * This class creates a J2EE Subject and an asserted SAFCredential for the
 * mvsUserId and sets the Subject on the thread.
 *
 * Note: it's OK that we're blithely asserting the mvsUserId in this case
 * because we trust where it came from. See com.ibm.zos.native/server_wola_message.mc
 * for more details.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class WolaSecurityInterceptor implements WolaRequestInterceptor {

    protected static final String SafAuditString = "Inbound WOLA request asserted ID";

    /**
     * To determine whether SAF UserRegistry is configured.
     */
    @Reference(target = "(com.ibm.ws.security.registry.type=SAF)")
    UserRegistry safUserRegistry;

    /**
     * For authenticating the user (presumably with SAF registry, but could be other).
     */
    @Reference
    AuthenticationService authenticationService;

    /**
     * For pushing/popping the RunAs Subject on the thread.
     */
    private final SubjectManager subjectManager = new SubjectManager();

    /**
     * IF the WolaMessage contains an MvsUserId, then set it as the J2EE subject
     * for the request.
     *
     * This requires creating an asserted SAF credential for the user, creating a
     * Subject for the credential, and setting the Subject as the J2EE subject
     * (via SubjectManager).
     * T
     *
     * @param wolaMessage - the wola message for the current request
     */
    @Override
    public Object preInvoke(WolaMessage wolaMessage) {

        String mvsUserId = wolaMessage.getMvsUserId();

        if (!isEmpty(mvsUserId)) {
            return setRunAsSubject(createSubject(mvsUserId));
        }

        return null;
    }

    /**
     * @return true if the given String is null or "".
     */
    protected boolean isEmpty(String s) {
        return (s == null || s.length() == 0);
    }

    /**
     * Create a partial subject which will be used for a hashtable login.
     */
    protected Subject createUserIdHashtableSubject(String mvsUserId, UserRegistry safRegistry) {
        Subject newSubject = new Subject();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        if (authenticationService == null || !authenticationService.isAllowHashTableLoginWithIdOnly())
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, mvsUserId);
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, safRegistry.getRealm());
        newSubject.getPublicCredentials().add(hashtable);
        return newSubject;
    }

    /**
     * Fluff up a Subject for the given mvsUserId. The subject will contain a
     * "lazy" asserted SAFCredential.
     *
     * Note: we're OK with creating the asserted SAFCredential because we trust
     * where the mvsUserId came from. See com.ibm.zos.native/server_wola_message.mc
     * for more details.
     *
     * @return A subject for the given mvsUserId that can be used to authorize the user
     *         against SAF; e.g. for J2EE roles (EJBROLE profiles).
     */
    private Subject createSubject(String mvsUserId) {
        Subject sub = createUserIdHashtableSubject(mvsUserId, safUserRegistry);
        Subject authenticatedSubject = null;

        try {
            authenticatedSubject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_DEFAULT, sub);
        } catch (AuthenticationException ae) {
            throw new RuntimeException("Could not create Subject for WOLA request", ae);
        }

        return authenticatedSubject;
    }

    /**
     * Place the given Subject on the thread as both Caller/Invocation
     * and return the previous Subjects on the thread inside a PreInvokeToken.
     *
     * @return A PreInvokeToken containing the Caller/Invocation Subjects previously
     *         on the thread.
     */
    protected PreInvokeToken setRunAsSubject(Subject subject) {
        if (subject == null) {
            return null;
        }

        return new PreInvokeToken(subjectManager.replaceCallerSubject(subject), subjectManager.replaceInvocationSubject(subject));
    }

    /**
     *
     */
    @Override
    public void postInvoke(Object preInvokeToken, Exception responseException) {
        if (preInvokeToken != null) {
            PreInvokeToken p = (PreInvokeToken) preInvokeToken;

            subjectManager.setCallerSubject(p.prevCallerSubject);
            subjectManager.setInvocationSubject(p.prevInvocationSubject);
        }
    }

    /**
     * This token is returned from preInvoke and passed back on postInvoke.
     * It contains the Subjects that were previously on the thread before we
     * asserted the WolaMessage's mvsUserId.
     *
     * (Technically there should never be a Subject already on the thread,
     * since we're on the inbound WOLA path, but who am I to assume?)
     */
    protected static class PreInvokeToken {
        Subject prevCallerSubject;
        Subject prevInvocationSubject;

        public PreInvokeToken(Subject prevCallerSubject, Subject prevInvocationSubject) {
            this.prevCallerSubject = prevCallerSubject;
            this.prevInvocationSubject = prevInvocationSubject;
        }

    }

}
