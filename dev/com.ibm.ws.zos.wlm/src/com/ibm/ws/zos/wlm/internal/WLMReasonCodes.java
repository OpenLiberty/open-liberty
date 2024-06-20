/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

/**
 * WLM Native Services REASON CODE helper class
 */

public enum WLMReasonCodes {
    IWMRSNCODENOTENCLAVE(0x0000041C) {
        @Override
        public String descriptionStr() {
            return "Not currently joined";
        }
    },
    IWMRSNCODEBEGINENVOUTSTANDING(0x00000850) {
        @Override
        public String descriptionStr() {
            return "Caller is already operating under an outstanding Begin environment which has implicitly joined an enclave";
        }
    },
    IWMRSNCODEALREADYINENCLAVE(0x00000857) {
        @Override
        public String descriptionStr() {
            return "Current dispatchable workunit is already in an enclave";
        }
    };

    private int _rsnCode;

    WLMReasonCodes(int rsnCode) {
        this._rsnCode = rsnCode;
    }

    public int rsnCode() {
        return this._rsnCode;
    }

    public abstract String descriptionStr();
}
