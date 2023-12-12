package io.metersphere.python.scriptengine;

import org.graalvm.polyglot.Source;

import javax.script.*;


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
        if (context instanceof SimpleScriptContext) {
            context.getScopes().forEach((scope) -> {
                Bindings bindings = context.getBindings(scope);
                bindings.forEach((key, value) -> {
                    getEngine().getContext().setAttribute(key, value, scope);
                });
            });
            return engine.eval(source.getCharacters().toString(), getEngine().getContext());
        }
        throw new UnsupportedOperationException(
                "Polyglot CompiledScript instances can only be evaluated in Polyglot.");
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }
}
