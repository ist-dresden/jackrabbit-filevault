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
        this.aggregator = aggregator;
    }

    @Override
    public ArtifactSet createArtifacts(Aggregate aggregate) throws RepositoryException {
        return aggregator.createArtifacts(aggregate);
    }

    @Override
    public boolean includes(Node root, Node node, String path) throws RepositoryException {
        return aggregator.includes(root, node, path);
    }

    @Override
    public boolean includes(Node root, Node parent, Property property, String path) throws RepositoryException {
        return aggregator.includes(root, parent, property, path);
    }

    @Override
    public boolean matches(Node node, String path) throws RepositoryException {
        return aggregator.matches(node, path);
    }

    @Override
    public boolean hasFullCoverage() {
        return aggregator.hasFullCoverage();
    }

    @Override
    public boolean isDefault() {
        return aggregator.isDefault();
    }

    @Override
    public ImportInfo remove(Node node, boolean recursive, boolean trySave) throws RepositoryException {
        return aggregator.remove(node, recursive, trySave);
    }

    @Override
    public void dump(DumpContext ctx, boolean isLast) {
        aggregator.dump(ctx, isLast);
    }
}
