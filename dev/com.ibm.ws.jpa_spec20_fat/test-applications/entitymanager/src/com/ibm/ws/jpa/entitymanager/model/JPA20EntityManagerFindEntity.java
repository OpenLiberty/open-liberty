package com.ibm.ws.jpa.entitymanager.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class JPA20EntityManagerFindEntity {
    @Id
    private int id;

    private String lastName;
    private String firstName;

    private int vacationDays;

    private transient String str = null;

    public JPA20EntityManagerFindEntity() {}

    public JPA20EntityManagerFindEntity(int id, String lastName, String firstName) {
        this.id = id;
        this.lastName = lastName;
        this.firstName = firstName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        str = null;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        str = null;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        str = null;
    }

    public int getVacationDays() {
        return vacationDays;
    }

    public void setVacationDays(int vacationDays) {
        this.vacationDays = vacationDays;
        str = null;
    }

    @Override
    public String toString() {
        if (str == null) {
            StringBuffer sb = new StringBuffer();
            sb.append("Employee: ");
            sb.append(getLastName()).append(", ").append(getFirstName());
            sb.append(" Vacation Days: ").append(getVacationDays());
            str = new String(sb);
        }
        return str;
    }
}
