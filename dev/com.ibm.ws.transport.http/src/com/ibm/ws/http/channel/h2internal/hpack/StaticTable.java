/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.hpack;

import java.util.Arrays;
import java.util.List;

public final class StaticTable {

    public static final List<H2HeaderField> STATIC_TABLE = Arrays.asList(
                                                                         /* 1 */ new H2HeaderField(":authority", "", 1),
                                                                         /* 2 */ new H2HeaderField(":method", "GET", 2),
                                                                         /* 3 */ new H2HeaderField(":method", "POST", 3),
                                                                         /* 4 */ new H2HeaderField(":path", "/", 4),
                                                                         /* 5 */ new H2HeaderField(":path", "/index.html", 5),
                                                                         /* 6 */ new H2HeaderField(":scheme", "http", 6),
                                                                         /* 7 */ new H2HeaderField(":scheme", "https", 7),
                                                                         /* 8 */ new H2HeaderField(":status", "200", 8),
                                                                         /* 9 */ new H2HeaderField(":status", "204", 9),
                                                                         /* 10 */ new H2HeaderField(":status", "206", 10),
                                                                         /* 11 */ new H2HeaderField(":status", "304", 11),
                                                                         /* 12 */ new H2HeaderField(":status", "400", 12),
                                                                         /* 13 */ new H2HeaderField(":status", "404", 13),
                                                                         /* 14 */ new H2HeaderField(":status", "500", 14),
                                                                         /* 15 */ new H2HeaderField("accept-charset", "", 15),
                                                                         /* 16 */ new H2HeaderField("accept-encoding", "gzip, deflate", 16),
                                                                         /* 17 */ new H2HeaderField("accept-language", "", 17),
                                                                         /* 18 */ new H2HeaderField("accept-ranges", "", 18),
                                                                         /* 19 */ new H2HeaderField("accept", "", 19),
                                                                         /* 20 */ new H2HeaderField("access-control-allow-origin", "", 20),
                                                                         /* 21 */ new H2HeaderField("age", "", 21),
                                                                         /* 22 */ new H2HeaderField("allow", "", 22),
                                                                         /* 23 */ new H2HeaderField("authorization", "", 23),
                                                                         /* 24 */ new H2HeaderField("cache-control", "", 24),
                                                                         /* 25 */ new H2HeaderField("content-disposition", "", 25),
                                                                         /* 26 */ new H2HeaderField("content-encoding", "", 26),
                                                                         /* 27 */ new H2HeaderField("content-language", "", 27),
                                                                         /* 28 */ new H2HeaderField("content-length", "", 28),
                                                                         /* 29 */ new H2HeaderField("content-location", "", 29),
                                                                         /* 30 */ new H2HeaderField("content-range", "", 30),
                                                                         /* 31 */ new H2HeaderField("content-type", "", 31),
                                                                         /* 32 */ new H2HeaderField("cookie", "", 32),
                                                                         /* 33 */ new H2HeaderField("date", "", 33),
                                                                         /* 34 */ new H2HeaderField("etag", "", 34),
                                                                         /* 35 */ new H2HeaderField("expect", "", 35),
                                                                         /* 36 */ new H2HeaderField("expires", "", 36),
                                                                         /* 37 */ new H2HeaderField("from", "", 37),
                                                                         /* 38 */ new H2HeaderField("host", "", 38),
                                                                         /* 39 */ new H2HeaderField("if-match", "", 39),
                                                                         /* 40 */ new H2HeaderField("if-modified-since", "", 40),
                                                                         /* 41 */ new H2HeaderField("if-none-match", "", 41),
                                                                         /* 42 */ new H2HeaderField("if-range", "", 42),
                                                                         /* 43 */ new H2HeaderField("if-unmodified-since", "", 43),
                                                                         /* 44 */ new H2HeaderField("last-modified", "", 44),
                                                                         /* 45 */ new H2HeaderField("link", "", 45),
                                                                         /* 46 */ new H2HeaderField("location", "", 46),
                                                                         /* 47 */ new H2HeaderField("max-forwards", "", 47),
                                                                         /* 48 */ new H2HeaderField("proxy-authentication", "", 48),
                                                                         /* 49 */ new H2HeaderField("proxy-authorization", "", 49),
                                                                         /* 50 */ new H2HeaderField("range", "", 50),
                                                                         /* 51 */ new H2HeaderField("referer", "", 51),
                                                                         /* 52 */ new H2HeaderField("refresh", "", 52),
                                                                         /* 53 */ new H2HeaderField("retry-after", "", 53),
                                                                         /* 54 */ new H2HeaderField("server", "", 54),
                                                                         /* 55 */ new H2HeaderField("set-cookie", "", 55),
                                                                         /* 56 */ new H2HeaderField("strict-transport-authority", "", 56),
                                                                         /* 57 */ new H2HeaderField("transfer-encoding", "", 57),
                                                                         /* 58 */ new H2HeaderField("user-agent", "", 58),
                                                                         /* 59 */ new H2HeaderField("vary", "", 59),
                                                                         /* 60 */ new H2HeaderField("via", "", 60),
                                                                         /* 61 */ new H2HeaderField("www-authenticate", "", 61));

}
