/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import java.io.InputStream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.wsspi.http.WelcomePage;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Component that handles managing the WelcomePage for the HttpDispatcherLink
 */
@Component(service = WelcomePageHelper.class,
           immediate = true,
           property = { "service.vendor=IBM" })
public class WelcomePageHelper {
  private static ConcurrentServiceReferenceSet<WelcomePage> welcomePages = new ConcurrentServiceReferenceSet("welcomePage");

  @Activate
  protected void activeate(ComponentContext ctx) {
    welcomePages.activate(ctx);
  }

  @Deactivate
  protected void deactiveate(ComponentContext ctx) {
    welcomePages.deactivate(ctx);
  }

  @Trivial
  @Reference(policy = ReferencePolicy.DYNAMIC,
             service = WelcomePage.class,
             policyOption = ReferencePolicyOption.GREEDY,
             cardinality = ReferenceCardinality.MULTIPLE,
             name="welcomePage")
  protected void setWelcomePage(ServiceReference<WelcomePage> ref) {
    welcomePages.addReference(ref);
  }

  protected void unsetWelcomePage(ServiceReference<WelcomePage> ref) {
    welcomePages.removeReference(ref);
  }

  public static InputStream getWelcomePageStream(String url) {
    if (!welcomePages.isEmpty()) {
      WelcomePage page = welcomePages.getHighestRankedService();
      if (page != null) {
        return page.openWelcomePage(url);
      }
    }
    return null;
  }

  public static InputStream getNotFoundStream() {
    if (!welcomePages.isEmpty()) {
      WelcomePage page = welcomePages.getHighestRankedService();
      if (page != null) {
        return page.openNotFoundPage();
      }
    }
    return null;
  }
}
