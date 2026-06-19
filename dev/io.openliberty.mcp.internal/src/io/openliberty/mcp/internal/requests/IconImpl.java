/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.Icon;

import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * Record implementation of {@link Icon}
 */
public record IconImpl(String src, String mimeTypeValue, List<String> sizes, Theme themeValue) implements Icon {

    public IconImpl {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(sizes, "sizes must not be null");
    }

    @Override
    public Optional<String> mimeType() {
        return Optional.of(mimeTypeValue);
    }

    @Override
    public Optional<Theme> theme() {
        return Optional.of(themeValue);
    }

    /**
     * Builder implementation for {@link Icon}
     */
    public static class Builder implements Icon.Builder {

        private boolean anySize;
        private List<String> sizes = null;
        private String src;
        private Theme theme;
        private String mimeType;

        public Builder(String src) {
            Objects.requireNonNull(src, "src must not be null");
            this.src = src;
        }

        @Override
        public Icon.Builder addSize(int width, int height) {
            if (anySize) {
                throw new IllegalStateException("Cannot add sizes after calling setAnySize");
            }
            if (sizes == null) {
                sizes = new ArrayList<>();
            }
            sizes.add(width + "x" + height);
            return this;
        }

        @Override
        public Icon.Builder setAnySize() {
            if (!sizes.isEmpty()) {
                throw new IllegalStateException("Cannot call setAnySize after calling addSize");
            }
            anySize = true;
            return this;
        }

        @Override
        public Icon.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public Icon.Builder setTheme(Theme theme) {
            this.theme = theme;
            return this;
        }

        @Override
        public Icon build() {
            if (anySize) {
                sizes = List.of("any");
            } else if (sizes == null) {
                sizes = List.of();
            } else {
                sizes = List.copyOf(this.sizes);
            }
            return new IconImpl(src, mimeType, sizes, theme);
        }
    }

    public static class IconTO {
        public String src;
        public String mimeType;
        public List<String> sizes;
        public String theme;
    }

    public static class Adapter implements JsonbAdapter<Icon, IconTO> {

        @Override
        public Icon adaptFromJson(IconTO to) throws Exception {
            return new IconImpl(to.src,
                                to.mimeType,
                                to.sizes,
                                to.theme == null ? null : Theme.valueOf(to.theme.toUpperCase()));
        }

        @Override
        public IconTO adaptToJson(Icon icon) throws Exception {
            IconImpl obj = (IconImpl) icon;
            IconTO result = new IconTO();
            result.src = obj.src;
            result.mimeType = obj.mimeTypeValue;
            result.sizes = obj.sizes;
            result.theme = obj.themeValue == null ? null : obj.themeValue.name().toLowerCase();
            return result;
        }

    }

}
