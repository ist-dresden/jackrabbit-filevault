package org.apache.jackrabbit.vault.fs.impl.aggregator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.SerializerArtifact;
import org.apache.jackrabbit.vault.fs.api.Aggregate;
import org.apache.jackrabbit.vault.fs.api.AggregateManager;
import org.apache.jackrabbit.vault.fs.api.Aggregator;
import org.apache.jackrabbit.vault.fs.api.Artifact;
import org.apache.jackrabbit.vault.fs.api.ArtifactSet;
import org.apache.jackrabbit.vault.fs.api.ArtifactType;
import org.apache.jackrabbit.vault.fs.api.DumpContext;
import org.apache.jackrabbit.vault.fs.api.VaultFsConfig;
import org.apache.jackrabbit.vault.fs.impl.io.DocViewSerializer;
import org.apache.jackrabbit.vault.fs.impl.io.VersionsSerializer;
import org.apache.jackrabbit.vault.fs.io.Serializer;
import org.apache.jackrabbit.vault.util.Constants;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.PrintWriter;
import java.util.Collection;

import static org.apache.jackrabbit.vault.util.Constants.DOT_VERSIONS_XML;
import static org.apache.jackrabbit.vault.util.JcrConstants.MIX_VERSIONABLE;

public class VersionAggregatorDecorator extends AggregatorDecorator {

    public VersionAggregatorDecorator(Aggregator aggregator) {
        super(aggregator);
    }

    @Override
    public ArtifactSet createArtifacts(Aggregate aggregate) throws RepositoryException {
        boolean v = isVersionable(aggregate);
        if (v) {

        } else {
            Node vp = getVersionableParent(aggregate);
            if (!vp.getPath().equals("/")) {
            }
        }
        final ArtifactSet artifacts = super.createArtifacts(aggregate);
        if (v) {
            final Artifact directory = artifacts.getDirectory();
            Serializer ser = new VersionsSerializer(aggregate);
            final SerializerArtifact versions = new SerializerArtifact(directory, "", DOT_VERSIONS_XML, ArtifactType.VERSIONS, ser, 0);
            artifacts.add(versions);
        }
        return artifacts;
    }

    private boolean isVersionable(Aggregate aggregate) throws RepositoryException {
        return aggregate.includeVersions() && aggregate.getNode().isNodeType(JcrConstants.MIX_VERSIONABLE);
    }

    private Node getVersionableParent(Aggregate aggregate) throws RepositoryException {
        Node node = aggregate.getNode();
        while (!node.isNodeType(MIX_VERSIONABLE) && !node.getPath().equals("/")) {
            node = node.getParent();
        }
        return node;
    }

    @Override
    public boolean hasFullCoverage() {
        return super.hasFullCoverage();
    }
}
