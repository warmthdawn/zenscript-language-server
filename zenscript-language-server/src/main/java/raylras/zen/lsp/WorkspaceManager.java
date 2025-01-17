package raylras.zen.lsp;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import raylras.zen.model.CompilationEnvironment;
import raylras.zen.model.CompilationUnit;
import raylras.zen.model.Compilations;
import raylras.zen.model.Document;
import raylras.zen.util.PathUtil;
import raylras.zen.util.l10n.L10N;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.stream.Stream;

public class WorkspaceManager implements LanguageClientAware {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceManager.class);

    private final Set<Workspace> workspaceSet = new HashSet<>();
    private LanguageClient client;

    @Override
    public void connect(LanguageClient client) {
        this.client = client;
    }

    public Document openAsRead(TextDocumentIdentifier textDocument) {
        Path path = PathUtil.toPath(textDocument.getUri());
        Optional<CompilationUnit> compilationUnit = getUnit(path);
        Optional<ReadLock> readLock = compilationUnit.map(unit -> unit.getEnv().readLock());
        readLock.ifPresent(ReadLock::lock);
        return new Document() {
            @Override
            public Optional<CompilationUnit> getUnit() {
                return compilationUnit;
            }

            @Override
            public void close() {
                readLock.ifPresent(ReadLock::unlock);
            }
        };
    }

    public Document openAsWrite(TextDocumentIdentifier textDocument) {
        Path path = PathUtil.toPath(textDocument.getUri());
        Optional<CompilationUnit> compilationUnit = getUnit(path);
        Optional<WriteLock> writeLock = compilationUnit.map(unit -> unit.getEnv().writeLock());
        writeLock.ifPresent(WriteLock::lock);
        return new Document() {
            @Override
            public Optional<CompilationUnit> getUnit() {
                return compilationUnit;
            }

            @Override
            public void close() {
                writeLock.ifPresent(WriteLock::unlock);
            }
        };
    }

    public void addWorkspace(WorkspaceFolder folder) {
        Path workspacePath = PathUtil.toPath(folder.getUri());
        workspaceSet.add(new Workspace(workspacePath));
    }

    public void removeWorkspace(WorkspaceFolder folder) {
        Path workspacePath = PathUtil.toPath(folder.getUri());
        workspaceSet.removeIf(workspace -> workspace.path().equals(workspacePath));
    }

    public void createEnvIfNotExists(Path documentPath) {
        if (Compilations.isZsFile(documentPath)) {
            if (getEnv(documentPath).isEmpty()) {
                createEnv(documentPath);
            }
        }
    }

    public void createEnv(Path documentPath) {
        getWorkspace(documentPath).ifPresentOrElse(
                workspace -> {
                    Path compilationRoot = PathUtil.findUpwardsOrSelf(documentPath, CompilationEnvironment.DEFAULT_ROOT_DIRECTORY);
                    CompilationEnvironment env = new CompilationEnvironment(compilationRoot);
                    Compilations.load(env);
                    workspace.add(env);
                    checkDzs(env);
                },
                () -> logger.warn("Could not find workspace for document: {}", documentPath)
        );
    }

    public Optional<CompilationEnvironment> getEnv(Path documentPath) {
        return getWorkspace(documentPath).stream()
                .flatMap(Workspace::stream)
                .filter(env -> PathUtil.isSubPath(documentPath, env.getRoot()))
                .findFirst();
    }

    private Optional<Workspace> getWorkspace(Path documentPath) {
        return workspaceSet.stream()
                .filter(workspace -> PathUtil.isSubPath(documentPath, workspace.path))
                .findFirst();
    }

    private Optional<CompilationUnit> getUnit(Path documentPath) {
        return getEnv(documentPath).map(env -> env.getUnit(documentPath));
    }

    private void checkDzs(CompilationEnvironment env) {
        if (env.getGeneratedRoot().isEmpty()) {
            logger.info("Cannot find .dzs file directory of environment: {}", env);
            client.showMessage(new MessageParams(MessageType.Info, L10N.getString("environment.dzs_not_found")));
        }
    }

    public record Workspace(Path path, Set<CompilationEnvironment> envSet) implements Iterable<CompilationEnvironment> {
        public Workspace(Path path) {
            this(path, new HashSet<>());
        }

        public void add(CompilationEnvironment env) {
            envSet.add(env);
        }

        public Stream<CompilationEnvironment> stream() {
            return envSet.stream();
        }

        @Override
        public Iterator<CompilationEnvironment> iterator() {
            return envSet.iterator();
        }
    }

}
