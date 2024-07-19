/**
 *
 */
package io.openliberty.jpa.data.tests.models;

import java.io.Serializable;

/**
 * Recreate from Jakarta Data TCK
 */
@jakarta.persistence.Entity
public class AsciiCharacter implements Serializable {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    private long id;

    private int numericValue;

    private String hexadecimal;

    private char thisCharacter;

    private boolean isControl;

    public static AsciiCharacter of(int numericValue) {
        AsciiCharacter inst = new AsciiCharacter();
        inst.id = numericValue;
        inst.numericValue = numericValue;
        inst.hexadecimal = Integer.toHexString(numericValue);
        inst.thisCharacter = (char) numericValue;
        inst.isControl = Character.isISOControl(inst.thisCharacter);

        return inst;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(int numericValue) {
        this.numericValue = numericValue;
    }

    public String getHexadecimal() {
        return hexadecimal;
    }

    public void setHexadecimal(String hexadecimal) {
        this.hexadecimal = hexadecimal;
    }

    public char getThisCharacter() {
        return thisCharacter;
    }

    public void setThisCharacter(char thisCharacter) {
        this.thisCharacter = thisCharacter;
    }

    public boolean isControl() {
        return isControl;
    }

    public void setControl(boolean isControl) {
        this.isControl = isControl;
    }

}