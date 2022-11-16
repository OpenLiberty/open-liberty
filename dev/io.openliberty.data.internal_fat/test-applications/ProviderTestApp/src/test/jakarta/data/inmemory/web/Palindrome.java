/**
 *
 */
package test.jakarta.data.inmemory.web;

/**
 * Entity class for tests
 */
public class Palindrome {
    public long id;
    public String letters;
    public int length;

    public Palindrome() {
    }

    public Palindrome(long id, String letters) {
        this.id = id;
        this.letters = letters;
        this.length = letters.length();
    }
}
