/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsonp.lib.provider;

import java.math.BigDecimal;

import javax.json.stream.JsonLocation;

/**
 *
 */
public class JsonParserImpl implements javax.json.stream.JsonParser {

    @Override
    public void close() {}

    @Override
    public BigDecimal getBigDecimal() {
        return null;
    }

    @Override
    public int getInt() {
        return 0;
    }

    @Override
    public JsonLocation getLocation() {
        return null;
    }

    @Override
    public long getLong() {
        return 0;
    }

    @Override
    public String getString() {
        return "Custom JSONP implementation loaded from a shared library";
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean isIntegralNumber() {
        return false;
    }

    @Override
    public Event next() {
        return null;
    }
}
