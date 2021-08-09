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

package com.ibm.ws.jpa.example.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "SimpleExample")
public class SimpleExampleEntity {
    @Id
    private int id;

    private String str1;
    private String str2;
    private String str3;

    private char char1;
    private char char2;
    private char char3;

    private int int1;
    private int int2;
    private int int3;

    private long long1;
    private long long2;
    private long long3;

    private short short1;
    private short short2;
    private short short3;

    private float float1;
    private float float2;
    private float float3;

    private double double1;
    private double double2;
    private double double3;

    private boolean boolean1;
    private boolean boolean2;
    private boolean boolean3;

    private byte byte1;
    private byte byte2;
    private byte byte3;

    public SimpleExampleEntity() {

    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the str1
     */
    public String getStr1() {
        return str1;
    }

    /**
     * @param str1 the str1 to set
     */
    public void setStr1(String str1) {
        this.str1 = str1;
    }

    /**
     * @return the str2
     */
    public String getStr2() {
        return str2;
    }

    /**
     * @param str2 the str2 to set
     */
    public void setStr2(String str2) {
        this.str2 = str2;
    }

    /**
     * @return the str3
     */
    public String getStr3() {
        return str3;
    }

    /**
     * @param str3 the str3 to set
     */
    public void setStr3(String str3) {
        this.str3 = str3;
    }

    /**
     * @return the char1
     */
    public char getChar1() {
        return char1;
    }

    /**
     * @param char1 the char1 to set
     */
    public void setChar1(char char1) {
        this.char1 = char1;
    }

    /**
     * @return the char2
     */
    public char getChar2() {
        return char2;
    }

    /**
     * @param char2 the char2 to set
     */
    public void setChar2(char char2) {
        this.char2 = char2;
    }

    /**
     * @return the char3
     */
    public char getChar3() {
        return char3;
    }

    /**
     * @param char3 the char3 to set
     */
    public void setChar3(char char3) {
        this.char3 = char3;
    }

    /**
     * @return the int1
     */
    public int getInt1() {
        return int1;
    }

    /**
     * @param int1 the int1 to set
     */
    public void setInt1(int int1) {
        this.int1 = int1;
    }

    /**
     * @return the int2
     */
    public int getInt2() {
        return int2;
    }

    /**
     * @param int2 the int2 to set
     */
    public void setInt2(int int2) {
        this.int2 = int2;
    }

    /**
     * @return the int3
     */
    public int getInt3() {
        return int3;
    }

    /**
     * @param int3 the int3 to set
     */
    public void setInt3(int int3) {
        this.int3 = int3;
    }

    /**
     * @return the long1
     */
    public long getLong1() {
        return long1;
    }

    /**
     * @param long1 the long1 to set
     */
    public void setLong1(long long1) {
        this.long1 = long1;
    }

    /**
     * @return the long2
     */
    public long getLong2() {
        return long2;
    }

    /**
     * @param long2 the long2 to set
     */
    public void setLong2(long long2) {
        this.long2 = long2;
    }

    /**
     * @return the long3
     */
    public long getLong3() {
        return long3;
    }

    /**
     * @param long3 the long3 to set
     */
    public void setLong3(long long3) {
        this.long3 = long3;
    }

    /**
     * @return the short1
     */
    public short getShort1() {
        return short1;
    }

    /**
     * @param short1 the short1 to set
     */
    public void setShort1(short short1) {
        this.short1 = short1;
    }

    /**
     * @return the short2
     */
    public short getShort2() {
        return short2;
    }

    /**
     * @param short2 the short2 to set
     */
    public void setShort2(short short2) {
        this.short2 = short2;
    }

    /**
     * @return the short3
     */
    public short getShort3() {
        return short3;
    }

    /**
     * @param short3 the short3 to set
     */
    public void setShort3(short short3) {
        this.short3 = short3;
    }

    /**
     * @return the float1
     */
    public float getFloat1() {
        return float1;
    }

    /**
     * @param float1 the float1 to set
     */
    public void setFloat1(float float1) {
        this.float1 = float1;
    }

    /**
     * @return the float2
     */
    public float getFloat2() {
        return float2;
    }

    /**
     * @param float2 the float2 to set
     */
    public void setFloat2(float float2) {
        this.float2 = float2;
    }

    /**
     * @return the float3
     */
    public float getFloat3() {
        return float3;
    }

    /**
     * @param float3 the float3 to set
     */
    public void setFloat3(float float3) {
        this.float3 = float3;
    }

    /**
     * @return the double1
     */
    public double getDouble1() {
        return double1;
    }

    /**
     * @param double1 the double1 to set
     */
    public void setDouble1(double double1) {
        this.double1 = double1;
    }

    /**
     * @return the double2
     */
    public double getDouble2() {
        return double2;
    }

    /**
     * @param double2 the double2 to set
     */
    public void setDouble2(double double2) {
        this.double2 = double2;
    }

    /**
     * @return the double3
     */
    public double getDouble3() {
        return double3;
    }

    /**
     * @param double3 the double3 to set
     */
    public void setDouble3(double double3) {
        this.double3 = double3;
    }

    /**
     * @return the boolean1
     */
    public boolean isBoolean1() {
        return boolean1;
    }

    /**
     * @param boolean1 the boolean1 to set
     */
    public void setBoolean1(boolean boolean1) {
        this.boolean1 = boolean1;
    }

    /**
     * @return the boolean2
     */
    public boolean isBoolean2() {
        return boolean2;
    }

    /**
     * @param boolean2 the boolean2 to set
     */
    public void setBoolean2(boolean boolean2) {
        this.boolean2 = boolean2;
    }

    /**
     * @return the boolean3
     */
    public boolean isBoolean3() {
        return boolean3;
    }

    /**
     * @param boolean3 the boolean3 to set
     */
    public void setBoolean3(boolean boolean3) {
        this.boolean3 = boolean3;
    }

    /**
     * @return the byte1
     */
    public byte getByte1() {
        return byte1;
    }

    /**
     * @param byte1 the byte1 to set
     */
    public void setByte1(byte byte1) {
        this.byte1 = byte1;
    }

    /**
     * @return the byte2
     */
    public byte getByte2() {
        return byte2;
    }

    /**
     * @param byte2 the byte2 to set
     */
    public void setByte2(byte byte2) {
        this.byte2 = byte2;
    }

    /**
     * @return the byte3
     */
    public byte getByte3() {
        return byte3;
    }

    /**
     * @param byte3 the byte3 to set
     */
    public void setByte3(byte byte3) {
        this.byte3 = byte3;
    }

}
