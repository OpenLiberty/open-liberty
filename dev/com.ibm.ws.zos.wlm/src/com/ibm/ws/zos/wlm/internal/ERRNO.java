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
 * <errno.h> header file helper class
 */

public enum ERRNO {
    EFAULT(118) {
        @Override
        public String errStr() {
            return "Bad address";
        }
    },
    EINVAL(121) {
        @Override
        public String errStr() {
            return "Invalid argument";
        }
    },
    ENOMEM(132) {
        @Override
        public String errStr() {
            return "Not enough space";
        }
    },
    EPERM(139) {
        @Override
        public String errStr() {
            return "Operation not permitted";
        }
    },
    ESRCH(143) {
        @Override
        public String errStr() {
            return "No Enclave returned";
        }
    },
    EMVSSAFEXTRERR(163) {
        @Override
        public String errStr() {
            return "SAF/RACF extract error";
        }
    },
    EMVSSAF2ERR(164) {
        @Override
        public String errStr() {
            return "SAF/RACF error";
        }
    },
    EMVSWLMERROR(170) {
        @Override
        public String errStr() {
            return "WLM service ended in error";
        }
    };

    private int _errno;

    ERRNO(int errno) {
        this._errno = errno;
    }

    public int errno() {
        return this._errno;
    }

    public abstract String errStr();
}
