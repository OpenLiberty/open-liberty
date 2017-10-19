/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.providerPriority;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

@ApplicationPath("/rest")
@Path("/test")
public class ProviderPriorityTestApp extends Application {

    @Produces(MediaType.TEXT_PLAIN)
    @PUT
    public MyObject get(MyObject mo1) {
        MyObject mo2 = new MyObject();
        mo2.setMyString(mo1.getMyString());
        mo2.setMyInt(mo1.getMyInt());
        mo2.setContextResolverVersionFromReader(mo1.getContextResolverVersionFromReader());
        mo2.setMbrVersion(mo1.getMbrVersion());
        return mo2;
    }

}