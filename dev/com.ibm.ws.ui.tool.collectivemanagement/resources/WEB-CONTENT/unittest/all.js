/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * require() will cause us to load all of our test modules. This is the equivalent
 * functionality of a test suite in JUnit.
 *
 * This is the "master" suite. It loads the 'all' suites for all of our components.
 */
require([
         "unittest/util/all"
         ]);