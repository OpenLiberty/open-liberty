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
package io.openliberty.microprofile.openapi20.validation;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public final class OASValidationResult {

    private final List<ValidationEvent> events = new ArrayList<>();

    public List<ValidationEvent> getEvents() {
        return events;
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }

    public static class ValidationEvent {

        public enum Severity {
            ERROR, WARNING, INFO
        }

        public ValidationEvent(Severity severity, String location, String message) {
            this.severity = severity;
            this.location = location;
            this.message = message;
        }

        public final Severity severity;
        public final String location;
        public final String message;
    }
}
