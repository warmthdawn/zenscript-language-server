package raylras.zen.code;

import com.google.common.collect.ImmutableList;
import raylras.zen.code.scope.Scope;
import raylras.zen.code.symbol.Symbol;
import raylras.zen.code.symbol.VariableSymbol;
import raylras.zen.service.LibraryService;
import raylras.zen.service.ScriptService;
import raylras.zen.service.TypeService;
import raylras.zen.util.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompilationEnvironment {
    private static final Logger logger = Logger.getLogger("environment");

    private final Path sourceRoot;
    public final LibraryService libraryService;
    public final ScriptService scriptService;
    public final TypeService typeService;

    private final Map<Path, CompilationUnit> libraryDeclarations = new HashMap<>();

    public CompilationEnvironment(Path sourceRoot) {
        this.libraryService = new LibraryService(new Scope(null, null));
        this.scriptService = new ScriptService(sourceRoot, fileManager);
        this.typeService = new TypeService();
        this.sourceRoot = sourceRoot;
    }

    public LibraryService libraryService() {
        return libraryService;
    }

    public ScriptService scriptService() {
        return scriptService;
    }

    public TypeService typeService() {
        return typeService;
    }

    // about compliation units


    public void reloadLibraries(List<CompilationUnit> dzsUnits) {

    }
    public void addCompilationUnit(CompilationUnit unit) {
        if (unit.isDzs()) {
            libraryDeclarations.put(unit.path, unit);
            libraryService().reload(libraryDeclarations.values());
        } else {
            scriptService().load(unit);
        }
    }


    public CompilationUnit getCompilationUnit(Path unitPath) {
        if (libraryDeclarations.containsKey(unitPath)) {
            return libraryDeclarations.get(unitPath);
        } else {
            return scriptService().getUnit(unitPath);
        }
    }

    public void removeCompilationUnit(Path unitPath) {
        if (libraryDeclarations.containsKey(unitPath)) {
            libraryDeclarations.remove(unitPath);
            libraryService().reload(libraryDeclarations.values());
        } else {
            scriptService().unload(unitPath);
        }
    }


    public <T extends Symbol> T findSymbol(Class<T> type, String name) {
        if (name.startsWith("scripts")) {
            return scriptService.findSymbol(type, name);
        }
        if (type.isAssignableFrom(VariableSymbol.class)) {
            // script only has global variables, not having methods
            T scriptGlobal = type.cast(scriptService.getGlobalVariable(name));
            if (scriptGlobal != null) {
                return scriptGlobal;
            }
        }

        return libraryService.findSymbol(type, name);
    }


    public List<Symbol> getExpandFunctions(String toString) {
        // TODO: scripts expand functions
        return libraryService.getExpandFunctions(toString);
    }

    public List<Symbol> getGlobals() {
        return ImmutableList.<Symbol>builder()
            .addAll(scriptService.getGlobalVariables())
            .addAll(libraryService().allGlobalVariables())
            .addAll(libraryService().allGlobalFunctions())
            .build();
    }

    public List<Symbol> getSymbolsOfPackage(String packageName) {
        return ImmutableList.<Symbol>builder()
            .addAll(scriptService.getSymbolsOfPackage(packageName))
            .addAll(libraryService.getSymbolsOfPackage(packageName))
            .build();
    }

    public Path getSourceRoot() {
        return sourceRoot;
    }

    public void unload() {
        //
    }
}