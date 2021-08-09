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
package com.ibm.ws.http.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.wsspi.http.WelcomePage;

@Component(service = WelcomePage.class,
           property = { "service.vendor=IBM" })
public class DefaultWelcomePage implements WelcomePage {

  private Bundle bundle;
  private String version;

  @Activate
  protected void activate(BundleContext context) {
    bundle = context.getBundle();

    try {
      ProductInfo info = ProductInfo.getAllProductInfo().get("io.openliberty");

      if (info != null) {
        version = info.getVersion();
      }
    } catch (ProductInfoParseException | DuplicateProductInfoException | ProductInfoReplaceException e) {
      // just ignore this problem. Worst case here is the message that says an update is available isn't there.
    }
  }

  public InputStream openWelcomePage(String url) {
    if ("/".equals(url)) {
      url = "/index.html";
    }

    if ("/version.js".equals(url)) {
        StringBuilder versionJs = new StringBuilder();
        versionJs.append("var current = {\r\n");
        versionJs.append(" version: \"");
        if (version != null) {
          versionJs.append(version);
        }
        versionJs.append("\"\r\n");
        versionJs.append("}");
        try {
          return new ByteArrayInputStream(versionJs.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
          // This should never happen because UTF-8 is required in Java. #famouslastwords
          return null;
        }
    } else {
      return safeOpen("/OSGI-INF/welcome" + url);
    }
  }

  // Attempt to open the file in the bundle and return null if something goes wrong.
  private InputStream safeOpen(String file) {
    URL url = bundle.getEntry(file);
    if (url != null) {
      try {
        return url.openStream();
      } catch (IOException e) {
        // if we get an IOException just return null for default page.
      }
    }
    return null;
  }

  public InputStream openNotFoundPage() {
     return safeOpen("/OSGI-INF/notFound/index.html");
  }
}
