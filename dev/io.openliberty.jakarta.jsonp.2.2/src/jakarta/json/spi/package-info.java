/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/**
 * Service Provider Interface (SPI) to plug in implementations for
 * JSON processing objects.
 *
 * <p> {@link jakarta.json.spi.JsonProvider JsonProvider} is an abstract class 
 * that provides a service for creating JSON processing instances.
 * A <i>service provider</i> for {@code JsonProvider} provides an 
 * specific implementation by subclassing and implementing the methods in
 * {@code JsonProvider}. This enables using custom, efficient JSON processing
 * implementations (for e.g. parser and generator) other than the default ones.
 *
 * <p>The API locates and loads providers using {@link java.util.ServiceLoader}.
 *
 * Unless otherwise noted, passing a null argument to a constructor or method
 * in any class or interface in this package will cause a NullPointerException
 * to be thrown.
 *
 * @since JSON Processing 1.0
 */
package jakarta.json.spi;
