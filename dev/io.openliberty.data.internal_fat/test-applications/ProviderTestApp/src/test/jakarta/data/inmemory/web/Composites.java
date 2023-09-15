/**
 *
 */
package test.jakarta.data.inmemory.web;

import java.util.List;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Limit;
import jakarta.data.repository.Repository;

/**
 * Repository for the fake Composite entity.
 */
@Repository
public interface Composites extends DataRepository<Composite, Long> {
    List<Composite> findByFactorsContainsOrderByIdAsc(long factor, Limit limit);

    List<Composite> findByNumUniqueFactorsOrderByIdAsc(int numFactors, Limit limit);
}
