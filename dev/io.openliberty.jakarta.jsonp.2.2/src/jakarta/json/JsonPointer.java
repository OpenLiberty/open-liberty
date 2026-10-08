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
 * <p>This interface represents an immutable implementation of a JSON Pointer
 * as defined by <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
 * </p>
 * <p> A JSON Pointer, when applied to a target {@link JsonValue},
 * defines a reference location in the target.</p>
 * <p> An empty JSON Pointer string defines a reference to the target itself.</p>
 * <p> If the JSON Pointer string is non-empty, it must be a sequence
 * of '/' prefixed tokens, and the target must either be a {@link JsonArray}
 * or {@link JsonObject}. If the target is a {@code JsonArray}, the pointer
 * defines a reference to an array element, and the last token specifies the index.
 * If the target is a {@link JsonObject}, the pointer defines a reference to a
 * name/value pair, and the last token specifies the name.
 * </p>
 * <p> The method {@link #getValue getValue()} returns the referenced value.
 * Methods {@link #add add()}, {@link #replace replace()},
 * and {@link #remove remove()} execute operations specified in
 * <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>. </p>
 *
 * @see <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>
 * @see <a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>
 *
 * @since 1.1
 */
public interface JsonPointer {

    /**
     * Adds or replaces a value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     * <ol>
     * <li>If the reference is the target (empty JSON Pointer string),
     * the specified {@code value}, which must be the same type as
     * specified {@code target}, is returned.</li>
     * <li>If the reference is an array element, the specified {@code value} is inserted
     * into the array, at the referenced index. The value currently at that location, and
     * any subsequent values, are shifted to the right (adds one to the indices).
     * Index starts with 0. If the reference is specified with a "-", or if the
     * index is equal to the size of the array, the value is appended to the array.</li>
     * <li>If the reference is a name/value pair of a {@code JsonObject}, and the
     * referenced value exists, the value is replaced by the specified {@code value}.
     * If the value does not exist, a new name/value pair is added to the object.</li>
     * </ol>
     *
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be added
     * @return the transformed {@code target} after the value is added.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the reference is an array element and
     * the index is out of range ({@code index < 0 || index > array size}),
     * or if the pointer contains references to non-existing objects or arrays.
     */
    <T extends JsonStructure> T add(T target, JsonValue value);

    /**
     * Removes the value at the reference location in the specified {@code target}.
     *
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @return the transformed {@code target} after the value is removed.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    <T extends JsonStructure> T remove(T target);

    /**
     * Replaces the value at the referenced location in the specified
     * {@code target} with the specified {@code value}.
     *
     * @param <T> the target type, must be a subtype of {@link JsonValue}
     * @param target the target referenced by this {@code JsonPointer}
     * @param value the value to be stored at the referenced location
     * @return the transformed {@code target} after the value is replaced.
     * @throws NullPointerException if {@code target} is {@code null}
     * @throws JsonException if the referenced value does not exist,
     *    or if the reference is the target.
     */
    <T extends JsonStructure> T replace(T target, JsonValue value);

    /**
     * Returns {@code true} if there is a value at the referenced location in the specified {@code target}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return {@code true} if this pointer points to a value in a specified {@code target}.
     */
    boolean containsValue(JsonStructure target);

    /**
     * Returns the value at the referenced location in the specified {@code target}.
     *
     * @param target the target referenced by this {@code JsonPointer}
     * @return the referenced value in the target.
     * @throws NullPointerException if {@code target} is null
     * @throws JsonException if the referenced value does not exist
     */
    JsonValue getValue(JsonStructure target);

    /**
     * Returns the string representation of this JSON Pointer.
     * The value to be returned is an empty string or a sequence of '{@code /}' prefixed tokens.
     *
     * @return the valid escaped JSON Pointer string.
     */
    @Override
    String toString();
}
