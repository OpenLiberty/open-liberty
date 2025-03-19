/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.proxy;

import java.security.CodeSource;
import java.security.Permission;
import java.security.Policy;
import java.security.Principal;
import java.security.ProtectionDomain;

import javax.security.auth.Subject;

import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;

public class JavaSePolicyProxyImpl implements PolicyProxy {

    private static ProtectionDomain nullPd = new ProtectionDomain(new CodeSource(null, (java.security.cert.Certificate[]) null), null, null, null);
    private static CodeSource nullCs = new CodeSource(null, (java.security.cert.Certificate[]) null);
    private static final boolean isJava24OrLater = JavaInfo.majorVersion() >= 24;

    private final Policy policy;

    // Used by test
    public JavaSePolicyProxyImpl() {
        this(Policy.getPolicy());
    }

    JavaSePolicyProxyImpl(Policy p) {
        policy = p;
    }

    @Override
    public void refresh() {
        policy.refresh();
    }

    @Override
    public void setPolicy() {
        if (!isJava24OrLater) {
            Policy.setPolicy(policy);
        }
    }

    @Override
    public boolean implies(String contextId, Subject subject, Permission permission) {
        ProtectionDomain pd = null;
        if (subject != null && subject.getPrincipals().size() > 0) {
            Principal[] principalArray = subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]);
            pd = new ProtectionDomain(nullCs, null, null, principalArray);
        } else {
            pd = nullPd;
        }
        return (isJava24OrLater ? policy : Policy.getPolicy()).implies(pd, permission);
    }
}
