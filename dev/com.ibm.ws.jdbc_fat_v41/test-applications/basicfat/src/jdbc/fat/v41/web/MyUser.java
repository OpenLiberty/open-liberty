/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jdbc.fat.v41.web;

import java.io.Serializable;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;

public class MyUser implements SQLData, Serializable {
    private static final long serialVersionUID = -4235126659654360181L;
    public String name;
    public String address;
    public String phone;
    private String sql_type;

    public MyUser(String name, String address, String phone) {
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return "{name=" + name + "  address=" + address + "  phone=" + phone + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MyUser)) {
            System.out.println("Object not an instance of MyUser: " + o.getClass().getCanonicalName());
            return false;
        }
        MyUser u = (MyUser) o;
        if (!this.name.equals(u.getName()))
            return false;
        if (!this.address.equals(u.getAddress()))
            return false;
        if (!this.phone.equals(u.getPhone()))
            return false;

        return true;
    }

    @Override
    public String getSQLTypeName() throws SQLException {
        return sql_type;
    }

    @Override
    public void readSQL(SQLInput stream, String typeName) throws SQLException {
        sql_type = typeName;
        name = stream.readString();
        address = stream.readString();
        phone = stream.readString();
    }

    @Override
    public void writeSQL(SQLOutput stream) throws SQLException {
        stream.writeString(name);
        stream.writeString(address);
        stream.writeString(phone);
    }
}
