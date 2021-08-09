/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient10.basicEJB;

import java.util.Set;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@LocalBean
@Stateless
public class MyEJB {

    @Inject
    @RestClient
    BasicServiceClient client;

    public Set<String> getWidgetNames() {
        return client.getWidgetNames();
    }

    public Widget getWidget(String name) throws UnknownWidgetException {
        return client.getWidget(name);
    }

    public void createNewWidget(Widget widget) throws DuplicateWidgetException {
        client.createNewWidget(widget);
    }

    public Widget putWidget(Widget widget) {
        return client.putWidget(widget);
    }

    public Widget removeWidget(String name) throws UnknownWidgetException {
        return client.removeWidget(name);
    }
}
