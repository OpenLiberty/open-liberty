/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

//https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/opentelemetry-annotations-1.0/javaagent/src/main/java/io/opentelemetry/javaagent/instrumentation/otelannotations/MethodRequest.java

package io.openliberty.microprofile.telemetry.internal.cdi;

import java.lang.reflect.Method;

public final class MethodRequest {
  private final Method method;
  private final Object[] args;

  public MethodRequest(Method method, Object[] args) {
    this.method = method;
    this.args = args;
  }

  public Method getMethod() {
    return this.method;
  }

  public Object[] getArgs() {
    return this.args;
  }
}