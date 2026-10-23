package dev.catdoc.api.application;

import dev.catdoc.domain.Category;
import dev.catdoc.domain.CategoryObject;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** プロジェクトと、その中のカテゴリや文書を読む。 */
@Service
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projectRepository;

    public ProjectQueryService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectSummary> listProjects() {
        return projectRepository.findAllSummaries();
    }

    public Project getProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '%s' was not found".formatted(projectId)));
    }

    public Category getCategory(UUID projectId, String categoryId) {
        return getProject(projectId).graph().category(categoryId)
                .orElseThrow(() -> new NotFoundException("Category '%s' was not found".formatted(categoryId)));
    }

    /** 文書と、文書を含むカテゴリの ID。 */
    public record ObjectInCategory(CategoryObject object, String categoryId) {
    }

    public ObjectInCategory getObject(UUID projectId, String objectId) {
        var graph = getProject(projectId).graph();
        return graph.categoryOf(objectId)
                .map(c -> new ObjectInCategory(c.object(objectId).orElseThrow(), c.id()))
                .orElseThrow(() -> new NotFoundException("Object '%s' was not found".formatted(objectId)));
    }
}
