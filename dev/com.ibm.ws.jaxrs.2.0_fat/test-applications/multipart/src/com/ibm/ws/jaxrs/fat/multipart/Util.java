/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.multipart;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;

public class Util {
    private static final String LS = System.lineSeparator();

    static InputStream xmlFile() {
        String xml =
                        "<root>" + LS +
                        "  <mid attr1=\"value1\" attr2=\"value2\">" + LS +
                        "    <inner attr3=\"value3\"/>" + LS +
                        "  </mid>" + LS +
                        "  <mid attr1=\"value4\" attr2=\"value5\">" + LS +
                        "    <inner attr3=\"value6\"/>" + LS +
                        "  </mid>" + LS +
                        "</root>";
        return new ByteArrayInputStream(xml.getBytes());
    }

    static InputStream asciidocFile() {
        String adoc =
                        "=== MicroProfile Rest Client 2.0" + LS +
                        "" + LS +
                        "MicroProfile REST Client is a type-safe client API enabling rapid development of " + LS +
                        "applications capable of consuming RESTful services. Version 2.0 is the latest update " + LS +
                        "and adds support for HTTP proxy servers, automatically following HTTP redirects, Server " + LS +
                        "Sent Events, and additional configuration options for JSON-B providers and multiple query " + LS +
                        "parameters." + LS +
                        "" + LS +
                        "To enable this feature, add `<feature>mpRestClient-2.0</feature>` to the list of features " + LS +
                        "in the `<featureManager>` element as shown in the example below:" + LS +
                        "" + LS +
                        "[source, xml]" + LS +
                        "----" + LS +
                        "    <featureManager>" + LS +
                        "        <feature>mpRestClient-2.0</feature>" + LS +
                        "    </featureManager>" + LS +
                        "----" + LS +
                        "" + LS +
                        "Alternatively, the `microprofile-4.0` convenience feature can be used instead, as shown " + LS +
                        "below:" + LS +
                        "" + LS +
                        "[source, xml]" + LS +
                        "----" + LS +
                        "    <featureManager>" + LS +
                        "        <feature>microprofile-4.0</feature>" + LS +
                        "    </featureManager>" + LS +
                        "----" + LS +
                        "" + LS +
                        "In order to start coding with the updated API, you will also need to pull in the MP Rest " + LS +
                        "Client 2.0 dependencies. If you use Maven, try these coordinates:" + LS +
                        "" + LS +
                        "[source,xml]" + LS +
                        "----" + LS +
                        "<dependency>" + LS +
                        "    <groupId>org.eclipse.microprofile.rest.client</groupId>" + LS +
                        "    <artifactId>microprofile-rest-client-api</artifactId>" + LS +
                        "    <version>2.0</version>" + LS +
                        "    <scope>provided</scope>" + LS +
                        "</dependency>" + LS +
                        "----" + LS +
                        "" + LS +
                        "or, if you use Gradle:" + LS +
                        "[source,gradle]" + LS +
                        "----" + LS +
                        "dependencies {" + LS +
                        "    mpRestClient group: 'org.eclipse.microprofile.rest.client', name: 'microprofile-rest-client-api', version: '2.0'" + LS +
                        "}" + LS +
                        "----" + LS +
                        "" + LS +
                        "This allows you to change how multi-valued query parameters are formatted, specify a proxy " + LS +
                        "server, configure the client to automatically follow redirects and more.  Here is an" + LS +
                        "example:" + LS +
                        "" + LS +
                        "[source,java]" + LS +
                        "----" + LS +
                        "MyClient client =" + LS +
                        "    RestClientBuilder.newBuilder()" + LS +
                        "                     .baseUri(someURI)" + LS +
                        "                     .queryParamStyle(QueryParamStyle.COMMA_SEPARATED) // or ARRAY_PAIRS or MULTI_PAIRS (default)..." + LS +
                        "                     .proxyAddress(\"myProxyServer\", 1080)" + LS +
                        "                     .followRedirects(true)" + LS +
                        "                     .build(MyMultiValuedQueryParamClient.class);" + LS +
                        "----" + LS +
                        "" + LS +
                        "These can also be configured via MP Config using the following properties, respectively:" + LS +
                        "* `com.mypkg.MyClient/mp-rest/queryParamStyle=COMMA_SEPARATED`" + LS +
                        "* `com.mypkg.MyClient/mp-rest/proxyAddress=myProxyServer:1080`" + LS +
                        "* `com.mypkg.MyClient/mp-rest/followRedirects=true`" + LS +
                        "" + LS +
                        "In a future blog post, we'll discuss how you can also use MP Rest Client 2.0 to consume " + LS +
                        "Server Sent Events." + LS +
                        "" + LS +
                        "For examples of how to use these new features, please refer to the " + LS +
                        "link:http://download.eclipse.org/microprofile/microprofile-rest-client-2.0-RC2/microprofile-rest-client-2.0-RC2.html[specification document]" + LS +
                        "or the link:https://github.com/eclipse/microprofile-rest-client[MicroProfile Rest Client project page]." + LS +
                        "";
        return new ByteArrayInputStream(adoc.getBytes());
    }

    static String toString(InputStream is) throws IOException {
        String str = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                                           .lines()
                                           .collect(Collectors.joining("\n"));
        System.out.println("Util.toString " + str);
        return str;
        /*
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        int bytesRead = 0;
        while (bytesRead > -1) {
            bytesRead = is.read(buf);
            sb.append(new String(buf, 0, bytesRead));
        }
        String str = sb.toString();
        System.out.println("Util.toString " + str);
        return str;
        */
    }

    static String getPartName(IAttachment part) {
        String contentDisposition = part.getHeader("Content-Disposition");
        int x = contentDisposition.indexOf("name=\"") + "name=\"".length();
        if (x < 0) {
            return null;
        }
        int y = contentDisposition.indexOf("\"", x+1);
        return contentDisposition.substring(x, y);
    }

    static String getFileName(IAttachment part) {
        String contentDisposition = part.getHeader("Content-Disposition");
        int x = contentDisposition.indexOf("filename=\"") + "filename=\"".length();
        if (x < 0) {
            return null;
        }
        int y = contentDisposition.indexOf("\"", x+1);
        return contentDisposition.substring(x, y);
    }

    static CheckableInputStream wrapStream(InputStream is) {
        return new CheckableInputStream(is);
    }

    static class CheckableInputStream extends InputStream {
        final InputStream is;
        boolean closed = false;

        CheckableInputStream(InputStream is) {
            this.is = is;
        }

        @Override
        public int read() throws IOException {
            if (closed) throw new IOException("stream has been closed");
            return is.read();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            is.close();
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
