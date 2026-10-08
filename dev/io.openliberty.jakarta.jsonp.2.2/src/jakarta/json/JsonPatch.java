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
 * <p>This interface represents an immutable implementation of a JSON Patch
 * as defined by <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>.
 * </p>
 * <p>A {@code JsonPatch} can be instantiated with {@link Json#createPatch(JsonArray)}
 * by specifying the patch operations in a JSON Patch. Alternately, it
 * can also be constructed with a {@link JsonPatchBuilder}.
 * </p>
 * The following illustrates both approaches.
 * <p>1. Construct a JsonPatch with a JSON Patch.
 * <pre>{@code
 *   JsonArray contacts = ... // The target to be patched
 *   JsonArray patch = ...  ; // JSON Patch
 *   JsonPatch jsonpatch = Json.createPatch(patch);
 *   JsonArray result = jsonpatch.apply(contacts);
 * } </pre>
 * 2. Construct a JsonPatch with JsonPatchBuilder.
 * <pre>{@code
 *   JsonPatchBuilder builder = Json.createPatchBuilder();
 *   JsonArray result = builder.add("/John/phones/office", "1234-567")
 *                             .remove("/Amy/age")
 *                             .build()
 *                             .apply(contacts);
 * } </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPatch {

    /**
     * This enum represents the list of valid JSON Patch operations
     * as defined by <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>.
     *
     * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
     */
    enum Operation {

        /**
         * "add" operation.
         */
        ADD("add"),

        /**
         * "remove" operation.
         */
        REMOVE("remove"),

        /**
         * "replace" operation.
         */
        REPLACE("replace"),

        /**
         * "move" operation.
         */
        MOVE("move"),

        /**
         * "copy" operation.
         */
        COPY("copy"),

        /**
         * "test" operation.
         */
        TEST("test");

        /** Operation name */
        private final String operationName;

        /**
         * Create an enum constant with given {@code operationName}.
         * @param operationName the operation name for the enum constant
         */
        private Operation(String operationName) {
            this.operationName = operationName;
        }

        /**
         * Returns enum constant name as lower case string.
         *
         * @return lower case name of the enum constant
         */
        public String operationName() {
            return operationName;
        }

        /**
         * Returns the enum constant with the specified name.
         *
         * @param operationName {@code operationName} to convert to the enum constant.
         * @return the enum constant for given {@code operationName}
         * @throws JsonException if given {@code operationName} is not recognized
         */
        public static Operation fromOperationName(String operationName) {
            for (Operation op : values()) {
                if (op.operationName().equalsIgnoreCase(operationName)) {
                    return op;
                }
            }
            throw new JsonException("Illegal value for the operationName of the JSON patch operation: " + operationName);
        }
    }

    /**
     * Applies the patch operations to the specified {@code target}.
     * The target is not modified by the patch.
     *
     * @param <T> the target type, must be a subtype of {@link JsonStructure}
     * @param target the target to apply the patch operations
     * @return the transformed target after the patch
     * @throws JsonException if the supplied JSON Patch is malformed or if
     *    it contains references to non-existing members
     */
    <T extends JsonStructure> T apply(T target);

    /**
     * Returns the {@code JsonPatch} as {@code JsonArray}.
     *
     * @return this {@code JsonPatch} as {@code JsonArray}
     */
    JsonArray toJsonArray();

}
