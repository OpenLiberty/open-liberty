/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.content;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import org.mcpjava.server.Role;
import org.mcpjava.server.content.Annotations;

import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 *
 */
@JsonbTypeSerializer(AnnotationsImpl.Serializer.class)
public record AnnotationsImpl(Set<Role> audienceValue,
                              Instant lastModifiedValue,
                              OptionalDouble priority) implements Annotations {

    @Override
    public Optional<Set<Role>> audience() {
        return Optional.ofNullable(audienceValue);
    }

    @Override
    public Optional<Instant> lastModified() {
        return Optional.ofNullable(lastModifiedValue);
    }

    public static class Builder implements Annotations.Builder {

        private Set<Role> audience;
        private Instant lastModified;
        private OptionalDouble priority = OptionalDouble.empty();

        @Override
        public Annotations.Builder setAudience(Set<Role> audience) {
            this.audience = audience;
            return this;
        }

        @Override
        public Builder setAudience(Role... roles) {
            this.audience = Set.of(roles);
            return this;
        }

        @Override
        public Annotations.Builder setLastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public Annotations.Builder setPriority(double priority) {
            this.priority = OptionalDouble.of(priority);
            return this;
        }

        @Override
        public Annotations build() {
            return new AnnotationsImpl(audience, lastModified, priority);
        }
    }

    public static class Serializer implements JsonbSerializer<AnnotationsImpl> {

        @Override
        public void serialize(AnnotationsImpl annotations, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            if (annotations.audienceValue() != null) {
                json.writeStartArray("audience");
                for (var audience : annotations.audienceValue()) {
                    ctx.serialize(audience, json);
                }
                json.writeEnd();
            }
            ctx.serialize("lastModified", annotations.lastModified(), json);
            ctx.serialize("priority", annotations.priority(), json);
            json.writeEnd();
        }

    }

}
