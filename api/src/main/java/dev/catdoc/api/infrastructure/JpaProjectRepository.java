package dev.catdoc.api.infrastructure;

import dev.catdoc.api.application.ProjectRepository;
import dev.catdoc.api.application.ProjectSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaProjectRepository implements ProjectRepository {

    private final SpringDataProjectJpaRepository jpa;

    JpaProjectRepository(SpringDataProjectJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ProjectSummary> findAllSummaries() {
        return jpa.findAllByOrderByImportedAtDescIdAsc().stream()
                .map(e -> new ProjectSummary(UUID.fromString(e.getId()), e.getName(), e.getImportedAt()))
                .toList();
    }
}
