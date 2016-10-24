/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.vault.fs.impl.io;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jackrabbit.vault.fs.api.Artifact;
import org.apache.jackrabbit.vault.fs.api.ArtifactType;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.api.SerializationType;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.impl.ArtifactSetImpl;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.util.JcrConstants;
import org.xml.sax.SAXException;

/**
 * Handles artifact sets with just a directory.
 *
 */
public class FolderArtifactHandler extends AbstractArtifactHandler {

    /**
     * node type to use for the folders
     */
    private String nodeType = JcrConstants.NT_FOLDER;

    /**
     * Returns the node type that is used to create folders.
     * @return the node type name.
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Sets the node type name that is used to create folders. Default is "nt:folder"
     * @param nodeType the node type name
     */
    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * {@inheritDoc}
     *
     * Handles generic artifact sets
     */
    public ImportInfoImpl accept(WorkspaceFilter wspFilter, Node parent, String name,
                             ArtifactSetImpl artifacts)
            throws RepositoryException, IOException {
        Artifact dir = artifacts.getDirectory();
        final Artifact versions = artifacts.getVersionsData();
        if (dir == null || (artifacts.size() != 1 && versions == null)) {
            return null;
        }
        ImportInfoImpl info = new ImportInfoImpl();
        if (dir.getRelativePath().length() == 0) {
            // special check if artifact addresses itself
            return info;
        }
        if (!parent.hasNode(dir.getRelativePath())) {
            Node node = parent.addNode(dir.getRelativePath(), nodeType);
            info.onCreated(node.getPath());
        } else if (versions != null) {
            // import versions
            final VaultInputSource inputSource = versions.getInputSource();
            if (inputSource != null && versions.getSerializationType() == SerializationType.XML_GENERIC) {
                try {
                    VersionsSAXImporter handler = new VersionsSAXImporter(parent);
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
                    SAXParser parser = factory.newSAXParser();
                    parser.parse(inputSource, handler);
                } catch (ParserConfigurationException e) {
                    throw new RepositoryException(e);
                } catch (SAXException e) {
                    throw new IOException(e);
                }
            }
        } else {
            // sync nodes
            Set<String> hints = new HashSet<String>();
            String rootPath = parent.getPath();
            if (!rootPath.equals("/")) {
                rootPath += "/";
            }
            for (Artifact a: artifacts.values(ArtifactType.HINT)) {
                hints.add(rootPath + a.getRelativePath());
            }

            Node node = parent.getNode(dir.getRelativePath());
            NodeIterator iter = node.getNodes();
            while (iter.hasNext()) {
                Node child = iter.nextNode();
                String path = child.getPath();
                if (wspFilter.contains(path)) {
                    if (wspFilter.getImportMode(path) == ImportMode.REPLACE) {
                        if (!hints.contains(path)) {
                            // if the child is in the filter, it belongs to
                            // this aggregate and needs to be removed
                            if (getAclManagement().isACLNode(child)) {
                                if (acHandling == AccessControlHandling.OVERWRITE
                                        || acHandling == AccessControlHandling.CLEAR) {
                                    info.onDeleted(path);
                                    getAclManagement().clearACL(node);
                                }
                            } else {
                                info.onDeleted(path);
                                child.remove();
                            }
                        } else if (acHandling == AccessControlHandling.CLEAR
                                && getAclManagement().isACLNode(child)) {
                            info.onDeleted(path);
                            getAclManagement().clearACL(node);
                        }
                    }
                }
            }

        }
        return info;
    }

}