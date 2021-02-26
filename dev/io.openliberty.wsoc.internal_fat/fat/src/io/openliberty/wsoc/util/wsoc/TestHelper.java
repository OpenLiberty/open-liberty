/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

/**
 * tests should implement this so test runners can get set and get a hold of stored test result info.
 */
public interface TestHelper {

    public void addTestResponse(WsocTestContext wtr);

    public WsocTestContext getTestResponse();

}
