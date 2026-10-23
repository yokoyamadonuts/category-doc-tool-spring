package dev.catdoc.api.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProjectJpaRepository extends JpaRepository<JpaProjectEntity, String> {

    List<JpaProjectEntity> findAllByOrderByImportedAtDescIdAsc();
}
