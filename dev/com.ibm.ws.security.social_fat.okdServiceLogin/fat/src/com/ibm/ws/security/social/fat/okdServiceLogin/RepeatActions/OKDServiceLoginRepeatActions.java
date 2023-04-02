package com.ibm.ws.security.social.fat.okdServiceLogin.RepeatActions;
/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
public class OKDServiceLoginRepeatActions {
    protected static final String STUB = "Stub";
    protected static final String OPENSHIFT = "OpenShift";

    public static OKDServiceLogin usingStub() {

        return new OKDServiceLogin(STUB);
    }

    public static OKDServiceLogin minimumConfig_usingStub() {

        return new OKDServiceLogin(STUB, "minimumConfig_" + STUB);
    }

    public static OKDServiceLogin basicTests_usingStub() {

        return new OKDServiceLogin(STUB, "fullConfig_" + STUB);
    }

    public static OKDServiceLogin usingOpenShift() {

        return new OKDServiceLogin(OPENSHIFT);
    }

    public static OKDServiceLogin minimumConfig_usingOpenShift() {

        return new OKDServiceLogin(OPENSHIFT, "minimumConfig_" + OPENSHIFT);
    }

    public static OKDServiceLogin basicTests_usingOpenShift() {

        return new OKDServiceLogin(OPENSHIFT, "fullConfig_" + OPENSHIFT);
    }
}
