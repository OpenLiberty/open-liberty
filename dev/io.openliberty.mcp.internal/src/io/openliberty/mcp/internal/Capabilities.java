/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Optional;

/**
 * Data objects for describing MCP Capabilities
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-06-18/basic/lifecycle#capability-negotiation">Capbility Negotiation</a>
 */
public class Capabilities {

    public static interface ClientCapability {};

    public static record Roots(boolean listChanged) implements ClientCapability {};

    public static record Sampling() implements ClientCapability {};

    public static record Elicitation() implements ClientCapability {};

    public static interface ServerCapability {};

    public static record Prompts(boolean listChanged) implements ServerCapability {};

    public static record Resources(boolean subscribe, boolean listChanged) implements ServerCapability {};

    public static record Tools(boolean listChanged) implements ServerCapability {};

    public static record Logging() implements ServerCapability {};

    public static record Completions() implements ServerCapability {};

    /**
     * Describes the capabilities of a client.
     * <p>
     * Received by the server during intitialization.
     */
    public static record ClientCapabilities(Optional<Roots> roots,
                                            Optional<Sampling> sampling,
                                            Optional<Elicitation> elicitation) {

        /**
         * Constructs a capabilities object for a client from a list of capabilities
         *
         * @param caps the capabilities
         * @return the capabilities object
         */
        public static ClientCapabilities of(ClientCapability... caps) {
            Optional<Roots> roots = Optional.empty();
            Optional<Sampling> sampling = Optional.empty();
            Optional<Elicitation> elicitation = Optional.empty();

            for (ClientCapability cap : caps) {
                if (cap instanceof Roots r) {
                    roots = Optional.of(r);
                } else if (cap instanceof Sampling s) {
                    sampling = Optional.of(s);
                } else if (cap instanceof Elicitation e) {
                    elicitation = Optional.of(e);
                }
            }

            return new ClientCapabilities(roots, sampling, elicitation);
        }
    };

    /**
     * Describes the capabilities of a server.
     * <p>
     * Sent by the server during initialization.
     */
    public static record ServerCapabilities(Optional<Prompts> prompts,
                                            Optional<Resources> resources,
                                            Optional<Tools> tools,
                                            Optional<Logging> logging,
                                            Optional<Completions> completions) {

        /**
         * Constructs a capabilities object for a server from a list of capabilities
         *
         * @param caps the capabilities
         * @return the capabilities object
         */
        public static ServerCapabilities of(ServerCapability... caps) {
            Optional<Prompts> prompts = Optional.empty();
            Optional<Resources> resources = Optional.empty();
            Optional<Tools> tools = Optional.empty();
            Optional<Logging> logging = Optional.empty();
            Optional<Completions> completions = Optional.empty();

            for (ServerCapability cap : caps) {
                if (cap instanceof Prompts p) {
                    prompts = Optional.of(p);
                } else if (cap instanceof Resources r) {
                    resources = Optional.of(r);
                } else if (cap instanceof Tools t) {
                    tools = Optional.of(t);
                } else if (cap instanceof Logging l) {
                    logging = Optional.of(l);
                } else if (cap instanceof Completions c) {
                    completions = Optional.of(c);
                }
            }

            return new ServerCapabilities(prompts, resources, tools, logging, completions);
        }
    };
}
