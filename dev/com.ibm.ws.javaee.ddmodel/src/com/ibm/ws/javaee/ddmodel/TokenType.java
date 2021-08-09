/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class TokenType extends StringType {

    public static class ListType extends ParsableListImplements<TokenType, String> {
        @Override
        public TokenType newInstance(DDParser parser) {
            return new TokenType();
        }

        @Override
        public List<String> getList() {
            List<String> stringList = new ArrayList<String>();
            for (TokenType token : list) {
                stringList.add(token.getValue());
            }
            return stringList;
        }
    }

    public static TokenType wrap(DDParser parser, String wrapped) throws ParseException {
        return new TokenType(parser, wrapped);
    }

    public TokenType() {
        super(Whitespace.collapse);
    }

    protected TokenType(DDParser parser, String lexical) throws ParseException {
        super(Whitespace.collapse, parser, lexical);
    }

    public ListType split(DDParser parser, String expr) throws ParseException {
        ListType list = new ListType();
        String[] tokens = getValue().split(expr);
        for (String token : tokens) {
            list.add(parser.parseToken(token));
        }
        return list;
    }
}
