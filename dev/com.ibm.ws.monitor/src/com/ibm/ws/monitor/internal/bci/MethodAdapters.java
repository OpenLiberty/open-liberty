/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.bci;

import java.util.Set;

import com.ibm.ws.monitor.internal.ProbeListener;

public enum MethodAdapters {

    ENTRY_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeAtEntryMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    RETURN_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeAtReturnMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    EXCEPTION_EXIT_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeAtExceptionExitMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    CATCH_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeAtCatchMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    THROW_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeAtThrowMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    METHOD_CALL_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeMethodCallMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    FIELD_GET_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeFieldGetMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    },
    FIELD_SET_PROBE {
        ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
            return new ProbeFieldSetMethodAdapter(probeMethodAdapter, methodInfo, interested);
        }
    };

    abstract ProbeMethodAdapter create(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested);

    boolean isRequiredForListener(ProbeListener listener) {
        return true;
    };

}