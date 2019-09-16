/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.kafka.adapter;

/**
 *
 */
public class KafkaAdapterException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 8401273064700931173L;

    /**
     * @param cause
     */
    public KafkaAdapterException(Exception cause) {
        super(cause);
    }

}
