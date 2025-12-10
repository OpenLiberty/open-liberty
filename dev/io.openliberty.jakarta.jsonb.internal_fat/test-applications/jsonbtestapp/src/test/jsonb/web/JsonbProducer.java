/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package test.jsonb.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

@ApplicationScoped
public class JsonbProducer {

    @Produces
    @Default
    @ApplicationScoped
    public Jsonb produceJsonb() {
        Jsonb created = JsonbBuilder.create();
        System.out.println("JsonbProducer.produceJsonb() invoked. Using provider: " + created.getClass());
        return created;
    }

    @Produces
    @DefaultJsonb
    @ApplicationScoped
    public Jsonb produceJsonbDefault() {
        Jsonb created = JsonbBuilder.create();
        System.out.println("JsonbProducer.produceJsonb() invoked. Using provider: " + created.getClass());
        return created;
    }

    @Produces
    @PrettyJsonb
    @ApplicationScoped
    public Jsonb produceJsonbPretty() {
        Jsonb created = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
        System.out.println("JsonbProducer.produceJsonb() invoked. Using provider: " + created.getClass());
        return created;
    }

}
