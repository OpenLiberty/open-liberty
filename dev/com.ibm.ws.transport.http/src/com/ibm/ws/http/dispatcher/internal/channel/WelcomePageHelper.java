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
import java.util.concurrent.Callable;

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
import com.ibm.ws.staticvalue.StaticValue;

/**
 * Component that handles managing the WelcomePage for the HttpDispatcherLink
 */
@Component(service = WelcomePageHelper.class,
           immediate = true,
           property = { "service.vendor=IBM" })
public class WelcomePageHelper {
  private static StaticValue<ConcurrentServiceReferenceSet<WelcomePage>> welcomePages = StaticValue.createStaticValue(new Callable<ConcurrentServiceReferenceSet<WelcomePage>>(){
    @Override
    public ConcurrentServiceReferenceSet<WelcomePage> call() throws Exception {
        return new ConcurrentServiceReferenceSet<WelcomePage>("welcomePage");
    }
  });

  @Activate
  protected void activeate(ComponentContext ctx) {
    welcomePages.get().activate(ctx);
  }

  @Deactivate
  protected void deactiveate(ComponentContext ctx) {
    welcomePages.get().deactivate(ctx);
  }

  @Trivial
  @Reference(policy = ReferencePolicy.DYNAMIC,
             service = WelcomePage.class,
             policyOption = ReferencePolicyOption.GREEDY,
             cardinality = ReferenceCardinality.MULTIPLE,
             name="welcomePage")
  protected void setWelcomePage(ServiceReference<WelcomePage> ref) {
    welcomePages.get().addReference(ref);
  }

  protected void unsetWelcomePage(ServiceReference<WelcomePage> ref) {
    welcomePages.get().removeReference(ref);
  }

  public static InputStream getWelcomePageStream(String url) {
    if (!welcomePages.get().isEmpty()) {
      WelcomePage page = welcomePages.get().getHighestRankedService();
      if (page != null) {
        return page.openWelcomePage(url);
      }
    }
    return null;
  }

  public static InputStream getNotFoundStream() {
    if (!welcomePages.get().isEmpty()) {
      WelcomePage page = welcomePages.get().getHighestRankedService();
      if (page != null) {
        return page.openNotFoundPage();
      }
    }
    return null;
  }
}
