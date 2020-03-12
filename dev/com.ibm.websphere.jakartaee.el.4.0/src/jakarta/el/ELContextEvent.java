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

import java.util.EventObject;

/**
 * An event which indicates that an {@link ELContext} has been created. The source object is the ELContext that was
 * created.
 *
 * @see ELContext
 * @see ELContextListener
 * 
 * @since Jakarta Server Pages 2.1
 */
public class ELContextEvent extends EventObject {

    private static final long serialVersionUID = 1255131906285426769L;

    /**
     * Constructs an ELContextEvent object to indicate that an <code>ELContext</code> has been created.
     *
     * @param source the <code>ELContext</code> that was created.
     */
    public ELContextEvent(ELContext source) {
        super(source);
    }

    /**
     * Returns the <code>ELContext</code> that was created. This is a type-safe equivalent of the {@link #getSource} method.
     *
     * @return the ELContext that was created.
     */
    public ELContext getELContext() {
        return (ELContext) getSource();
    }
}
