package raylras.zen.code;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import raylras.zen.code.parser.ZenScriptLexer;
import raylras.zen.code.parser.ZenScriptParser;
import raylras.zen.code.symbol.*;
import raylras.zen.code.type.resolve.DefinitionResolver;
import raylras.zen.code.scope.Scope;
import raylras.zen.service.LibraryService;
import raylras.zen.service.TypeService;
import raylras.zen.util.ParseTreeProperty;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

public class CompilationUnit {

    public static final String FILE_EXTENSION = ".zs";
    public static final String DZS_FILE_EXTENSION = ".d.zs";

    public final Path path;

    private final CompilationEnvironment env;
    private final ParseTreeProperty<Scope> scopeProp = new ParseTreeProperty<>();
    private final ParseTreeProperty<Symbol> symbolProp = new ParseTreeProperty<>();

    private ParseTree parseTree;
    private CommonTokenStream tokenStream;

    public CompilationUnit(Path path, CompilationEnvironment env) {
        this.path = path;
        this.env = env;
    }

    public Scope lookupScope(ParseTree node) {
        ParseTree n = node;
        while (n != null) {
            Scope scope = scopeProp.get(n);
            if (scope != null) {
                return scope;
            }
            n = n.getParent();
        }
        return null;
    }

    public <T extends Symbol> T lookupSymbol(Class<T> type, Scope fromScope, String name, boolean searchGlobal) {
        Symbol symbol = lookupLocalSymbol(fromScope,
            s -> type.isInstance(s) && Objects.equals(s.getName(), name)
        );

        if (symbol == null && searchGlobal) {
            symbol = environment().findSymbol(type, name);
        }

        return type.cast(symbol);
    }


    /**
     * lookup class symbol, considering import
     */
    public ClassSymbol lookupClassSymbol(Scope fromScope, String name, boolean searchGlobal) {
        // priority: local -> import -> global
        Symbol symbol = lookupLocalSymbol(fromScope,
            it -> (it instanceof ClassSymbol || it instanceof ImportSymbol) &&
                Objects.equals(it.getName(), name)
        );

        if (symbol instanceof ClassSymbol) {
            return (ClassSymbol) symbol;
        }

        if (symbol instanceof ImportSymbol) {
            Symbol target = ((ImportSymbol) symbol).getSimpleTarget();
            if (target instanceof ClassSymbol) {
                return (ClassSymbol) target;
            } else {
                return null;
            }
        }

        if (searchGlobal) {
            return environment().findSymbol(ClassSymbol.class, name);
        }

        return null;
    }

    private Symbol lookupLocalSymbol(Scope fromScope, Predicate<Symbol> condition) {
        Scope scope = fromScope;
        Symbol symbol = null;
        while (scope != null) {
            symbol = scope.getSymbol(condition);
            if (symbol != null)
                break;
            scope = scope.parent;
        }

        return symbol;
    }

    public <T extends Symbol> List<T> lookupLocalSymbols(Class<T> type, Scope fromScope, Predicate<T> condition) {
        Scope scope = fromScope;
        List<T> result = new ArrayList<>();
        while (scope != null) {
            for (Symbol symbol : scope.getSymbols()) {
                if (type.isInstance(symbol) && condition.test(type.cast(symbol))) {
                    result.add(type.cast(symbol));
                }
            }
            scope = scope.parent;
        }

        return result;
    }

    public Scope getScope(ParseTree node) {
        return scopeProp.get(node);
    }

    @SuppressWarnings("unchecked")
    public <T extends Symbol> T getSymbol(ParseTree node) {
        return (T) symbolProp.get(node);
    }

    public LibraryService libraryService() {
        return environment().libraryService();
    }

    public TypeService typeService() {
        return environment().typeService();
    }

    public CompilationEnvironment environment() {
        return env;
    }

    public Collection<Scope> getScopes() {
        return scopeProp.getProperties();
    }

    public Collection<Symbol> getSymbols() {
        return symbolProp.getProperties();
    }

    public Collection<Symbol> getTopLevelSymbols() {
        return getScope(getParseTree()).symbols;
    }

    public void load(CharStream charStream) {
        this.symbolProp.clear();
        this.scopeProp.clear();
        parse(charStream);
        new DefinitionResolver(this, scopeProp, symbolProp).resolve();
    }

    private void parse(CharStream charStream) {
        ZenScriptLexer lexer = new ZenScriptLexer(charStream);
        tokenStream = new CommonTokenStream(lexer);
        ZenScriptParser parser = new ZenScriptParser(getTokenStream());
        parser.removeErrorListeners();
        parseTree = parser.compilationUnit();
    }

    public boolean isDzs() {
        return path.toString().endsWith(DZS_FILE_EXTENSION);
    }


    public String packageName() {
        return env.scriptService().packageName(this.path);
    }

    public ParseTree getParseTree() {
        return parseTree;
    }

    public CommonTokenStream getTokenStream() {
        return tokenStream;
    }
}
