/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import test.util.CartesianProduct;
import test.util.LogWatcher;

import java.util.List;
import java.util.logging.Level;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class  ConfigImplTest {
//    @Parameters(name="{0},{1}")
    @Parameters()
    public static List<Object[]>  parameters(){
        return CartesianProduct.of(ValidationMode.class).with(PermissionMode.class);
    }

    @Rule
    public LogWatcher flightRecorder = new LogWatcher();

    final ValidationMode defaultMode, mode1, mode2;
    final PermissionMode permissionMode;

    ConfigImpl cfg;

    static <E extends Enum<E>> E getNext(E current) {
        final Class<? extends Enum> currentClass = current.getClass();
        Class<E> enumClass = (Class<E>)(currentClass.isEnum() ? currentClass : currentClass.getSuperclass());
        E[] values = enumClass.getEnumConstants();
        return values[(current.ordinal() + 1)%values.length];
    }

    public ConfigImplTest(ValidationMode defaultMode, PermissionMode permissionMode) {
        System.out.println("Validation : " + defaultMode + ", Permission : " + permissionMode);
        this.defaultMode = defaultMode;
        this.mode1 = getNext(defaultMode);
        this.mode2 = getNext(mode1);
        this.permissionMode = permissionMode;
    }

    @Before
    public void setup() {
        cfg = new ConfigImpl(new ConfigImpl.Initializer(){
            void init(ConfigImpl cfg) {
                cfg.setValidationMode(defaultMode, "*");
                cfg.setPermission(permissionMode, "*");
            }
        });
    }

    @Test
    public void testDefaultMode() {
        assertEquals(defaultMode, cfg.getDefaultMode());
        cfg.setValidationMode(getNext(defaultMode), "a.b.c.d");
        assertEquals(defaultMode, cfg.getDefaultMode());
    }

    @Test
    public void testNullReturnedForUnknownClass() {
        assertNull(cfg.getValidationMode("a.b.c"));
        cfg.setValidationMode(getNext(defaultMode), "a.b.c.d");
        assertNull(cfg.getValidationMode("a.b.c"));
    }

    @Test
    public void testExactClassMatch() {
        cfg.setValidationMode(mode1, "a.b.c");
        assertEquals(mode1, cfg.getValidationMode("a.b.c"));
    }

    @Test
    public void testExactMethodMatch() {
        cfg.setValidationMode(mode1, "a.b.c#d");
        assertEquals(mode1, cfg.getValidationMode("a.b.c#d"));
    }

    @Test
    public void testPrefixClassMatch() {
        cfg.setValidationMode(mode1, "a.b.*");
        assertEquals(mode1, cfg.getValidationMode("a.b.c"));
        assertEquals(mode1, cfg.getValidationMode("a.b.c#d"));
    }

    @Test
    public void testPrefixMethodMatch() {
        cfg.setValidationMode(mode1, "a.b.c#*");
        assertEquals(mode1, cfg.getValidationMode("a.b.c#d"));
    }

    @Test
    public void testEmptyPrefixMethodDoesNotMatchClass() {
        cfg.setValidationMode(mode1, "a.b.c#*");
        assertEquals(null, cfg.getValidationMode("a.b.c"));
        cfg.setValidationMode(mode2, "a.b.c");
        assertEquals(mode2, cfg.getValidationMode("a.b.c"));
    }

    @Test
    public void testPrefixSpecialMethodMatch() {
        cfg.setValidationMode(mode1, "a.b.c#*");
        assertEquals(mode1, cfg.getValidationMode("a.b.c#<init>"));
        assertEquals(mode1, cfg.getValidationMode("a.b.c#<clinit>"));
    }

    @Test
    public void testIllegalSpecialMethod() {
        cfg.setValidationMode(mode1, "a.b.c#<INIT>");
        flightRecorder.expectLog(Level.SEVERE, "a.b.c#<INIT>");
        cfg.setValidationMode(mode1, "a.b.c#<i*");
        flightRecorder.expectLog(Level.SEVERE, "a.b.c#<i*");
    }
}
