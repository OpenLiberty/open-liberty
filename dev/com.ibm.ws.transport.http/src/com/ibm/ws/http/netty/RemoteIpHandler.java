/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 *
 */
public class RemoteIpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * Identifies between the forwarded for and by lists
     */
    private enum ListType {
        FOR, BY
    };

    private static final String FOR = "for";
    private static final String BY = "by";
    private static final String PROTO = "proto";
    private static final String HOST = "host";

    static final String FORWARDED_HEADER = "Forwarded";
    static final String X_FORWARDED_FOR = "X-Forwarded-For";
    static final String X_FORWARDED_BY = "X-Forwarded-By";
    static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";
    static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

    Pattern proxies;
    Boolean useInAccessLog;

    String forwardedProto;
    String forwardedHost;
    String forwardedPort;

    List<String> forwardedFor = new ArrayList<String>();
    List<String> forwardedBy = new ArrayList<String>();

    boolean noErrors;

    public RemoteIpHandler(HttpChannelConfig httpConfig) {
        MSP.log("Forwarded handler created. Good config: " + Objects.nonNull(httpConfig));
        Objects.requireNonNull(httpConfig);
        proxies = httpConfig.getForwardedProxiesRegex();
        useInAccessLog = httpConfig.useForwardingHeadersInAccessLog();

        noErrors = Boolean.TRUE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {

//        String remote = context.channel().remoteAddress().toString();
//        Matcher matcher = proxies.matcher(remote);
//
//        if (matcher.matches()) {
//
//        }

        String forwardedHeader = request.headers().get(FORWARDED_HEADER);

        if (!Objects.isNull(forwardedHeader)) {

            this.parseForwarded(request);

        }

        else {
            this.parseXForwarded(request);
        }

        if (noErrors) {

            context.channel().attr(NettyHttpConstants.FORWARDED_HOST_KEY).set(forwardedHost);
            context.channel().attr(NettyHttpConstants.FORWARDED_PORT_KEY).set(forwardedPort);
            context.channel().attr(NettyHttpConstants.FORWARDED_PROTO_KEY).set(forwardedProto);

            context.channel().attr(NettyHttpConstants.FORWARDED_BY_KEY).set(forwardedBy.toArray(new String[forwardedBy.size()]));
            context.channel().attr(NettyHttpConstants.FORWARDED_FOR_KEY).set(forwardedFor.toArray(new String[forwardedFor.size()]));

            MSP.log("ForwardedHost: " + forwardedHost);
            MSP.log("Forwarded Port: " + forwardedPort);
            MSP.log("Forwarded Proto: " + forwardedProto);

        }

        context.fireChannelRead(request);

    }

    /**
     * Used when a misconfigured forwarded or x-forwarded-* header is used. Attributes
     * will not be set when the error flag is set.
     */
    private void setErrorState() {
        this.forwardedBy = null;
        this.forwardedFor = null;
        this.forwardedHost = null;
        this.forwardedPort = null;
        this.forwardedProto = null;
        this.noErrors = Boolean.FALSE;
    }

    private void parseForwarded(FullHttpRequest request) {

        List<String> values = request.headers().getAll(FORWARDED_HEADER);
        if (Objects.nonNull(values) && !values.isEmpty()) {
            values.forEach(this::processForwardedHeader);
        }
    }

    private void processForwardedHeader(String value) {
        //Each Forwarded header may consist of a combination of the four
        //spec defined parameters: by, for, host, proto. When more than
        //one parameter is present, the header value will use the semi-
        //colon character to delimit between them.
        String[] parameters = value.split(";");
        String[] nodes = null;
        String node = null;
        String nodeExtract = null;

        for (String param : parameters) {
            //The "for" and "by" parameters could be comma delimitted
            //lists. As such, lets split this again to save the data in the same
            //format as X-Forwarding
            nodes = param.split(",");

            for (String split : nodes) {
                //Note that HTTP list allows white spaces between the identifiers, as such,
                //trim the string before evaluating. Normalize it to lowercase as well
                node = split.toLowerCase().trim();

                try {
                    nodeExtract = node.substring(node.indexOf("=") + 1);
                } catch (IndexOutOfBoundsException e) {
                    setErrorState();
                    //TODO: debug error
                    return;
                }

                switch (node) {
                    case (FOR): {
                        processForwardedAddress(nodeExtract, ListType.FOR);
                        break;
                    }
                    case (BY): {
                        processForwardedAddress(nodeExtract, ListType.BY);
                        break;
                    }
                    case (PROTO): {
                        forwardedProto = this.isValidProto(nodeExtract) ? nodeExtract : null;
                        if (Objects.isNull(forwardedProto)) {
                            MSP.log("Forwarded header proto value was malformed: " + nodeExtract);
                            this.setErrorState();
                            //TODO exit log
                            return;
                        }
                        break;
                    }
                    case (HOST): {
                        forwardedHost = this.isValidHost(nodeExtract) ? forwardedHost : null;
                        if (Objects.isNull(forwardedHost)) {
                            MSP.log("Forwarded header host value was malformed: " + nodeExtract);
                            this.setErrorState();
                            //TODO exit log
                            return;
                        }
                        break;
                    }
                    default: {
                        MSP.log("Unrecognized parameter in Forwarded header: " + node);
                        setErrorState();
                    }
                }
                if (!noErrors) {
                    MSP.log("processForwardedHeader");
                    return;
                }
            }
        }

        MSP.log("processForwardedHeader");

    }

    private void processForwardedAddress(String address, ListType type) {
        List<String> list = null;

        if (type == ListType.BY) {
            list = this.forwardedBy;
        }

        if (type == ListType.FOR) {
            list = this.forwardedFor;
        }

        String extract = address.replaceAll("\"", "").trim();
        String nodeName = null;

        int openBracket = extract.indexOf("[");
        int closedBracket = extract.indexOf("]");

        if (openBracket > -1) {
            //This is an IPv6 address
            //The nodename is enclosed in "[ ]", get it now

            if (openBracket != 0 || !(closedBracket > -1)) {
                setErrorState();
                MSP.log("Forwarded header IPv6 was malformed");
            }
            return;
        }

        nodeName = extract.substring(openBracket + 1, closedBracket);

        if ((type == ListType.FOR) && list.isEmpty() && extract.contains("]:")) {
            try {
                this.forwardedPort = extract.substring(closedBracket + 2);
            } catch (IndexOutOfBoundsException e) {
                setErrorState();
                //TODO log error
                return;
            }
        }

        //Simply delimit by ":" on other node types to separate nodename and node-port
        else {
            if (extract.contains(":")) {
                int index = extract.indexOf(":");
                nodeName = extract.substring(0, index);
                if ((type == ListType.FOR) && list.isEmpty()) {
                    try {
                        this.forwardedPort = extract.substring(index + 1);
                    } catch (IndexOutOfBoundsException e) {
                        setErrorState();
                        //TODO: log error
                        return;
                    }
                }
            }
            // No port or "[ ]" character, the nodename is the entire provided extract
            else {

                nodeName = extract;
            }
        }
        MSP.log("Forwarded address [" + nodeName + "] being tracked in " + type.toString() + " list.");

        list.add(nodeName);
    }

    private void parseXForwarded(FullHttpRequest request) {

        List<String> value;
        HttpHeaders headers = request.headers();
        Objects.nonNull(headers);

        value = headers.getAll(X_FORWARDED_FOR);
        if (Objects.nonNull(value)) {
            value.forEach(this::processXForwardedFor);
        }

        value = headers.getAll(X_FORWARDED_BY);
        if (Objects.nonNull(value)) {
            value.forEach(this::processXForwardedBy);
        }

        value = headers.getAll(X_FORWARDED_PROTO_HEADER);
        if (Objects.nonNull(value) && this.isValidProto(value.get(value.size() - 1))) {
            // this.forwardedProto;
        }

        this.forwardedHost = NettyHeaderUtils.getLast(headers, X_FORWARDED_HOST_HEADER);
        this.forwardedPort = NettyHeaderUtils.getLast(headers, X_FORWARDED_PORT_HEADER);

    }

    /**
     * The forwarded FOR and BY lists are comma delimited. Split by the comma
     * character and add each value to the corresponding list.
     *
     * @param header
     * @param list
     */
    private void processXForwardedAddress(String header, ListType type) {

        String[] addresses = header.split(",");
        for (String address : addresses) {
            if (type == ListType.BY) {
                this.forwardedBy.add(address.trim());
            } else {
                this.forwardedFor.add(address.trim());
            }
        }
    }

    private void processXForwardedBy(String header) {
        processXForwardedAddress(header, ListType.BY);
    }

    private void processXForwardedFor(String header) {

        processXForwardedAddress(header, ListType.FOR);
    }

    /**
     * Ver
     *
     * @param forwardedHost
     * @return
     */
    private boolean isValidHost(String forwardedHost) {

        boolean valid = Boolean.TRUE;

        int openBracket = forwardedHost.indexOf("[");
        int closedBracket = forwardedHost.indexOf("]");

        if (openBracket > -1) {
            if (openBracket != 0 || !(closedBracket > -1)) {
                valid = Boolean.FALSE;
            }
        }

        return valid;
    }

    /*
     * A valid proto may start with an alpha followed by any number of chars that are
     * - alpha
     * - numeric
     * - "+" or "-" or "."
     */

    private boolean isValidProto(String forwardedProto) {

        //     Tr.entry(tc, "validateProto");

        char[] a = forwardedProto.toCharArray();
        boolean valid = true;
        char c = a[0];
        valid = ((c >= 'a') && (c <= 'z')) ||
                ((c >= 'A') && (c <= 'Z'));
        if (valid) {

            for (int i = 1; i < a.length; i++) {
                c = a[i];
                valid = ((c >= 'a') && (c <= 'z')) ||
                        ((c >= 'A') && (c <= 'Z')) ||
                        ((c >= '0') && (c <= '9')) ||
                        (c == '+') || (c == '-') || (c == '.');
                if (!valid) {
                    break;
                }
            }

        }

        //    Tr.debug(tc, "ValidateProto value is valid: " + valid);
        //    Tr.exit(tc, "validateProto");
        return valid;

    }

}