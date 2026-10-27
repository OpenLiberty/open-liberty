/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

/**
 * Super type for the two structured types in JSON ({@link JsonObject object}s
 * and {@link JsonArray array}s).
 */
public interface JsonStructure extends JsonValue {

    /**
     * Get the value referenced by the provided JSON Pointer in the JsonStructure.
     *
     * @param jsonPointer the JSON Pointer
     * @return the {@code JsonValue} at the referenced location
     * @throws JsonException if the JSON Pointer is malformed, or if it references
     *     a non-existing member or value.
     *
     * @since 1.1
     */
    default public JsonValue getValue(String jsonPointer) {
        return Json.createPointer(jsonPointer).getValue(this);
    }
}
