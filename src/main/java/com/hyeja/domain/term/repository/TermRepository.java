package com.hyeja.domain.term.repository;

import com.hyeja.domain.term.entity.Term;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findAllByDeletedAtIsNullOrderByTermIdAsc();

    Optional<Term> findByTermIdAndDeletedAtIsNull(Long termId);
}
