/**
 *
 */
package test.jakarta.data.inmemory.web;

import java.util.List;

import jakarta.data.Limit;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;

/**
 *
 */
@Repository
public interface Palindromes extends DataRepository<Palindrome, Long> {
    List<Palindrome> findByLengthOrderByLettersAsc(int length, Limit limit);
}
