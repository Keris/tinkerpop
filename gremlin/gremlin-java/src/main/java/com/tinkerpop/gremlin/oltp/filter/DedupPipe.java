package com.tinkerpop.gremlin.oltp.filter;

import com.tinkerpop.gremlin.Pipeline;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class DedupPipe<S> extends FilterPipe<S> {

    public DedupPipe(final Pipeline pipeline, final Function<S, ?> uniqueFunction) {
        super(pipeline);
        final Set<Object> set = new LinkedHashSet<>();
        this.setPredicate(holder -> set.add(uniqueFunction.apply(holder.get())));
    }

    public DedupPipe(final Pipeline pipeline) {
        this(pipeline, s -> s);
    }
}
