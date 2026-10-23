package dev.catdoc.api.web;

import dev.catdoc.api.application.ProjectQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectQueryService projectQueryService;

    public ProjectController(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    @GetMapping
    public List<ProjectSummaryResponse> listProjects() {
        return projectQueryService.listProjects().stream().map(ProjectSummaryResponse::from).toList();
    }
}
