/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.io.InputStream;

/**
 * Implementations of this service can override the default branding and
 * information applied to Open Liberty. By default when no apps are bound to /
 * the welcome page is shown, and if a 404 is returned a pretty 404 is returned
 * as a default. This can be overridden by registering a service with a higher
 * service ranking than 0.
 */
public interface WelcomePage {
  /**
   * This is called when the welcome page is being displayed. It can be used to
   * return any text or image file required for the page. Resources in
   * sub-directories could clash with deployed applications, so care needs to be
   * taken.
   *
   * If null is returned a  404 response will be returned.
   *
   * @param url the url being requested.
   * @return the InputStream for the 404 response, or null if there is none.
   */
  public InputStream openWelcomePage(String url);

  /**
   * The not found page is returned for a 404 page if there is no replacement.
   * The returned page must be self contained, it can't have references to other
   * local pages since there is no way to guarantee these pages do not clash
   * with other locations.
   *
   * If null is returned a defualt and basic 404 response will be returned.
   *
   * @return the InputStream for the 404 response, or null if there is none.
   */
  public InputStream openNotFoundPage();
}
