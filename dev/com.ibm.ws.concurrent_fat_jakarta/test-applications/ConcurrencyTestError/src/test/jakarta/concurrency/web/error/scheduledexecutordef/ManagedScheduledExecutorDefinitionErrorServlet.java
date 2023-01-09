/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency.web.error.scheduledexecutordef;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.servlet.annotation.WebServlet;

import componenttest.app.FATServlet;

@ManagedScheduledExecutorDefinition(name = "java:module/concurrent/executor-err-0", maxAsync = 0)
@SuppressWarnings("serial")
@WebServlet("/*")
public class ManagedScheduledExecutorDefinitionErrorServlet extends FATServlet {
}
