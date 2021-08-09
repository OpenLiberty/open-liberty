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

package mpRestClient13.hostnameVerifier;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


public class MyHostnameVerifier implements HostnameVerifier {
    private static Logger LOG = Logger.getLogger(MyHostnameVerifier.class.getName());

    static AtomicInteger INVOCATION_COUNT = new AtomicInteger(0);


    @Override
    public boolean verify(String hostname, SSLSession session) {
        LOG.info("MyHostnameVerifier invoked " + INVOCATION_COUNT.incrementAndGet() + " times");
        return true;
    }

}
