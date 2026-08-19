/*
 * Copyright (c) 2015, 2021 Oracle and/or its affiliates. All rights reserved.
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
 * A builder for constructing a JSON Patch as defined by
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a> by adding
 * JSON Patch operations incrementally.
 * <p>
 * The following illustrates the approach.
 * <pre>
 *   JsonPatchBuilder builder = Json.createPatchBuilder();
 *   JsonPatch patch = builder.add("/John/phones/office", "1234-567")
 *                            .remove("/Amy/age")
 *                            .build();
 * </pre>
 * The result is equivalent to the following JSON Patch.
 * <pre>
 * [
 *    {"op" = "add", "path" = "/John/phones/office", "value" = "1234-567"},
 *    {"op" = "remove", "path" = "/Amy/age"}
 * ] </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPatchBuilder {

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, JsonValue value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, String value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, int value);

    /**
     * Adds an "add" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder add(String path, boolean value);

    /**
     * Adds a "remove" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder remove(String path);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, JsonValue value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, String value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, int value);

    /**
     * Adds a "replace" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder replace(String path, boolean value);

    /**
     * Adds a "move" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param from the "from" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder move(String path, String from);

    /**
     * Adds a "copy" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param from the "from" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder copy(String path, String from);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, JsonValue value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, String value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, int value);

    /**
     * Adds a "test" JSON Patch operation.
     *
     * @param path the "path" member of the operation. Must be a valid escaped JSON-Pointer string.
     * @param value the "value" member of the operation
     * @return this JsonPatchBuilder
     */
    JsonPatchBuilder test(String path, boolean value);


    /**
     * Returns the JSON Patch.
     *
     * @return a JSON Patch
     */
    JsonPatch build();

}
