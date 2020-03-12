/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates and others.
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
 * Thrown when a method could not be found while evaluating a {@link MethodExpression}.
 *
 * @see MethodExpression
 * @since Jakarta Server Pages 2.1
 */
public class MethodNotFoundException extends ELException {

    private static final long serialVersionUID = 7727548537051164640L;

    /**
     * Creates a <code>MethodNotFoundException</code> with no detail message.
     */
    public MethodNotFoundException() {
        super();
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the provided detail message.
     *
     * @param message the detail message
     */
    public MethodNotFoundException(String message) {
        super(message);
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the given root cause.
     *
     * @param exception the originating cause of this exception
     */
    public MethodNotFoundException(Throwable exception) {
        super(exception);
    }

    /**
     * Creates a <code>MethodNotFoundException</code> with the given detail message and root cause.
     *
     * @param pMessage the detail message
     * @param pRootCause the originating cause of this exception
     */
    public MethodNotFoundException(String pMessage, Throwable pRootCause) {
        super(pMessage, pRootCause);
    }
}
