/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.core.jackson.mixin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Define mix-in annotations to use for augmenting annotations that processable (serializable / deserializable) classes have.
 * Mixing in is done when introspecting class annotations and properties.
 * All annotations from mixinSource are taken to override annotations that target (or its supertypes) has.
 */
public abstract class ExtensionsMixin {

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @JsonAnySetter
    public abstract void addExtension(String name, Object value);

    @JsonProperty("enum")
    public abstract void getEnumeration();

    @JsonProperty("default")
    public abstract void getDefaultValue();

    @JsonProperty("$ref")
    public abstract void getRef();

}
