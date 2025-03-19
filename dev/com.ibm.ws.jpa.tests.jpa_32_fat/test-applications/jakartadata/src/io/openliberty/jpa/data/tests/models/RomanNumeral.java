package io.openliberty.jpa.data.tests.models;
import java.util.ArrayList;
public record RomanNumeral(
                String name,
                String romanNumeral,
                ArrayList<String> romanNumeralSymbols
) {

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(name).append(" ");
        s.append(romanNumeral).append(" ( ");
        s.append(romanNumeralSymbols).append(' ');
        s.append(")");
        return s.toString();
    }
}