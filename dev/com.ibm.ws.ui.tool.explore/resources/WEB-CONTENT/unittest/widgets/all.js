/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
/**
 * require() will cause us to load all of our test modules. This is the equivalent
 * functionality of a test suite in JUnit.
 *
 * This is the 'primary' suite. It loads the 'all' suites for all of our components.
 */
define([
         'unittest/widgets/graphs/all',
         'unittest/widgets/FilterBarTests'
         ],
         function() {
});