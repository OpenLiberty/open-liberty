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
 * <p>This interface represents an implementation of a JSON Merge Patch
 * as defined by <a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>.
 * </p>
 * <p>A {@code JsonMergePatch} can be instantiated with {@link Json#createMergePatch(JsonValue)}
 * by specifying the patch operations in a JSON Merge Patch or using {@link Json#createMergeDiff(JsonValue, JsonValue)}
 * to create a JSON Merge Patch based on the difference between two {@code JsonValue}s.
 * </p>
 * The following illustrates both approaches.
 * <p>1. Construct a JsonMergePatch with an existing JSON Merge Patch.
 * <pre>{@code
 *   JsonValue contacts = ... ; // The target to be patched
 *   JsonValue patch = ...  ; // JSON Merge Patch
 *   JsonMergePatch mergePatch = Json.createMergePatch(patch);
 *   JsonValue result = mergePatch.apply(contacts);
 * } </pre>
 * 2. Construct a JsonMergePatch from a difference between two {@code JsonValue}s.
 * <pre>{@code
 *   JsonValue source = ... ; // The source object
 *   JsonValue target = ... ; // The modified object
 *   JsonMergePatch mergePatch = Json.createMergeDiff(source, target); // The diff between source and target in a Json Merge Patch format
 * } </pre>
 *
 * @see <a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>
 *
 * @since 1.1
 */
public interface JsonMergePatch {

    /**
     * Applies the JSON Merge Patch to the specified {@code target}.
     * The target is not modified by the patch.
     *
     * @param target the target to apply the merge patch
     * @return the transformed target after the patch
     */
    JsonValue apply(JsonValue target);

    /**
     * Returns the {@code JsonMergePatch} as {@code JsonValue}.
     *
     * @return this {@code JsonMergePatch} as {@code JsonValue}
     */
    JsonValue toJsonValue();
}
