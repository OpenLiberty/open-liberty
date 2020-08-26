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
package com.ibm.testapp.g3store.grpcConsumer.security;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;

/**
 * @author anupag
 *
 */
public class TestAppCallCredentials extends CallCredentials {

    private static Logger log = Logger.getLogger(TestAppCallCredentials.class.getName());

    String _authHeader = null;

    public TestAppCallCredentials(String authHeader) {
        _authHeader = authHeader;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, final MetadataApplier applier) {

        appExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Metadata headers = new Metadata();
                    Metadata.Key<String> authHeader = Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER);

                    if (log.isLoggable(Level.FINE)) {
                        log.finest("Auth_CallCred: Add auth header to Metadata");
                    }

                    headers.put(authHeader, _authHeader);
                    applier.apply(headers);

                } catch (Throwable ex) {
                    applier.fail(Status.UNAUTHENTICATED.withCause(ex));
                }
            }
        });

    }

    @Override
    public void thisUsesUnstableApi() {
        // TODO Auto-generated method stub

    }

}
