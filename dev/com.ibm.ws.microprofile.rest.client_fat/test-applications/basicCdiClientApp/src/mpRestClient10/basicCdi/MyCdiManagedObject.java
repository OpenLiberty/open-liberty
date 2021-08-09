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
package mpRestClient10.basicCdi;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Dependent
public class MyCdiManagedObject {

    private BasicServiceClient injectedFromCtor;

    private BasicServiceClient injectedFromSetter;

    @Inject
    @RestClient
    private BasicServiceClient injectedFromField;

    @Inject
    @RestClient
    public MyCdiManagedObject(BasicServiceClient client) {
        this.injectedFromCtor = client;
    }

    @Inject
    @RestClient
    public void setClient(BasicServiceClient client) {
        this.injectedFromSetter = client;
    }

    public BasicServiceClient getClientFromCtor() {
        return injectedFromCtor;
    }

    public BasicServiceClient getClientFromField() {
        return injectedFromField;
    }

    public BasicServiceClient getClientFromMethod() {
        return injectedFromSetter;
    }
}
