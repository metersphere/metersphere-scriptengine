package io.metersphere.scriptengine;

import org.graalvm.home.Version;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Language;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.List;
import java.util.Objects;

public final class GraalPyEngineFactory implements ScriptEngineFactory {
    public static final String LANGUAGE_ID = "python";

    /***********************************************************/
    /* Everything below is generic and does not need to change */
    /***********************************************************/

    private final Engine polyglotEngine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").build();
    private final Language language = polyglotEngine.getLanguages().get(LANGUAGE_ID);

    @Override
    public String getEngineName() {
        return language.getImplementationName();
    }

    @Override
    public String getEngineVersion() {
        return Version.getCurrent().toString();
    }

    @Override
    public List getExtensions() {
        return List.of(LANGUAGE_ID);
    }

    @Override
    public List getMimeTypes() {
        return List.copyOf(language.getMimeTypes());
    }

    @Override
    public List getNames() {
        return List.of(language.getName(), LANGUAGE_ID, language.getImplementationName());
    }

    @Override
    public String getLanguageName() {
        return language.getName();
    }

    @Override
    public String getLanguageVersion() {
        return language.getVersion();
    }

    @Override
    public Object getParameter(final String key) {
        switch (key) {
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case ScriptEngine.NAME:
                return LANGUAGE_ID;
        }
        return null;
    }

    @Override
    public String getMethodCallSyntax(final String obj, final String m, final String... args) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("%s.%s(", obj, m));
        int i = args.length;
        for (String arg : args) {
            buffer.append(arg);
            if (i-- > 0) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    @Override
    public String getOutputStatement(final String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(final String... statements) {
        StringBuilder buffer = new StringBuilder();
        for (String statement : statements) {
            buffer.append(statement);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new PolyglotEngine(this);
    }


}

