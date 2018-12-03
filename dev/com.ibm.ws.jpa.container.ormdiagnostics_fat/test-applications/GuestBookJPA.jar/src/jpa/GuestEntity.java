package jpa;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@IdClass(GuestId.class)
@NamedQueries(value = {
                        @NamedQuery(name = "findAllGuests", query = "SELECT g FROM GuestEntity g"),
                        @NamedQuery(name = "findByFirstName", query = "SELECT g FROM GuestEntity g WHERE g.firstName = :firstname"),
                        @NamedQuery(name = "findByLastName", query = "SELECT g FROM GuestEntity g WHERE g.lastName = :lastname") })
public class GuestEntity {

    @Id
    private String firstName;
    @Id
    private String lastName;
    @Basic
    private java.time.LocalDateTime localDateTime;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public java.time.LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(java.time.LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }
}
