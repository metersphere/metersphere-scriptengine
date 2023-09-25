package io.metersphere.scriptengine;

import org.graalvm.polyglot.Source;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;


public class PolyglotCompiledScript extends CompiledScript {
    private final Source source;
    private final ScriptEngine engine;

    public PolyglotCompiledScript(Source src, ScriptEngine engine) {
        this.source = src;
        this.engine = engine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        if (context instanceof PolyglotContext) {
            return ((PolyglotContext) context).getContext().eval(source).as(Object.class);
        }
        throw new UnsupportedOperationException(
                "Polyglot CompiledScript instances can only be evaluated in Polyglot.");
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }
}
