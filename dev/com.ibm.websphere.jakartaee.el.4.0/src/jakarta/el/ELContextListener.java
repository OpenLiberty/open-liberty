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
 * The listener interface for receiving notification when an {@link ELContext} is created.
 *
 * @see ELContext
 * @see ELContextEvent
 * 
 * @since Jakarta Server Pages 2.1
 */
public interface ELContextListener extends java.util.EventListener {

    /**
     * Invoked when a new <code>ELContext</code> has been created.
     *
     * @param ece the notification event.
     */
    void contextCreated(ELContextEvent ece);

}
