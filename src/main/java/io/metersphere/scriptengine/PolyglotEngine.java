package io.metersphere.scriptengine;

import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;

import static io.metersphere.scriptengine.GraalPyEngineFactory.LANGUAGE_ID;

public final class PolyglotEngine implements ScriptEngine, Compilable, Invocable, AutoCloseable {
    private final ScriptEngineFactory factory;
    private PolyglotContext defaultContext;

    PolyglotEngine(ScriptEngineFactory factory) {
        this.factory = factory;
        this.defaultContext = new PolyglotContext(factory);
    }

    @Override
    public void close() {
        defaultContext.getContext().close();
    }

    @Override
    public CompiledScript compile(String script) throws ScriptException {
        Source src = Source.create(LANGUAGE_ID, script);
        try {
            defaultContext.getContext().parse(src); // only for the side-effect of validating the source
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
        return new PolyglotCompiledScript(src, this);
    }

    @Override
    public CompiledScript compile(Reader script) throws ScriptException {
        Source src;
        try {
            src = Source.newBuilder(LANGUAGE_ID, script, "sourcefromreader").build();
            defaultContext.getContext().parse(src); // only for the side-effect of validating the source
        } catch (PolyglotException | IOException e) {
            throw new ScriptException(e);
        }
        return new PolyglotCompiledScript(src, this);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (context instanceof PolyglotContext) {
            PolyglotContext c = (PolyglotContext) context;
            try {
                return c.getContext().eval(LANGUAGE_ID, script).as(Object.class);
            } catch (PolyglotException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ClassCastException("invalid context");
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        Source src;
        try {
            src = Source.newBuilder(LANGUAGE_ID, reader, "sourcefromreader").build();
        } catch (IOException e) {
            throw new ScriptException(e);
        }
        if (context instanceof PolyglotContext) {
            PolyglotContext c = (PolyglotContext) context;
            try {
                return c.getContext().eval(src).as(Object.class);
            } catch (PolyglotException e) {
                throw new ScriptException(e);
            }
        } else {
            throw new ScriptException("invalid context");
        }
    }

    @Override
    public Object eval(String script) throws ScriptException {
        return eval(script, defaultContext);
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, defaultContext);
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
        n.forEach((key, value) -> defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value));
        return eval(script);
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException {
        n.forEach((key, value) -> defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value));
        return eval(reader);
    }

    @Override
    public void put(String key, Object value) {
        defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).put(key, value);
    }

    @Override
    public Object get(String key) {
        return defaultContext.getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int scope) {
        return defaultContext.getBindings(scope);
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        defaultContext.setBindings(bindings, scope);
    }

    @Override
    public Bindings createBindings() {
        return defaultContext.getBindings(ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public ScriptContext getContext() {
        return defaultContext;
    }

    @Override
    public void setContext(ScriptContext context) {
        throw new UnsupportedOperationException("The context of a Polyglot ScriptEngine cannot be modified.");
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object invokeMethod(Object thiz, String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        try {
            Value receiver = defaultContext.getContext().asValue(thiz);
            if (receiver.canInvokeMember(name)) {
                return receiver.invokeMember(name, args).as(Object.class);
            } else {
                throw new NoSuchMethodException(name);
            }
        } catch (PolyglotException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getInterface(Class interfaceClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getInterface(Object thiz, Class interfaceClass) {
        return defaultContext.getContext().asValue(thiz).as(interfaceClass);
    }
}