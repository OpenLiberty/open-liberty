/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package com.ibm.wsspi.webcontainer.osgi.util;

import javax.servlet.ServletContext;

public class ExtendedDocumentRootUtils extends com.ibm.ws.webcontainer.util.ExtendedDocumentRootUtils
{
  public ExtendedDocumentRootUtils(ServletContext ctxt, String extendedDocumentRoot)
  {
    super(ctxt, extendedDocumentRoot);
  }

  public ExtendedDocumentRootUtils(String baseDir, String extendedDocumentRoot)
  {
    super(baseDir, extendedDocumentRoot);
  }
}
