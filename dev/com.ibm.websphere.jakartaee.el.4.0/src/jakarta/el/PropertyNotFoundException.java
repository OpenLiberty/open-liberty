/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

/**
 * Thrown when a property could not be found while evaluating a {@link ValueExpression} or {@link MethodExpression}.
 *
 * <p>
 * For example, this could be triggered by an index out of bounds while setting an array value, or by an unreadable
 * property while getting the value of a JavaBeans property.
 * </p>
 *
 * @since Jakarta Server Pages 2.1
 */
public class PropertyNotFoundException extends ELException {

    private static final long serialVersionUID = 7876728153282609955L;

    // -------------------------------------
    /**
     * Creates a <code>PropertyNotFoundException</code> with no detail message.
     */
    public PropertyNotFoundException() {
        super();
    }

    // -------------------------------------
    /**
     * Creates a <code>PropertyNotFoundException</code> with the provided detail message.
     *
     * @param message the detail message
     */
    public PropertyNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a <code>PropertyNotFoundException</code> with the given root cause.
     *
     * @param exception the originating cause of this exception
     */
    public PropertyNotFoundException(Throwable exception) {
        super(exception);
    }

    /**
     * Creates a <code>PropertyNotFoundException</code> with the given detail message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public PropertyNotFoundException(String pMessage, Throwable pRootCause) {
        super(pMessage, pRootCause);
    }

}
