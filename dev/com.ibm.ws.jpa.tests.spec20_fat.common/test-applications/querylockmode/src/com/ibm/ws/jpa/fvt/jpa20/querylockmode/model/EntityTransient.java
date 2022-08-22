/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import java.math.BigDecimal;
import java.util.Collection;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import com.ibm.websphere.simplicity.log.Log;

/**
 * <p>Entity of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
 * <a href="http://www.j2ee.me/javaee/6/docs/api/javax/persistence/package-summary.html">javax.persistence documentation</a>)
 *
 *
 * <p>These annotations are exercised:
 * <ul>
 * <li><b>@Transient</b> (63 of 65):
 * <p>This annotation specifies that the property or field is not persistent.
 *
 * <p>@Transient combinations:
 * <ul>
 * <li>@Transient on field
 * <li>@Transient on property
 * <li>transient (modifier) on field
 * </ul>
 *
 * <p>@Transient combinations exercised in this entity:
 * <ul>
 * <li>@Transient on field
 * <li>@Transient on property
 * <li>transient (modifier) on field
 * </ul>
 * </ul>
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>Per the JSR-317 spec (page 26), mapping annotations must not be applied to fields or properties that are transient
 * </ol>
 */
@Access(AccessType.FIELD)
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class EntityTransient {

    @Transient
    private int entityTransient_id;

    @Transient
    private long entityTransient_version;

    @Transient
    private static final long serialVersionUID = 1L;

    @Transient
    private static final java.util.Date CURRENT_DATE = new java.util.Date();

    @Transient
    private static final java.util.Date PAST_DATE = new java.util.Date(CURRENT_DATE.getTime() - (8 * 3600 * 1000));

    @Transient
    private static final java.util.Date FUTURE_DATE = new java.util.Date(CURRENT_DATE.getTime() + (8 * 3600 * 1000));

    @Transient
    private static final BigDecimal negative = new BigDecimal(-99.99);

    @Transient
    private static final BigDecimal positive = new BigDecimal(99.99);

    //----------------------------------------------------------------------------------------------
    // @Transient combinations
    //----------------------------------------------------------------------------------------------
    @Transient
    private Log entityTransient_transient05;
    transient private Log entityTransient_transient06;

    @Transient
    private Log[] entityTransient_transient07;
    transient private Log[] entityTransient_transient08;

    @Transient
    private Collection entityTransient_transient09;
    transient private Collection entityTransient_transient10;

    @Transient
    private Collection[] entityTransient_transient11;
    transient private Collection[] entityTransient_transient12;

    @Transient
    private boolean entityTransient_transient13;
    transient private boolean entityTransient_transient14;

    @Transient
    private boolean[] entityTransient_transient15;
    transient private boolean[] entityTransient_transient16;

    @Transient
    private long entityTransient_transient17;
    transient private long entityTransient_transient18;

    @Transient
    private long[] entityTransient_transient19;
    transient private long[] entityTransient_transient20;

    @Transient
    private Long entityTransient_transient21;
    transient private Long entityTransient_transient22;

    @Transient
    private Long[] entityTransient_transient23;
    transient private Long[] entityTransient_transient24;

    @Transient
    private Collection<Log> entityTransient_transient25;
    transient private Collection<Log> entityTransient_transient26;

    @Transient
    private Collection<Log>[] entityTransient_transient27;
    transient private Collection<Log>[] entityTransient_transient28;

    @Transient
    private int entityTransient_transient29;
    transient private int entityTransient_transient30;

    @Transient
    private int[] entityTransient_transient31;
    transient private int[] entityTransient_transient32;

    @Transient
    private Integer entityTransient_transient33;
    transient private Integer entityTransient_transient34;

    @Transient
    private Integer[] entityTransient_transient35;
    transient private Integer[] entityTransient_transient36;

    @Transient
    private short entityTransient_transient37;
    transient private short entityTransient_transient38;

    @Transient
    private short[] entityTransient_transient39;
    transient private short[] entityTransient_transient40;

    @Transient
    private Short entityTransient_transient41;
    transient private Short entityTransient_transient42;

    @Transient
    private Short[] entityTransient_transient43;
    transient private Short[] entityTransient_transient44;

    @Transient
    private Object entityTransient_transient45;
    transient private Object entityTransient_transient46;

    @Transient
    private Object[] entityTransient_transient47;
    transient private Object[] entityTransient_transient48;

    @Transient
    private Boolean entityTransient_transient49;
    transient private Boolean entityTransient_transient50;

    @Transient
    private Boolean[] entityTransient_transient51;
    transient private Boolean[] entityTransient_transient52;

    @Transient
    private String entityTransient_transient53;
    transient private String entityTransient_transient54;

    @Transient
    private String[] entityTransient_transient55;
    transient private String[] entityTransient_transient56;

    @Transient
    private BigDecimal entityTransient_transient57;
    transient private BigDecimal entityTransient_transient58;

    @Transient
    private BigDecimal[] entityTransient_transient59;
    transient private BigDecimal[] entityTransient_transient60;

    @Transient
    private java.util.Date entityTransient_transient61;
    transient private java.util.Date entityTransient_transient62;

    @Transient
    private java.util.Date[] entityTransient_transient63;
    transient private java.util.Date[] entityTransient_transient64;

    @Transient
    private java.sql.Date entityTransient_transient65;
    transient private java.sql.Date entityTransient_transient66;

    @Transient
    private java.sql.Date[] entityTransient_transient67;
    transient private java.sql.Date[] entityTransient_transient68;

    @Transient
    private float entityTransient_transient69;
    transient private float entityTransient_transient70;

    @Transient
    private float[] entityTransient_transient71;
    transient private float[] entityTransient_transient72;

    @Transient
    private Float entityTransient_transient73;
    transient private Float entityTransient_transient74;

    @Transient
    private Float[] entityTransient_transient75;
    transient private Float[] entityTransient_transient76;

    @Transient
    private char entityTransient_transient77;
    transient private char entityTransient_transient78;

    @Transient
    private char[] entityTransient_transient79;
    transient private char[] entityTransient_transient80;

    @Transient
    private Character entityTransient_transient81;
    transient private Character entityTransient_transient82;

    @Transient
    private Character[] entityTransient_transient83;
    transient private Character[] entityTransient_transient84;

    public EntityTransient() {}

    public EntityTransient(int id,
                           long version,
                           Log transient05,
                           Log transient06,
                           Log[] transient07,
                           Log[] transient08,
                           Collection transient09,
                           Collection transient10,
                           Collection[] transient11,
                           Collection[] transient12,
                           boolean transient13,
                           boolean transient14,
                           boolean[] transient15,
                           boolean[] transient16,
                           long transient17,
                           long transient18,
                           long[] transient19,
                           long[] transient20,
                           Long transient21,
                           Long transient22,
                           Long[] transient23,
                           Long[] transient24,
                           Collection<Log> transient25,
                           Collection<Log> transient26,
                           Collection<Log>[] transient27,
                           Collection<Log>[] transient28,
                           int transient29,
                           int transient30,
                           int[] transient31,
                           int[] transient32,
                           Integer transient33,
                           Integer transient34,
                           Integer[] transient35,
                           Integer[] transient36,
                           short transient37,
                           short transient38,
                           short[] transient39,
                           short[] transient40,
                           Short transient41,
                           Short transient42,
                           Short[] transient43,
                           Short[] transient44,
                           Object transient45,
                           Object transient46,
                           Object[] transient47,
                           Object[] transient48,
                           Boolean transient49,
                           Boolean transient50,
                           Boolean[] transient51,
                           Boolean[] transient52,
                           String transient53,
                           String transient54,
                           String[] transient55,
                           String[] transient56,
                           BigDecimal transient57,
                           BigDecimal transient58,
                           BigDecimal[] transient59,
                           BigDecimal[] transient60,
                           java.util.Date transient61,
                           java.util.Date transient62,
                           java.util.Date[] transient63,
                           java.util.Date[] transient64,
                           java.sql.Date transient65,
                           java.sql.Date transient66,
                           java.sql.Date[] transient67,
                           java.sql.Date[] transient68,
                           float transient69,
                           float transient70,
                           float[] transient71,
                           float[] transient72,
                           Float transient73,
                           Float transient74,
                           Float[] transient75,
                           Float[] transient76,
                           char transient77,
                           char transient78,
                           char[] transient79,
                           char[] transient80,
                           Character transient81,
                           Character transient82,
                           Character[] transient83,
                           Character[] transient84) {
        this.entityTransient_id = id;
        this.entityTransient_version = version;
        this.entityTransient_transient05 = transient05;
        this.entityTransient_transient06 = transient06;
        this.entityTransient_transient07 = transient07;
        this.entityTransient_transient08 = transient08;
        this.entityTransient_transient09 = transient09;
        this.entityTransient_transient10 = transient10;
        this.entityTransient_transient11 = transient11;
        this.entityTransient_transient12 = transient12;
        this.entityTransient_transient13 = transient13;
        this.entityTransient_transient14 = transient14;
        this.entityTransient_transient15 = transient15;
        this.entityTransient_transient16 = transient16;
        this.entityTransient_transient17 = transient17;
        this.entityTransient_transient18 = transient18;
        this.entityTransient_transient19 = transient19;
        this.entityTransient_transient20 = transient20;
        this.entityTransient_transient21 = transient21;
        this.entityTransient_transient22 = transient22;
        this.entityTransient_transient23 = transient23;
        this.entityTransient_transient24 = transient24;
        this.entityTransient_transient25 = transient25;
        this.entityTransient_transient26 = transient26;
        this.entityTransient_transient27 = transient27;
        this.entityTransient_transient28 = transient28;
        this.entityTransient_transient29 = transient29;
        this.entityTransient_transient30 = transient30;
        this.entityTransient_transient31 = transient31;
        this.entityTransient_transient32 = transient32;
        this.entityTransient_transient33 = transient33;
        this.entityTransient_transient34 = transient34;
        this.entityTransient_transient35 = transient35;
        this.entityTransient_transient36 = transient36;
        this.entityTransient_transient37 = transient37;
        this.entityTransient_transient38 = transient38;
        this.entityTransient_transient39 = transient39;
        this.entityTransient_transient40 = transient40;
        this.entityTransient_transient41 = transient41;
        this.entityTransient_transient42 = transient42;
        this.entityTransient_transient43 = transient43;
        this.entityTransient_transient44 = transient44;
        this.entityTransient_transient45 = transient45;
        this.entityTransient_transient46 = transient46;
        this.entityTransient_transient47 = transient47;
        this.entityTransient_transient48 = transient48;
        this.entityTransient_transient49 = transient49;
        this.entityTransient_transient50 = transient50;
        this.entityTransient_transient51 = transient51;
        this.entityTransient_transient52 = transient52;
        this.entityTransient_transient53 = transient53;
        this.entityTransient_transient54 = transient54;
        this.entityTransient_transient55 = transient55;
        this.entityTransient_transient56 = transient56;
        this.entityTransient_transient57 = transient57;
        this.entityTransient_transient58 = transient58;
        this.entityTransient_transient59 = transient59;
        this.entityTransient_transient60 = transient60;
        this.entityTransient_transient61 = transient61;
        this.entityTransient_transient62 = transient62;
        this.entityTransient_transient63 = transient63;
        this.entityTransient_transient64 = transient64;
        this.entityTransient_transient65 = transient65;
        this.entityTransient_transient66 = transient66;
        this.entityTransient_transient67 = transient67;
        this.entityTransient_transient68 = transient68;
        this.entityTransient_transient69 = transient69;
        this.entityTransient_transient70 = transient70;
        this.entityTransient_transient71 = transient71;
        this.entityTransient_transient72 = transient72;
        this.entityTransient_transient73 = transient73;
        this.entityTransient_transient74 = transient74;
        this.entityTransient_transient75 = transient75;
        this.entityTransient_transient76 = transient76;
        this.entityTransient_transient77 = transient77;
        this.entityTransient_transient78 = transient78;
        this.entityTransient_transient79 = transient79;
        this.entityTransient_transient80 = transient80;
        this.entityTransient_transient81 = transient81;
        this.entityTransient_transient82 = transient82;
        this.entityTransient_transient83 = transient83;
        this.entityTransient_transient84 = transient84;
    }

    @Override
    public String toString() {
        return (" EntityTransient: " +
                " entityTransient_id: " + getEntityTransient_id() +
                " entityTransient_version: " + getEntityTransient_version() +
                " entityTransient_transient05: " + getEntityTransient_transient05() +
                " entityTransient_transient06: " + getEntityTransient_transient06() +
                " entityTransient_transient07: " + getEntityTransient_transient07() +
                " entityTransient_transient08: " + getEntityTransient_transient08() +
                " entityTransient_transient09: " + getEntityTransient_transient09() +
                " entityTransient_transient10: " + getEntityTransient_transient10() +
                " entityTransient_transient11: " + getEntityTransient_transient11() +
                " entityTransient_transient12: " + getEntityTransient_transient12() +
                " entityTransient_transient13: " + getEntityTransient_transient13() +
                " entityTransient_transient14: " + getEntityTransient_transient14() +
                " entityTransient_transient15: " + getEntityTransient_transient15() +
                " entityTransient_transient16: " + getEntityTransient_transient16() +
                " entityTransient_transient17: " + getEntityTransient_transient17() +
                " entityTransient_transient18: " + getEntityTransient_transient18() +
                " entityTransient_transient19: " + getEntityTransient_transient19() +
                " entityTransient_transient20: " + getEntityTransient_transient20() +
                " entityTransient_transient21: " + getEntityTransient_transient21() +
                " entityTransient_transient22: " + getEntityTransient_transient22() +
                " entityTransient_transient23: " + getEntityTransient_transient23() +
                " entityTransient_transient24: " + getEntityTransient_transient24() +
                " entityTransient_transient25: " + getEntityTransient_transient25() +
                " entityTransient_transient26: " + getEntityTransient_transient26() +
                " entityTransient_transient27: " + getEntityTransient_transient27() +
                " entityTransient_transient28: " + getEntityTransient_transient28() +
                " entityTransient_transient29: " + getEntityTransient_transient29() +
                " entityTransient_transient30: " + getEntityTransient_transient30() +
                " entityTransient_transient31: " + getEntityTransient_transient31() +
                " entityTransient_transient32: " + getEntityTransient_transient32() +
                " entityTransient_transient33: " + getEntityTransient_transient33() +
                " entityTransient_transient34: " + getEntityTransient_transient34() +
                " entityTransient_transient35: " + getEntityTransient_transient35() +
                " entityTransient_transient36: " + getEntityTransient_transient36() +
                " entityTransient_transient37: " + getEntityTransient_transient37() +
                " entityTransient_transient38: " + getEntityTransient_transient38() +
                " entityTransient_transient39: " + getEntityTransient_transient39() +
                " entityTransient_transient40: " + getEntityTransient_transient40() +
                " entityTransient_transient41: " + getEntityTransient_transient41() +
                " entityTransient_transient42: " + getEntityTransient_transient42() +
                " entityTransient_transient43: " + getEntityTransient_transient43() +
                " entityTransient_transient44: " + getEntityTransient_transient44() +
                " entityTransient_transient45: " + getEntityTransient_transient45() +
                " entityTransient_transient46: " + getEntityTransient_transient46() +
                " entityTransient_transient47: " + getEntityTransient_transient47() +
                " entityTransient_transient48: " + getEntityTransient_transient48() +
                " entityTransient_transient49: " + getEntityTransient_transient49() +
                " entityTransient_transient50: " + getEntityTransient_transient50() +
                " entityTransient_transient51: " + getEntityTransient_transient51() +
                " entityTransient_transient52: " + getEntityTransient_transient52() +
                " entityTransient_transient53: " + getEntityTransient_transient53() +
                " entityTransient_transient54: " + getEntityTransient_transient54() +
                " entityTransient_transient55: " + getEntityTransient_transient55() +
                " entityTransient_transient56: " + getEntityTransient_transient56() +
                " entityTransient_transient57: " + getEntityTransient_transient57() +
                " entityTransient_transient58: " + getEntityTransient_transient58() +
                " entityTransient_transient59: " + getEntityTransient_transient59() +
                " entityTransient_transient60: " + getEntityTransient_transient60() +
                " entityTransient_transient61: " + getEntityTransient_transient61() +
                " entityTransient_transient62: " + getEntityTransient_transient62() +
                " entityTransient_transient63: " + getEntityTransient_transient63() +
                " entityTransient_transient64: " + getEntityTransient_transient64() +
                " entityTransient_transient65: " + getEntityTransient_transient65() +
                " entityTransient_transient66: " + getEntityTransient_transient66() +
                " entityTransient_transient67: " + getEntityTransient_transient67() +
                " entityTransient_transient68: " + getEntityTransient_transient68() +
                " entityTransient_transient69: " + getEntityTransient_transient69() +
                " entityTransient_transient70: " + getEntityTransient_transient70() +
                " entityTransient_transient71: " + getEntityTransient_transient71() +
                " entityTransient_transient72: " + getEntityTransient_transient72() +
                " entityTransient_transient73: " + getEntityTransient_transient73() +
                " entityTransient_transient74: " + getEntityTransient_transient74() +
                " entityTransient_transient75: " + getEntityTransient_transient75() +
                " entityTransient_transient76: " + getEntityTransient_transient76() +
                " entityTransient_transient77: " + getEntityTransient_transient77() +
                " entityTransient_transient78: " + getEntityTransient_transient78() +
                " entityTransient_transient79: " + getEntityTransient_transient79() +
                " entityTransient_transient80: " + getEntityTransient_transient80() +
                " entityTransient_transient81: " + getEntityTransient_transient81() +
                " entityTransient_transient82: " + getEntityTransient_transient82() +
                " entityTransient_transient83: " + getEntityTransient_transient83() +
                " entityTransient_transient84: " + getEntityTransient_transient84());
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntityTransient_id() {
        return entityTransient_id;
    }

    public void setEntityTransient_id(int p) {
        this.entityTransient_id = p;
    }

    public long getEntityTransient_version() {
        return entityTransient_version;
    }

    public void setEntityTransient_version(long p) {
        this.entityTransient_version = p;
    }

    public Log getEntityTransient_transient05() {
        return entityTransient_transient05;
    }

    public void setEntityTransient_transient05(Log p) {
        this.entityTransient_transient05 = p;
    }

    public Log getEntityTransient_transient06() {
        return entityTransient_transient06;
    }

    public void setEntityTransient_transient06(Log p) {
        this.entityTransient_transient06 = p;
    }

    public Log[] getEntityTransient_transient07() {
        return entityTransient_transient07;
    }

    public void setEntityTransient_transient07(Log[] p) {
        this.entityTransient_transient07 = p;
    }

    public Log[] getEntityTransient_transient08() {
        return entityTransient_transient08;
    }

    public void setEntityTransient_transient08(Log[] p) {
        this.entityTransient_transient08 = p;
    }

    public Collection getEntityTransient_transient09() {
        return entityTransient_transient09;
    }

    public void setEntityTransient_transient09(Collection p) {
        this.entityTransient_transient09 = p;
    }

    public Collection getEntityTransient_transient10() {
        return entityTransient_transient10;
    }

    public void setEntityTransient_transient10(Collection p) {
        this.entityTransient_transient10 = p;
    }

    public Collection[] getEntityTransient_transient11() {
        return entityTransient_transient11;
    }

    public void setEntityTransient_transient11(Collection[] p) {
        this.entityTransient_transient11 = p;
    }

    public Collection[] getEntityTransient_transient12() {
        return entityTransient_transient12;
    }

    public void setEntityTransient_transient12(Collection[] p) {
        this.entityTransient_transient12 = p;
    }

    public boolean getEntityTransient_transient13() {
        return entityTransient_transient13;
    }

    public void setEntityTransient_transient13(boolean p) {
        this.entityTransient_transient13 = p;
    }

    public boolean getEntityTransient_transient14() {
        return entityTransient_transient14;
    }

    public void setEntityTransient_transient14(boolean p) {
        this.entityTransient_transient14 = p;
    }

    public boolean[] getEntityTransient_transient15() {
        return entityTransient_transient15;
    }

    public void setEntityTransient_transient15(boolean[] p) {
        this.entityTransient_transient15 = p;
    }

    public boolean[] getEntityTransient_transient16() {
        return entityTransient_transient16;
    }

    public void setEntityTransient_transient16(boolean[] p) {
        this.entityTransient_transient16 = p;
    }

    public long getEntityTransient_transient17() {
        return entityTransient_transient17;
    }

    public void setEntityTransient_transient17(long p) {
        this.entityTransient_transient17 = p;
    }

    public long getEntityTransient_transient18() {
        return entityTransient_transient18;
    }

    public void setEntityTransient_transient18(long p) {
        this.entityTransient_transient18 = p;
    }

    public long[] getEntityTransient_transient19() {
        return entityTransient_transient19;
    }

    public void setEntityTransient_transient19(long[] p) {
        this.entityTransient_transient19 = p;
    }

    public long[] getEntityTransient_transient20() {
        return entityTransient_transient20;
    }

    public void setEntityTransient_transient20(long[] p) {
        this.entityTransient_transient20 = p;
    }

    public Long getEntityTransient_transient21() {
        return entityTransient_transient21;
    }

    public void setEntityTransient_transient21(Long p) {
        this.entityTransient_transient21 = p;
    }

    public Long getEntityTransient_transient22() {
        return entityTransient_transient22;
    }

    public void setEntityTransient_transient22(Long p) {
        this.entityTransient_transient22 = p;
    }

    public Long[] getEntityTransient_transient23() {
        return entityTransient_transient23;
    }

    public void setEntityTransient_transient23(Long[] p) {
        this.entityTransient_transient23 = p;
    }

    public Long[] getEntityTransient_transient24() {
        return entityTransient_transient24;
    }

    public void setEntityTransient_transient24(Long[] p) {
        this.entityTransient_transient24 = p;
    }

    public Collection<Log> getEntityTransient_transient25() {
        return entityTransient_transient25;
    }

    public void setEntityTransient_transient25(Collection<Log> p) {
        this.entityTransient_transient25 = p;
    }

    public Collection<Log> getEntityTransient_transient26() {
        return entityTransient_transient26;
    }

    public void setEntityTransient_transient26(Collection<Log> p) {
        this.entityTransient_transient26 = p;
    }

    public Collection<Log>[] getEntityTransient_transient27() {
        return entityTransient_transient27;
    }

    public void setEntityTransient_transient27(Collection<Log>[] p) {
        this.entityTransient_transient27 = p;
    }

    public Collection<Log>[] getEntityTransient_transient28() {
        return entityTransient_transient28;
    }

    public void setEntityTransient_transient28(Collection<Log>[] p) {
        this.entityTransient_transient28 = p;
    }

    public int getEntityTransient_transient29() {
        return entityTransient_transient29;
    }

    public void setEntityTransient_transient29(int p) {
        this.entityTransient_transient29 = p;
    }

    public int getEntityTransient_transient30() {
        return entityTransient_transient30;
    }

    public void setEntityTransient_transient30(int p) {
        this.entityTransient_transient30 = p;
    }

    public int[] getEntityTransient_transient31() {
        return entityTransient_transient31;
    }

    public void setEntityTransient_transient31(int[] p) {
        this.entityTransient_transient31 = p;
    }

    public int[] getEntityTransient_transient32() {
        return entityTransient_transient32;
    }

    public void setEntityTransient_transient32(int[] p) {
        this.entityTransient_transient32 = p;
    }

    public Integer getEntityTransient_transient33() {
        return entityTransient_transient33;
    }

    public void setEntityTransient_transient33(Integer p) {
        this.entityTransient_transient33 = p;
    }

    public Integer getEntityTransient_transient34() {
        return entityTransient_transient34;
    }

    public void setEntityTransient_transient34(Integer p) {
        this.entityTransient_transient34 = p;
    }

    public Integer[] getEntityTransient_transient35() {
        return entityTransient_transient35;
    }

    public void setEntityTransient_transient35(Integer[] p) {
        this.entityTransient_transient35 = p;
    }

    public Integer[] getEntityTransient_transient36() {
        return entityTransient_transient36;
    }

    public void setEntityTransient_transient36(Integer[] p) {
        this.entityTransient_transient36 = p;
    }

    public short getEntityTransient_transient37() {
        return entityTransient_transient37;
    }

    public void setEntityTransient_transient37(short p) {
        this.entityTransient_transient37 = p;
    }

    public short getEntityTransient_transient38() {
        return entityTransient_transient38;
    }

    public void setEntityTransient_transient38(short p) {
        this.entityTransient_transient38 = p;
    }

    public short[] getEntityTransient_transient39() {
        return entityTransient_transient39;
    }

    public void setEntityTransient_transient39(short[] p) {
        this.entityTransient_transient39 = p;
    }

    public short[] getEntityTransient_transient40() {
        return entityTransient_transient40;
    }

    public void setEntityTransient_transient40(short[] p) {
        this.entityTransient_transient40 = p;
    }

    public Short getEntityTransient_transient41() {
        return entityTransient_transient41;
    }

    public void setEntityTransient_transient41(Short p) {
        this.entityTransient_transient41 = p;
    }

    public Short getEntityTransient_transient42() {
        return entityTransient_transient42;
    }

    public void setEntityTransient_transient42(Short p) {
        this.entityTransient_transient42 = p;
    }

    public Short[] getEntityTransient_transient43() {
        return entityTransient_transient43;
    }

    public void setEntityTransient_transient43(Short[] p) {
        this.entityTransient_transient43 = p;
    }

    public Short[] getEntityTransient_transient44() {
        return entityTransient_transient44;
    }

    public void setEntityTransient_transient44(Short[] p) {
        this.entityTransient_transient44 = p;
    }

    public Object getEntityTransient_transient45() {
        return entityTransient_transient45;
    }

    public void setEntityTransient_transient45(Object p) {
        this.entityTransient_transient45 = p;
    }

    public Object getEntityTransient_transient46() {
        return entityTransient_transient46;
    }

    public void setEntityTransient_transient46(Object p) {
        this.entityTransient_transient46 = p;
    }

    public Object[] getEntityTransient_transient47() {
        return entityTransient_transient47;
    }

    public void setEntityTransient_transient47(Object[] p) {
        this.entityTransient_transient47 = p;
    }

    public Object[] getEntityTransient_transient48() {
        return entityTransient_transient48;
    }

    public void setEntityTransient_transient48(Object[] p) {
        this.entityTransient_transient48 = p;
    }

    public Boolean getEntityTransient_transient49() {
        return entityTransient_transient49;
    }

    public void setEntityTransient_transient49(Boolean p) {
        this.entityTransient_transient49 = p;
    }

    public Boolean getEntityTransient_transient50() {
        return entityTransient_transient50;
    }

    public void setEntityTransient_transient50(Boolean p) {
        this.entityTransient_transient50 = p;
    }

    public Boolean[] getEntityTransient_transient51() {
        return entityTransient_transient51;
    }

    public void setEntityTransient_transient51(Boolean[] p) {
        this.entityTransient_transient51 = p;
    }

    public Boolean[] getEntityTransient_transient52() {
        return entityTransient_transient52;
    }

    public void setEntityTransient_transient52(Boolean[] p) {
        this.entityTransient_transient52 = p;
    }

    public String getEntityTransient_transient53() {
        return entityTransient_transient53;
    }

    public void setEntityTransient_transient53(String p) {
        this.entityTransient_transient53 = p;
    }

    public String getEntityTransient_transient54() {
        return entityTransient_transient54;
    }

    public void setEntityTransient_transient54(String p) {
        this.entityTransient_transient54 = p;
    }

    public String[] getEntityTransient_transient55() {
        return entityTransient_transient55;
    }

    public void setEntityTransient_transient55(String[] p) {
        this.entityTransient_transient55 = p;
    }

    public String[] getEntityTransient_transient56() {
        return entityTransient_transient56;
    }

    public void setEntityTransient_transient56(String[] p) {
        this.entityTransient_transient56 = p;
    }

    public BigDecimal getEntityTransient_transient57() {
        return entityTransient_transient57;
    }

    public void setEntityTransient_transient57(BigDecimal p) {
        this.entityTransient_transient57 = p;
    }

    public BigDecimal getEntityTransient_transient58() {
        return entityTransient_transient58;
    }

    public void setEntityTransient_transient58(BigDecimal p) {
        this.entityTransient_transient58 = p;
    }

    public BigDecimal[] getEntityTransient_transient59() {
        return entityTransient_transient59;
    }

    public void setEntityTransient_transient59(BigDecimal[] p) {
        this.entityTransient_transient59 = p;
    }

    public BigDecimal[] getEntityTransient_transient60() {
        return entityTransient_transient60;
    }

    public void setEntityTransient_transient60(BigDecimal[] p) {
        this.entityTransient_transient60 = p;
    }

    public java.util.Date getEntityTransient_transient61() {
        return entityTransient_transient61;
    }

    public void setEntityTransient_transient61(java.util.Date p) {
        this.entityTransient_transient61 = p;
    }

    public java.util.Date getEntityTransient_transient62() {
        return entityTransient_transient62;
    }

    public void setEntityTransient_transient62(java.util.Date p) {
        this.entityTransient_transient62 = p;
    }

    public java.util.Date[] getEntityTransient_transient63() {
        return entityTransient_transient63;
    }

    public void setEntityTransient_transient63(java.util.Date[] p) {
        this.entityTransient_transient63 = p;
    }

    public java.util.Date[] getEntityTransient_transient64() {
        return entityTransient_transient64;
    }

    public void setEntityTransient_transient64(java.util.Date[] p) {
        this.entityTransient_transient64 = p;
    }

    public java.sql.Date getEntityTransient_transient65() {
        return entityTransient_transient65;
    }

    public void setEntityTransient_transient65(java.sql.Date p) {
        this.entityTransient_transient65 = p;
    }

    public java.sql.Date getEntityTransient_transient66() {
        return entityTransient_transient66;
    }

    public void setEntityTransient_transient66(java.sql.Date p) {
        this.entityTransient_transient66 = p;
    }

    public java.sql.Date[] getEntityTransient_transient67() {
        return entityTransient_transient67;
    }

    public void setEntityTransient_transient67(java.sql.Date[] p) {
        this.entityTransient_transient67 = p;
    }

    public java.sql.Date[] getEntityTransient_transient68() {
        return entityTransient_transient68;
    }

    public void setEntityTransient_transient68(java.sql.Date[] p) {
        this.entityTransient_transient68 = p;
    }

    public float getEntityTransient_transient69() {
        return entityTransient_transient69;
    }

    public void setEntityTransient_transient69(float p) {
        this.entityTransient_transient69 = p;
    }

    public float getEntityTransient_transient70() {
        return entityTransient_transient70;
    }

    public void setEntityTransient_transient70(float p) {
        this.entityTransient_transient70 = p;
    }

    public float[] getEntityTransient_transient71() {
        return entityTransient_transient71;
    }

    public void setEntityTransient_transient71(float[] p) {
        this.entityTransient_transient71 = p;
    }

    public float[] getEntityTransient_transient72() {
        return entityTransient_transient72;
    }

    public void setEntityTransient_transient72(float[] p) {
        this.entityTransient_transient72 = p;
    }

    public Float getEntityTransient_transient73() {
        return entityTransient_transient73;
    }

    public void setEntityTransient_transient73(Float p) {
        this.entityTransient_transient73 = p;
    }

    public Float getEntityTransient_transient74() {
        return entityTransient_transient74;
    }

    public void setEntityTransient_transient74(Float p) {
        this.entityTransient_transient74 = p;
    }

    public Float[] getEntityTransient_transient75() {
        return entityTransient_transient75;
    }

    public void setEntityTransient_transient75(Float[] p) {
        this.entityTransient_transient75 = p;
    }

    public Float[] getEntityTransient_transient76() {
        return entityTransient_transient76;
    }

    public void setEntityTransient_transient76(Float[] p) {
        this.entityTransient_transient76 = p;
    }

    public char getEntityTransient_transient77() {
        return entityTransient_transient77;
    }

    public void setEntityTransient_transient77(char p) {
        this.entityTransient_transient77 = p;
    }

    public char getEntityTransient_transient78() {
        return entityTransient_transient78;
    }

    public void setEntityTransient_transient78(char p) {
        this.entityTransient_transient78 = p;
    }

    public char[] getEntityTransient_transient79() {
        return entityTransient_transient79;
    }

    public void setEntityTransient_transient79(char[] p) {
        this.entityTransient_transient79 = p;
    }

    public char[] getEntityTransient_transient80() {
        return entityTransient_transient80;
    }

    public void setEntityTransient_transient80(char[] p) {
        this.entityTransient_transient80 = p;
    }

    public Character getEntityTransient_transient81() {
        return entityTransient_transient81;
    }

    public void setEntityTransient_transient81(Character p) {
        this.entityTransient_transient81 = p;
    }

    public Character getEntityTransient_transient82() {
        return entityTransient_transient82;
    }

    public void setEntityTransient_transient82(Character p) {
        this.entityTransient_transient82 = p;
    }

    public Character[] getEntityTransient_transient83() {
        return entityTransient_transient83;
    }

    public void setEntityTransient_transient83(Character[] p) {
        this.entityTransient_transient83 = p;
    }

    public Character[] getEntityTransient_transient84() {
        return entityTransient_transient84;
    }

    public void setEntityTransient_transient84(Character[] p) {
        this.entityTransient_transient84 = p;
    }
}
