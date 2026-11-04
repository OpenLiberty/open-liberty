/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm.internal;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;

/**
 * A ClassificationData object represents an individual rule for classifying incoming
 * http requests. It serves to hold the particular rule mappings and provide
 * functions to evaluate when incoming requests match this rule.
 */
public class ClassificationData {
    private final String host;
    private final String resource;
    private final String tclass;
    /**
     * Converted Transaction Class for native (EBCDIC and Blank Padded).
     */
    private final byte[] tclassNative;

    private final HashSet<String> method;
    private final HashSet<Integer> port;

    public ClassificationData(String newTransactionClass, String newHost, HashSet<Integer> newPort, String newResource, HashSet<String> newMethod) {
        // Pull in variables
        tclass = newTransactionClass;
        host = newHost;
        port = newPort;
        resource = newResource;
        method = newMethod;

        // Blank-pad TransactionClass and convert to EBCDIC for Native use.
        if (!tclass.equals("")) {
            String temp = tclass + "        ".substring(0, WLMNativeServices.WLM_MAXIMUM_TRANSACTIONCLASS_LENGTH - tclass.length());
            try {
                tclassNative = temp.getBytes("Cp1047");
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("code page conversion error", uee);
            }
        } else {
            tclassNative = null;
        }
    }

    public String getTransactionClass() {
        return tclass;
    }

    public byte[] getTransactionClassEBCDIC() {
        return tclassNative;
    }

    public boolean matchesPort(Integer port) {
        if (this.port.contains(-1) || this.port.contains(port)) {
            /*
             * There are two cases where we want to match. Either the configuration was
             * specified as a wildcard character (*) which we store as a value of -1, or
             * the configuration specified our port directly.
             */
            return true;
        }

        // At this point we know the ports don't match any of ours
        return false;
    }

    public boolean matchesMethod(String method) {
        if (this.method.contains("*") || this.method.contains(method)) {
            return true;
        }

        // At this point the incoming method doesn't match any of ours
        return false;
    }

    public boolean matchesHost(String host) {
        if (this.host.equals("*") || this.host.equals(host)) {
            return true;
        }

        // At this point the incoming host doesn't match
        return false;
    }

    public boolean matchesResource(String resource) {
        if (this.resource.equals(resource) || this.resource.equals("*")) {
            // This is the quick case where the resource matches exactly
            return true;
        } else if (this.resource.contains("*")) {
            /*
             * In this case the resource name contains wildcards. The resource has been
             * preprocessed at this point, and we should treat it as a regular expression.
             * This means all that is required to to match the incoming request against the
             * known regex.
             */
            if (resource.matches(this.resource)) {
                return true;
            }
        }

        // At this point the resources don't match
        return false;
    }
}
