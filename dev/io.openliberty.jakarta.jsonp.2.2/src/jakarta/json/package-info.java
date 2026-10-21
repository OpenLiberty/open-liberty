/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides an object model API to process <a href="http://json.org/">JSON</a>.
 *
 * <p>The object model API is a high-level API that provides immutable object
 * models for JSON object and array structures. These JSON structures are
 * represented as object models using the Java types {@link jakarta.json.JsonObject}
 * and {@link jakarta.json.JsonArray}. The interface {@code jakarta.json.JsonObject} provides
 * a {@link java.util.Map} view to access the unordered collection of zero or
 * more name/value pairs from the model. Similarly, the interface
 * {@code JsonArray} provides a {@link java.util.List} view to access the
 * ordered sequence of zero or more values from the model.
 *
 * <p>The object model API uses builder patterns to create and modify
 * these object models. The classes {@link jakarta.json.JsonObjectBuilder} and 
 * {@link jakarta.json.JsonArrayBuilder} provide methods to create and modify models
 * of type {@code JsonObject} and {@code JsonArray} respectively.
 *
 * <p>These object models can also be created from an input source using
 * the class {@link jakarta.json.JsonReader}. Similarly, these object models
 * can be written to an output source using the class {@link jakarta.json.JsonWriter}.
 * <p>
 * This package includes several classes that implement other JSON related
 * standards: <a href="http://tools.ietf.org/html/rfc6901">JSON Pointer</a>,
 * <a Href="http://tools.ietf.org/html/rfc6902">JSON Patch</a>, and
 * <a Href="http://tools.ietf.org/html/rfc7396">JSON Merge Patch</a>.
 * They can be used to retrieve, transform or manipulate values in an
 * object model.
 */
package jakarta.json;
