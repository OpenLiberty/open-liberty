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
package mpRestClient12.jsonbContext;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.ext.ContextResolver;

public class JsonbContextResolver implements ContextResolver<Jsonb> {
    @Override
    public Jsonb getContext(Class<?> type) {
        JsonbConfig config = new JsonbConfig().
                withPropertyVisibilityStrategy(new PrivateVisibilityStrategy());
        return JsonbBuilder.newBuilder().
                withConfig(config).
                build();
    }
}