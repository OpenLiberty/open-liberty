/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/* Temporary file pending public availability of api jar */
package javax.servlet.http;

import java.util.Set;

public interface PushBuilder {

    PushBuilder method(String method) throws IllegalArgumentException;

    PushBuilder queryString(String queryString);

    PushBuilder sessionId(String sessionId);

    PushBuilder setHeader(String name, String value);

    PushBuilder addHeader(String name, String value);

    PushBuilder removeHeader(String name);

    PushBuilder path(String path);

    void push() throws IllegalStateException;

    String getMethod();

    String getQueryString();

    String getSessionId();

    Set<String> getHeaderNames();

    String getHeader(String name);

    String getPath();

}
