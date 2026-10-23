package dev.catdoc.api.application;

import dev.catdoc.api.application.importing.ProjectArchiveReader;
import dev.catdoc.api.application.importing.ProjectAssembler;
import java.io.InputStream;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** プロジェクトの取り込みと削除。 */
@Service
public class ProjectCommandService {

    private final ProjectArchiveReader archiveReader;
    private final ProjectRepository projectRepository;
    private final Clock clock;

    public ProjectCommandService(ProjectArchiveReader archiveReader, ProjectRepository projectRepository, Clock clock) {
        this.archiveReader = archiveReader;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    /**
     * zip を読み、構造を確かめてから1つのトランザクションで保存する。
     * 読み込みと確かめは保存の前に済ませるので、誤りがあれば何も保存しない。
     */
    @Transactional
    public ProjectSummary importProject(String name, InputStream archive) {
        var graph = ProjectAssembler.assemble(archiveReader.read(archive));
        // DB の DATETIME(6) に合わせ、マイクロ秒で切る。読み戻したときに等しくなるように
        var project = new Project(UUID.randomUUID(), name, clock.instant().truncatedTo(ChronoUnit.MICROS), graph);
        projectRepository.save(project);
        return project.summary();
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        if (!projectRepository.deleteById(projectId)) {
            throw new NotFoundException("Project '%s' was not found".formatted(projectId));
        }
    }
}
