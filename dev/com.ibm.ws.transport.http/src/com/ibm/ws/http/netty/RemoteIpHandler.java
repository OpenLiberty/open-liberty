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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.BNFHeadersImpl.ListType;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

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

    Pattern proxies;
    Boolean useInAccessLog;
    Matcher matcher;

    String forwardedProto;
    String forwardedHost;
    String forwardedPort;

    List<String> forwardedFor = new ArrayList<String>();
    List<String> forwardedBy = new ArrayList<String>();

    public RemoteIpHandler(HttpChannelConfig httpConfig) {
        Objects.requireNonNull(httpConfig);
        proxies = httpConfig.getForwardedProxiesRegex();
        useInAccessLog = httpConfig.useForwardingHeadersInAccessLog();

    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {

        //Favor the spec defined header
        if (request.headers().contains("forwarded")) {
            this.parseForwarded(request);
        } else {
            this.parseXForwarded(request);
        }

        //tc, Verifying connected endpoint matches proxy regex

        String remoteIp = context.channel().remoteAddress().toString();

        matcher = proxies.matcher(remoteIp);
        if (matcher.matches()) {
            //tc connectected endpoint matched, verifying forwarded FOR list addresses

            List<String> forwardedForValues = Collections.EMPTY_LIST;

            if (Objects.isNull(forwardedForValues) || forwardedForValues.isEmpty()) {

                return;
            }

            for (String proxy : forwardedForValues) {
                matcher = proxies.matcher(proxy);
                if (!matcher.matches()) {
                    //debug Found address not defined in proxy regex, forwarded values will not be used
                    return;
                }
            }

            //First check that the last node identifier is not an obfuscated address or
            //unknown token
            if (Objects.isNull(forwardedForValues) || "unknown".equalsIgnoreCase(forwardedForValues.get(0))
                || forwardedForValues.get(0).startsWith("_")) {

                //Client address is unknown or obfuscated, forwarded values will not be used
                //exit
                return;
            }

            //Set attributes values

//              this.forwardedRemotePort = Integer.parseInt(getMessageBeingParsed().getForwardedPort());
//          } catch (NumberFormatException e) {
            //
//              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                  Tr.debug(tc, "Remote port provided was either obfuscated or malformed, forwarded values will not be used.");
//                  Tr.exit(tc, "initForwardedValues");
//              }
//              return;
//          }
            //
//      }
            //
//      this.forwardedRemoteAddress = forwardedForList[0];
//      this.forwardedHost = this.getMessageBeingParsed().getForwardedHost();
//      this.forwardedProto = this.getMessageBeingParsed().getForwardedProto();

        }

    }

    private void parseForwarded(FullHttpRequest request) {

        //Each Forwarded header may consist of a combination of the four
        //spec defined parameters: by, for, host, proto. When more than
        //one parameter is present, the header value will use the semi-
        //colon character to delimit between them.
        String[] parameters = null;
        String[] nodes = null;
        String node = null;
        String nodeExtract = null;

        for (String value : request.headers().getAll("forwarded")) {
            parameters = value.split(";");

            for (String param : parameters) {
                //The "for" and "by" parameters could be comma delimited
                //lists. As such, lets split this again to save the
                //data in the same format as X-Forwarding
                nodes = param.split(",");

                //Note that HTTP list allows white spaces between the identifiers, as such,
                //trim the string before evaluating.
                node = value.trim();
                try {
                    nodeExtract = node.substring(node.indexOf("=") + 1);
                } catch (IndexOutOfBoundsException e) {
                    processForwardedErrorState();
                    // if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    //     Tr.debug(tc, "Forwarded header node value was malformed.");
                    //     Tr.exit(tc, "processSpecForwardedHeader");
                    // }
                    return;
                }
                if (node.toLowerCase().startsWith(FOR)) {

                    processForwardedAddressExtract(nodeExtract, ListType.FOR);

                } else if (node.toLowerCase().startsWith(BY)) {

                    processForwardedAddressExtract(nodeExtract, ListType.BY);

                } else if (node.toLowerCase().startsWith(PROTO)) {
                    forwardedProto = nodeExtract;
                    boolean validProto = validateProto(forwardedProto);
                    if (!validProto) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header proto value was malformed: " + forwardedProto);
                        }
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "processSpecForwardedHeader");
                        }
                        return;
                    }
                } else if (node.toLowerCase().startsWith(HOST)) {
                    forwardedHost = nodeExtract;
                    forwardedHost = validateForwardedHost(forwardedHost);
                    if (forwardedHost == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header host value was malformed: " + nodeExtract);
                        }
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "processSpecForwardedHeader");
                        }
                        return;
                    }
                }
                //Unrecognized parameter
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Unrecognized parameter if Forwarded header: " + node);
                    }
                    processForwardedErrorState();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }

                //Check that processing of this node has not entered error state
                if (forwardHeaderErrorState) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }
            }
        }

    }
    // Tr.entry(tc, "processSpecForwardedHeader");

    //Each Forwarded header may consist of a combination of the four
    //spec defined parameters: by, for, host, proto. When more than
    //one parameter is present, the header value will use the semi-
    //colon character to delimit between them.
    String[] parameters = header.getDebugValue().split(";");
    String[] nodes = null;
    String node = null;
    String nodeExtract = null;

    for(
    String param:parameters)
    {

        //The "for" and "by" parameters could be comma delimited
        //lists. As such, lets split this again to save the
        //data in the same format as X-Forwarding
        nodes = param.split(",");

        for (String value : nodes) {

            //Note that HTTP list allows white spaces between the identifiers, as such,
            //trim the string before evaluating.
            node = value.trim();
            try {
                nodeExtract = node.substring(node.indexOf("=") + 1);
            } catch (IndexOutOfBoundsException e) {
                processForwardedErrorState();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, "Forwarded header node value was malformed.");
                    Tr.exit(tc, "processSpecForwardedHeader");
                }
                return;
            }

            if (node.toLowerCase().startsWith(FOR)) {

                processForwardedAddressExtract(nodeExtract, ListType.FOR);

            } else if (node.toLowerCase().startsWith(BY)) {

                processForwardedAddressExtract(nodeExtract, ListType.BY);

            } else if (node.toLowerCase().startsWith(PROTO)) {
                forwardedProto = nodeExtract;
                boolean validProto = validateProto(forwardedProto);
                if (!validProto) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Forwarded header proto value was malformed: " + forwardedProto);
                    }
                    processForwardedErrorState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }
            } else if (node.toLowerCase().startsWith(HOST)) {
                forwardedHost = nodeExtract;
                forwardedHost = validateForwardedHost(forwardedHost);
                if (forwardedHost == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Forwarded header host value was malformed: " + nodeExtract);
                    }
                    processForwardedErrorState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "processSpecForwardedHeader");
                    }
                    return;
                }
            }
            //Unrecognized parameter
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, "Unrecognized parameter if Forwarded header: " + node);
                }
                processForwardedErrorState();

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "processSpecForwardedHeader");
                }
                return;
            }

            //Check that processing of this node has not entered error state
            if (forwardHeaderErrorState) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "processSpecForwardedHeader");
                }
                return;
            }
        }

    }if(TraceComponent.isAnyTracingEnabled()&&tc.isEntryEnabled())
    {
        Tr.exit(tc, "processSpecForwardedHeader");
    }

    }

    private void parseXForwarded(FullHttpRequest request) {

        for(request.headers().getAll("))

            processSpecForwardedHeader(header);

        }

    else

    {
        //We received an X-Forwarded-* header, while having already processed
        //a Forwarded header. In this case, just exit this processing and
        //do not change the state of the internal instances. The X-Forwarding
        //information is cleared out on the first Forwarded processed header,
        //so it should be clear at this point.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "X-Forwarded header received after Forwarded header tracking has been enabled, "
                         + "X-Forwarded header will not be processed");
        }
    }if(TraceComponent.isAnyTracingEnabled()&&tc.isEntryEnabled())
    {
        Tr.exit(tc, "processForwardedHeader");
    }
    }

    /**
     * Returns a String representation of the provided address node delimiter
     * which is used to update the Forwarded for/by builders. This
     * method will remove any provided port in the process, with the exception
     * of the very first element added to the list. The first element would
     * correspond to the client address, and as such, the port is saved off
     * to be referenced as the remote port.
     *
     * @param nodeExtract
     * @return
     */
    private void processForwardedAddressExtract(String nodeExtract, ListType type) {

        List<String> list = null;
        if (type == ListType.BY) {
            list = this.forwardedBy;
        }
        if (type == ListType.FOR) {
            list = this.forwardedFor;
        }

        //The node identifier is defined by the ABNF syntax as
        //        node     = nodename [ ":" node-port ]
        //                   nodename = IPv4address / "[" IPv6address "]" /
        //                             "unknown" / obfnode
        //As such, to make it equivalent to the de-facto headers, remove the quotations
        //and possible port
        String extract = nodeExtract.replaceAll("\"", "").trim();
        String nodeName = null;

        //obfnodes are only allowed to contain ALPHA / DIGIT / "." / "_" / "-"
        //so if the token contains "[", it is an IPv6 address
        int openBracket = extract.indexOf("[");
        int closedBracket = extract.indexOf("]");

        if (openBracket > -1) {
            //This is an IPv6address
            //The nodename is enclosed in "[ ]", get it now

            //If the first character isn't the open bracket or if a close bracket
            //is not provided, this is a badly formed header
            if (openBracket != 0 || !(closedBracket > -1)) {
                processForwardedErrorState();
                //badly formated header
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.debug(tc, "Forwarded header IPv6 was malformed.");
                    Tr.exit(tc, "processForwardedHeader");
                }
                return;
            }

            nodeName = extract.substring(openBracket + 1, closedBracket);

            //If this extract contains a port, there will be a ":" after
            //the closing bracket. Only get it if this is the first address
            //being added to the "for" list
            if ((type == ListType.FOR) && list.isEmpty() && extract.contains("]:")) {
                try {
                    this.forwardedPort = extract.substring(closedBracket + 2);
                } catch (IndexOutOfBoundsException e) {
                    processForwardedErrorState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.debug(tc, "Forwarded header IPv6 port was malformed.");
                        Tr.exit(tc, "processForwardedHeader");
                    }
                    return;
                }
            }

        }

        //Simply delimit by ":" on other node types to separate nodename and node-port
        else {

            if (extract.contains(":")) {
                int index = extract.indexOf(":");
                nodeName = extract.substring(0, index);
                //Record the port if this is the first address being added to the
                //"for" list, corresponding to the client
                if ((type == ListType.FOR) && list.isEmpty()) {
                    try {
                        this.forwardedPort = extract.substring(index + 1);
                    } catch (IndexOutOfBoundsException e) {
                        processForwardedErrorState();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.debug(tc, "Forwarded header node-port was malformed.");
                            Tr.exit(tc, "processForwardedHeader");
                        }
                        return;
                    }

                }
            }
            //No port or "[ ]" characters, the nodename is the entire provided extract
            else {
                nodeName = extract;
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "Forwarded address [" + nodeName + "] being tracked in " + type.toString() + " list.");
            Tr.exit(tc, "processForwardedHeader");
        }
        list.add(nodeName);
    }

    /*
     * A valid proto may start with an alpha followed by any number of chars that are
     * - alpha
     * - numeric
     * - "+" or "-" or "."
     */

    private boolean validateProto(String forwardedProto) {

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

    /*
     * Valid hostname can be a bracketed ipv6 address, or
     * any other string.
     * Validate the ipv6 address has opening and closing brackets.
     */

   private String validateForwardedHost(String forwardedHost) {
       int openBracket = forwardedHost.indexOf("[");
       int closedBracket = forwardedHost.indexOf("]");
       String nodename = forwardedHost;

       if (openBracket > -1) {
           //This is an IPv6address
           //The nodename is enclosed in "[ ]", get it now

           //If the first character isn't the open bracket or if close bracket
           //is missing, this is a badly formed header
           if (openBracket != 0 || !(closedBracket > -1)) {
               nodename = null;
           }
       }
       return nodename;
   }

}
