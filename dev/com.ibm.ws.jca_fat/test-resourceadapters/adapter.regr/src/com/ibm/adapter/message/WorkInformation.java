/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.adapter.message;

import java.io.Serializable;
import java.security.Principal;

/**
 * This class contains the information that is required to configure the resource adapter to submit JCA 1.6
 * work with a security context. This information needs to be set by the test case before it sends a message
 * and during work submission the resource adapter will use the information stored in this object to setup the
 * Work that is submitted
 */
public class WorkInformation implements Serializable {

    // This is the callerIdentity that is set in the CallerPrincipalCallback
    private String calleridentity;

    private Principal identity;

    // This is the username that is set in the PasswordValidationCallback
    private String username;

    // This is the password that is set in the PasswordValidationCallback
    private String password;

    // These are the password that are set in the GroupPrincipalCallback
    private String[] groups;

    // This parameter indicates whether the Callbacks should be populated with the same execution
    // subject that is passed in by the WorkManager.
    private boolean sameSubject = true;

    // This parameter indicates whether the subject passed in by the resource adapter to the WorkManager is
    // authenticated or not.
    private boolean authenticated = false;

    // This parameter denotes the callbacks that should be passed in to the WorkManager by the resource adapter.
    private String[] callbacks;

    // This parameter denotes the identity that should be passed in the execution subject to the WorkManager.
    private String[] subjectIdentities;

    // This parameter denotes whether we should pass an identity in the subject or not.
    private boolean passIdentityInSubject;

    // Denotes whether the work has a securitycontext or not.
    private boolean hasSecurityContext = true;

    // Denotes whether the work has a transaction context or not.
    private boolean hasTransactionContext = true;

    private boolean implementsWorkContextProvider = true;

    private int workSpecLevel = JCA16;

    private WorkInformation nestedWorkInformation = null;

    private boolean throwsUnexpectedException = false;

    private boolean nullSubject = false;

    public boolean isThrowsUnexpectedException() {
        return throwsUnexpectedException;
    }

    public void setThrowsUnexpectedException(boolean throwsUnexpectedException) {
        this.throwsUnexpectedException = throwsUnexpectedException;
    }

    public String getCalleridentity() {
        return calleridentity;
    }

    public void setCalleridentity(String calleridentity) {
        this.calleridentity = calleridentity;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String[] getGroups() {
        return groups;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public boolean isSameSubject() {
        return sameSubject;
    }

    public void setSameSubject(boolean sameSubject) {
        this.sameSubject = sameSubject;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String[] getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(String[] callbacks) {
        this.callbacks = callbacks;
    }

    public String[] getSubjectIdentities() {
        return subjectIdentities;
    }

    public void setSubjectIdentities(String[] subjectIdentities) {
        this.subjectIdentities = subjectIdentities;
    }

    public boolean isPassIdentityInSubject() {
        return passIdentityInSubject;
    }

    public void setPassIdentityInSubject(boolean passIdentityInSubject) {
        this.passIdentityInSubject = passIdentityInSubject;
    }

    public boolean isHasSecurityContext() {
        return hasSecurityContext;
    }

    public void setHasSecurityContext(boolean hasSecurityContext) {
        this.hasSecurityContext = hasSecurityContext;
    }

    public boolean isHasTransactionContext() {
        return hasTransactionContext;
    }

    public void setHasTransactionContext(boolean hasTransactionContext) {
        this.hasTransactionContext = hasTransactionContext;
    }

    public boolean isImplementsWorkContextProvider() {
        return implementsWorkContextProvider;
    }

    public void setImplementsWorkContextProvider(
                                                 boolean implementsWorkContextProvider) {
        this.implementsWorkContextProvider = implementsWorkContextProvider;
    }

    public WorkInformation getNestedWorkInformation() {
        return nestedWorkInformation;
    }

    public void setNestedWorkInformation(WorkInformation nestedWorkInformation) {
        this.nestedWorkInformation = nestedWorkInformation;
    }

    public int getWorkSpecLevel() {
        return workSpecLevel;
    }

    public void setWorkSpecLevel(int workSpecLevel) {
        this.workSpecLevel = workSpecLevel;
    }

    public Principal getIdentity() {
        return identity;
    }

    public void setIdentity(Principal identity) {
        this.identity = identity;
    }

    public boolean isNullSubject() {
        return nullSubject;
    }

    public void setNullSubject(boolean nullSubject) {
        this.nullSubject = nullSubject;
    }

    public static final int JCA15 = 0;
    public static final int JCA16 = 1;
    public static final String CALLERPRINCIPALCALLBACK = "CALLERPRINCIPALCALLBACK";
    public static final String PASSWORDVALIDATIONCALLBACK = "PASSWORDVALIDATIONCALLBACK";
    public static final String GROUPPRINCIPALCALLBACK = "GROUPPRINCIPALCALLBACK";
}
