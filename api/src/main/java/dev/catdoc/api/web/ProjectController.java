package dev.catdoc.api.web;

import dev.catdoc.api.application.InvalidRequestException;
import dev.catdoc.api.application.ProjectCommandService;
import dev.catdoc.api.application.ProjectQueryService;
import dev.catdoc.api.web.ProjectResponses.CategoryGraph;
import dev.catdoc.api.web.ProjectResponses.ObjectDetail;
import dev.catdoc.api.web.ProjectResponses.ProjectDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final int MAX_NAME_LENGTH = 200;

    private final ProjectQueryService projectQueryService;
    private final ProjectCommandService projectCommandService;

    public ProjectController(ProjectQueryService projectQueryService, ProjectCommandService projectCommandService) {
        this.projectQueryService = projectQueryService;
        this.projectCommandService = projectCommandService;
    }

    @GetMapping
    public List<ProjectSummaryResponse> listProjects() {
        return projectQueryService.listProjects().stream().map(ProjectSummaryResponse::from).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "201")
    public ResponseEntity<ProjectSummaryResponse> importProject(@RequestPart("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name) throws IOException {
        var projectName = projectName(name, file.getOriginalFilename());
        ProjectSummaryResponse created;
        try (var archive = file.getInputStream()) {
            created = ProjectSummaryResponse.from(projectCommandService.importProject(projectName, archive));
        }
        // 相対の URL にする。中継の nginx や Docker のポートの割り当てに左右されないように
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @GetMapping("/{projectId}")
    public ProjectDetail getProject(@PathVariable UUID projectId) {
        return ProjectDetail.from(projectQueryService.getProject(projectId));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable UUID projectId) {
        projectCommandService.deleteProject(projectId);
    }

    @GetMapping("/{projectId}/categories/{categoryId}/graph")
    public CategoryGraph getCategoryGraph(@PathVariable UUID projectId, @PathVariable String categoryId) {
        return CategoryGraph.from(projectQueryService.getCategory(projectId, categoryId));
    }

    @GetMapping("/{projectId}/objects/{objectId}")
    public ObjectDetail getObject(@PathVariable UUID projectId, @PathVariable String objectId) {
        return ObjectDetail.from(projectQueryService.getObject(projectId, objectId));
    }

    /** 名前を省けば、zip のファイル名から拡張子を除いたもの。 */
    private static String projectName(String name, String filename) {
        var resolved = name;
        if (resolved == null || resolved.isBlank()) {
            resolved = filename == null ? "" : filename.replaceFirst("(?i)\\.zip$", "");
        }
        resolved = resolved.strip();
        if (resolved.isEmpty()) {
            resolved = "project";
        }
        if (resolved.length() > MAX_NAME_LENGTH) {
            throw new InvalidRequestException("Project name must be at most %d characters".formatted(MAX_NAME_LENGTH));
        }
        return resolved;
    }
}
