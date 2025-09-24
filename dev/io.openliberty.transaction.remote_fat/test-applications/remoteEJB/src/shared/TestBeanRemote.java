/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package shared;

import javax.transaction.SystemException;

public interface TestBeanRemote {
    public String getProperty(String var);

    public String mandatory() throws SystemException;

    public String never() throws SystemException;

    public String notSupported() throws SystemException;

    public String required() throws SystemException;

    public String requiresNew() throws SystemException;

    public String supports() throws SystemException;
}