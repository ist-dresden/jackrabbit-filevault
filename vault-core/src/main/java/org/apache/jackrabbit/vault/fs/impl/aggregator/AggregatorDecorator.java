package org.apache.jackrabbit.vault.fs.impl.aggregator;

import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.Aggregator;
import org.apache.jackrabbit.vault.fs.api.ArtifactSet;
import org.apache.jackrabbit.vault.fs.api.DumpContext;
import org.apache.jackrabbit.vault.fs.api.ImportInfo;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Date;

public abstract class AggregatorDecorator implements Aggregator {

    private Aggregator aggregator;

    protected AggregatorDecorator(Aggregator aggregator) {
        System.out.print(new Date() + ": ");
        System.out.print("AggregatorDecorator.AggregatorDecorator ");
        System.out.println("aggregator = [" + aggregator + "]");
        this.aggregator = aggregator;
    }

    @Override
    public ArtifactSet createArtifacts(Aggregate aggregate) throws RepositoryException {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".createArtifacts ");
        System.out.print("aggregate = [" + aggregate + "] ("+aggregate.getPath()+")");
        final ArtifactSet artifacts = aggregator.createArtifacts(aggregate);
        System.out.println(" => " + artifacts);
        return artifacts;
    }

    @Override
    public boolean includes(Node root, Node node, String path) throws RepositoryException {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".includes ");
        System.out.print("root = [" + root.getPath() + "], node = [" + node.getPath() + "], path = [" + path + "]");
        final boolean includes = aggregator.includes(root, node, path);
        System.out.println(" => " + includes);
        return includes;
    }

    @Override
    public boolean includes(Node root, Node parent, Property property, String path) throws RepositoryException {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".includes ");
        System.out.print("root = [" + root.getPath() + "], parent = [" + parent.getPath() + "], property = [" + property.getPath() + "], path = [" + path + "]");
        final boolean includes = aggregator.includes(root, parent, property, path);
        System.out.println(" => " + includes);
        return includes;
    }

    @Override
    public boolean matches(Node node, String path) throws RepositoryException {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".matches ");
        System.out.print("node = [" + node.getPath() + "], path = [" + path + "]");
        final boolean matches = aggregator.matches(node, path);
        System.out.println("=> " + matches);
        return matches;
    }

    @Override
    public boolean hasFullCoverage() {
//        System.out.print(new Date() + ": ");
//        System.out.print(aggregator.getClass().getSimpleName() + ".hasFullCoverage ");
        final boolean b = aggregator.hasFullCoverage();
//        System.out.println(" => " + b);
        return b;
    }

    @Override
    public boolean isDefault() {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".isDefault ");
        final boolean aDefault = aggregator.isDefault();
        System.out.println(" => " + aDefault);
        return aDefault;
    }

    @Override
    public ImportInfo remove(Node node, boolean recursive, boolean trySave) throws RepositoryException {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".remove ");
        System.out.print("node = [" + node.getPath() + "], recursive = [" + recursive + "], trySave = [" + trySave + "]");
        final ImportInfo remove = aggregator.remove(node, recursive, trySave);
        System.out.println(" => " + remove);
        return remove;
    }

    @Override
    public void dump(DumpContext ctx, boolean isLast) {
        System.out.print(new Date() + ": ");
        System.out.print(aggregator.getClass().getSimpleName() + ".dump ");
        System.out.println("ctx = [" + ctx + "], isLast = [" + isLast + "]");
        aggregator.dump(ctx, isLast);
    }
}
