package io.metersphere.scriptengine;

import org.graalvm.polyglot.Context;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import java.io.*;
import java.util.List;
import java.util.Map;

import static io.metersphere.scriptengine.GraalPyEngineFactory.LANGUAGE_ID;

public final class PolyglotContext implements ScriptContext {
    private Context context;
    private final ScriptEngineFactory factory;
    private final PolyglotReader in;
    private final PolyglotWriter out;
    private final PolyglotWriter err;
    private Bindings globalBindings;

    PolyglotContext(ScriptEngineFactory factory) {
        this.factory = factory;
        this.in = new PolyglotReader(new InputStreamReader(System.in));
        this.out = new PolyglotWriter(new OutputStreamWriter(System.out));
        this.err = new PolyglotWriter(new OutputStreamWriter(System.err));
    }

    Context getContext() {
        if (context == null) {
            Context.Builder builder = Context.newBuilder(LANGUAGE_ID)
                    .in(this.in)
                    .out(this.out)
                    .err(this.err)
                    .allowAllAccess(true);
            Bindings globalBindings = getBindings(ScriptContext.GLOBAL_SCOPE);
            if (globalBindings != null) {
                for (Map.Entry<String, Object> entry : globalBindings.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        builder.option(entry.getKey(), (String) value);
                    }
                }
            }
            context = builder.build();
        }
        return context;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (context == null) {
                globalBindings = bindings;
            } else {
                throw new UnsupportedOperationException(
                        "Global bindings for Polyglot language can only be set before the context is initialized.");
            }
        } else {
            throw new UnsupportedOperationException("Bindings objects for Polyglot language is final.");
        }
    }

    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return new PolyglotBindings(getContext().getBindings(LANGUAGE_ID));
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return globalBindings;
        } else {
            return null;
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            getBindings(scope).put(name, value);
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (context == null) {
                globalBindings.put(name, value);
            } else {
                throw new IllegalStateException("Cannot modify global bindings after context creation.");
            }
        }
    }

    @Override
    public Object getAttribute(String name, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return getBindings(scope).get(name);
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return globalBindings.get(name);
        }
        return null;
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        Object prev = getAttribute(name, scope);
        if (prev != null) {
            if (scope == ScriptContext.ENGINE_SCOPE) {
                getBindings(scope).remove(name);
            } else if (scope == ScriptContext.GLOBAL_SCOPE) {
                if (context == null) {
                    globalBindings.remove(name);
                } else {
                    throw new IllegalStateException("Cannot modify global bindings after context creation.");
                }
            }
        }
        return prev;
    }

    @Override
    public Object getAttribute(String name) {
        return getAttribute(name, ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public int getAttributesScope(String name) {
        if (getAttribute(name, ScriptContext.ENGINE_SCOPE) != null) {
            return ScriptContext.ENGINE_SCOPE;
        } else if (getAttribute(name, ScriptContext.GLOBAL_SCOPE) != null) {
            return ScriptContext.GLOBAL_SCOPE;
        }
        return -1;
    }

    @Override
    public Writer getWriter() {
        return this.out.writer;
    }

    @Override
    public Writer getErrorWriter() {
        return this.err.writer;
    }

    @Override
    public void setWriter(Writer writer) {
        this.out.writer = writer;
    }

    @Override
    public void setErrorWriter(Writer writer) {
        this.err.writer = writer;
    }

    @Override
    public Reader getReader() {
        return this.in.reader;
    }

    @Override
    public void setReader(Reader reader) {
        this.in.reader = reader;
    }

    @Override
    public List getScopes() {
        return List.of(ScriptContext.ENGINE_SCOPE, ScriptContext.GLOBAL_SCOPE);
    }

    private static final class PolyglotReader extends InputStream {
        private volatile Reader reader;

        public PolyglotReader(InputStreamReader inputStreamReader) {
            this.reader = inputStreamReader;
        }

        @Override
        public int read() throws IOException {
            return reader.read();
        }
    }

    private static final class PolyglotWriter extends OutputStream {
        private volatile Writer writer;

        public PolyglotWriter(OutputStreamWriter outputStreamWriter) {
            this.writer = outputStreamWriter;
        }

        @Override
        public void write(int b) throws IOException {
            writer.write(b);
        }
    }
}