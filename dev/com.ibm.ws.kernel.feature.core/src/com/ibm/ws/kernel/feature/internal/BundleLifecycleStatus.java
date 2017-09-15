/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Container for the results of starting a set of bundles:
 * A) A list of successfully started Bundles, and
 * B) A map of String-Exception pairs for bundles that caused exceptions
 * when being started.
 * 
 * <p>
 * It is up to the caller to use this information to provide appropriate diagnostics.
 */
public class BundleLifecycleStatus {
    private Map<Bundle, Throwable> startExceptions = null;

    protected volatile boolean contextIsValid = true;

    @Trivial
    public boolean startExceptions() {
        return startExceptions != null;
    }

    public Map<Bundle, Throwable> getStartExceptions() {
        return startExceptions;
    }

    @Trivial
    public boolean contextIsValid() {
        return contextIsValid;
    }

    public void markContextInvalid() {
        contextIsValid = false;
    }

    public void addStartException(Bundle bundle, Throwable e) {
        if (startExceptions == null)
            startExceptions = new HashMap<Bundle, Throwable>();

        startExceptions.put(bundle, e);
    }

    @Trivial
    public String traceStartExceptions() {
        if (startExceptions == null)
            return null;

        StringBuffer strbuf = new StringBuffer();

        for (Bundle key : startExceptions.keySet()) {
            strbuf.append("(");
            strbuf.append("bundle=").append(key.getSymbolicName());
            strbuf.append(", ");
            strbuf.append("ex=").append(startExceptions.get(key));
            strbuf.append("); ");
        }

        return strbuf.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[validContext=" + contextIsValid
               + ",exceptions=" + startExceptions
               + "]";
    }
}