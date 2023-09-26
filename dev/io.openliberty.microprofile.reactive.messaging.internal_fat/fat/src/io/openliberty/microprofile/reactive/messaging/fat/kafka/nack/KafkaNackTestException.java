/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.microprofile.reactive.messaging.fat.kafka.nack;

@SuppressWarnings("serial")
public class KafkaNackTestException extends Exception {

    public KafkaNackTestException(String message, Throwable cause) {
        super(message, cause);
    }

    public KafkaNackTestException(String message) {
        super(message);
    }

}
