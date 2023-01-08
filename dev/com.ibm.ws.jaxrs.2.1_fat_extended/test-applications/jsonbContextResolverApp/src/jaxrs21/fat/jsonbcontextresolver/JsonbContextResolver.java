/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package jaxrs21.fat.jsonbcontextresolver;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonbContextResolver implements ContextResolver<Jsonb> {
    @Context
    HttpHeaders httpHeaders;

    @Override
    public Jsonb getContext(Class<?> type) {
        if (!"CanReadHeaderFromContextInjection".equals(httpHeaders.getHeaderString("MyHeader"))) {
            throw new IllegalStateException("Context injection worked, but we didn't get the expected header");
        }
        if (!Person.class.isAssignableFrom(type)) {
            throw new IllegalStateException("The wrong class was passed to getContext " + type);
        }
        JsonbConfig config = new JsonbConfig().
                withPropertyVisibilityStrategy(new PrivateVisibilityStrategy());
        return JsonbBuilder.newBuilder().
                withConfig(config).
                build();
    }
}
