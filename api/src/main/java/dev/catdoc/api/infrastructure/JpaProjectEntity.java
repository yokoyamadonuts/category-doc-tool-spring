package dev.catdoc.api.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "projects")
public class JpaProjectEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected JpaProjectEntity() {}

    public JpaProjectEntity(String id, String name, Instant importedAt) {
        this.id = id;
        this.name = name;
        this.importedAt = importedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
