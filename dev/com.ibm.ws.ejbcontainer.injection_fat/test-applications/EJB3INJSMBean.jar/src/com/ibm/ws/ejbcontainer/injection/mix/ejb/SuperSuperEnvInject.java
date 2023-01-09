/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.annotation.Resource;

public class SuperSuperEnvInject extends SuperOfSuperSuperEnvInject {
    // used to test field injection when name is not defaulted
    @Resource(name = "superProtectedDouble")
    protected Double superProtectedDouble = new Double(00.00);

    @Resource(name = "superPrivateNumber")
    private int superPrivateNumber = 0;

    @Resource(name = "superPublicChar", description = "This is the superclasses public character.")
    public char superPublicChar = 'A';

    // TODO: what does this one do?
    @SuppressWarnings("hiding")
    protected int myNumber = 90210;

    // (isMyInjectionDefaulted is Used to test injection when the name is defaulted
    @Resource
    private boolean isMyInjectionDefaulted = false;

    @Resource
    protected String isMyInjectionDefaulted2 = "No";

    @Resource
    public Character isMyInjectionDefaulted3 = 'n';

    protected int isMyInjectionDefaulted4 = 0;
    private float isMyInjectionDefaulted5 = 1.11F;
    public short isMyInjectionDefaulted6 = 1;

    // Test inherited method injection of defaulted names
    @Resource
    protected void setIsMyInjectionDefaulted4(int isMyInjectionDefaulted4) {
        this.isMyInjectionDefaulted4 = isMyInjectionDefaulted4;
    }

    @SuppressWarnings("unused")
    @Resource
    private void setIsMyInjectionDefaulted5(float isMyInjectionDefaulted5) {
        this.isMyInjectionDefaulted5 = isMyInjectionDefaulted5;
    }

    @Resource
    public void setIsMyInjectionDefaulted6(short isMyInjectionDefaulted6) {
        this.isMyInjectionDefaulted6 = isMyInjectionDefaulted6;
    }

    protected String willIBeInjected = "No I won't";
    private boolean willIBeInjected2 = false;
    public Long willIBeInjected3 = new Long(0000);

    // willIBeInjected is used to test inherited method injection when the name is not defaulted
    @Resource(name = "willIBeInjected")
    protected void setWillIBeInjected(String willIBeInjected) {
        this.willIBeInjected = willIBeInjected;
    }

    @SuppressWarnings("unused")
    @Resource(name = "willIBeInjected2")
    private void setWillIBeInjected2(boolean willIBeInjected2) {
        this.willIBeInjected2 = willIBeInjected2;
    }

    @Resource(name = "willIBeInjected3")
    public void setWillIBeInjected3(Long willIBeInjected3) {
        this.willIBeInjected3 = willIBeInjected3;
    }

    protected String iamOverridden = "No I won't";
    public Long iamOverridden2 = new Long(0000);
    protected short iamOverridden3 = 1;

    // iamOverriden is used to test overridden method injection when the name is not defaulted
    @Resource
    protected void setIamOverriden(String iamOverridden) {
        this.iamOverridden = iamOverridden;
    }

    @Resource
    public void setIamOverriden2(Long iamOverridden2) {
        this.iamOverridden2 = iamOverridden2;
    }

    @Resource
    protected void setIamOverriden3(short iamOverridden3) {
        this.iamOverridden3 = iamOverridden3;
    }

    public String getWillIBeInjected() {
        return willIBeInjected;
    }

    public boolean isWillIBeInjected2() {
        return willIBeInjected2;
    }

    public Long getWillIBeInjected3() {
        return willIBeInjected3;
    }

    public Double getSuperProtectedDouble() {
        return superProtectedDouble;
    }

    protected int getSuperPrivateNumber() {
        return superPrivateNumber;
    }

    public char getSuperPublicChar() {
        return superPublicChar;
    }

    protected void setMyNumber(int myNumber) {
        this.myNumber = myNumber;
    }

    public int getMyNumber() {
        return myNumber;
    }

    public boolean getIsMyInjectionDefaulted() {
        return this.isMyInjectionDefaulted;
    }

    public float getIsMyInjectionDefaulted5() {
        return this.isMyInjectionDefaulted5;
    }
}
