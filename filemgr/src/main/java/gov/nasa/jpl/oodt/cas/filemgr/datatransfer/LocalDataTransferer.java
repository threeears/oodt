/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.nasa.jpl.oodt.cas.filemgr.datatransfer;

//APACHE Imports
import org.apache.commons.io.FileUtils;

//OODT imports
import gov.nasa.jpl.oodt.cas.filemgr.structs.Product;
import gov.nasa.jpl.oodt.cas.filemgr.structs.Reference;
import gov.nasa.jpl.oodt.cas.filemgr.structs.exceptions.DataTransferException;
import gov.nasa.jpl.oodt.cas.filemgr.structs.exceptions.ConnectionException;
import gov.nasa.jpl.oodt.cas.filemgr.structs.mime.MimeTypes;
import gov.nasa.jpl.oodt.cas.filemgr.versioning.VersioningUtils;
import gov.nasa.jpl.oodt.cas.filemgr.system.XmlRpcFileManagerClient;

//JDK imports
import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URISyntaxException;

/**
 * @author mattmann
 * @author bfoster
 * @version $Revision$
 * 
 * <p>
 * An implementation of the {@link DataTransfer} interface that moves products
 * that are available via URIs on the same machine, through an NFS mounted disk,
 * or via the locally mounted file repository.
 * </p>
 * 
 */
public class LocalDataTransferer implements DataTransfer {

    /* our log stream */
    private static Logger LOG = Logger.getLogger(LocalDataTransferer.class
            .getName());

    /* file manager client */
    private XmlRpcFileManagerClient client = null;

    /**
     * <p>
     * Default Constructor
     * </p>
     */
    public LocalDataTransferer() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.nasa.jpl.oodt.cas.filemgr.datatransfer.DataTransfer#setFileManagerUrl(java.net.URL)
     */
    public void setFileManagerUrl(URL url) {
        try {
            client = new XmlRpcFileManagerClient(url);
            LOG.log(Level.INFO, "Local Data Transfer to: ["
                    + client.getFileManagerUrl().toString() + "] enabled");
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.nasa.jpl.oodt.cas.datatransfer.DataTransfer#transferProduct(gov.nasa.jpl.oodt.cas.data.structs.Product)
     */
    public void transferProduct(Product product) throws DataTransferException,
            IOException {
        // check whether or not it's a set of files, or it's actually a dir
        // structure
        if (product.getProductStructure()
                .equals(Product.STRUCTURE_HIERARCHICAL)) {
            try {
                moveDirToProductRepo(product);
            } catch (URISyntaxException e) {
                LOG.log(Level.WARNING, "URI Syntax Exception when moving dir "
                        + ((Reference) product.getProductReferences().get(0))
                                .getOrigReference() + ": Message: "
                        + e.getMessage());
                throw new DataTransferException(e);
            }
        } else if (product.getProductStructure().equals(Product.STRUCTURE_FLAT)) {
            try {
                moveFilesToProductRepo(product);
            } catch (URISyntaxException e) {
                LOG.log(Level.WARNING,
                        "URI Syntax Exception when moving files: Message: "
                                + e.getMessage());
                throw new DataTransferException(e);
            }
        } else {
            throw new DataTransferException(
                    "Cannot transfer product on unknown ProductStructure: "
                            + product.getProductStructure());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws DataTransferException,
            IOException, URISyntaxException {
        String usage = "LocalFileTransfer --productName <name> --productRepo <repo> [--dir <dirRef>] [--files <origRef 1>...<origRef N>]\n";

        MimeTypes mimeTypeRepo = MimeTypes
                .buildRepository(System
                        .getProperty("gov.nasa.jpl.oodt.cas.filemgr.mime.type.repository"));

        String productName = null;
        String productRepo = null;
        String transferType = null;
        Reference dirReference = null;

        List<Reference> fileReferences = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--dir")) {
                transferType = "dir";
                dirReference = new Reference();
                dirReference.setOrigReference(new File(new URI(args[++i]))
                        .toURI().toString());
                LOG.log(Level.FINER,
                        "LocalFileTransfer.main: Generated orig reference: "
                                + dirReference.getOrigReference());
            } else if (args[i].equals("--files")) {
                transferType = "files";
                fileReferences = new Vector<Reference>();
                for (int j = i + 1; j < args.length; j++) {
                    LOG.log(Level.FINER,
                            "LocalFileTransfer.main: Adding file ref: "
                                    + args[j]);
                    fileReferences.add(new Reference(args[j], null, new File(
                            args[j]).length(), mimeTypeRepo
                            .getMimeType(args[j])));
                }
            } else if (args[i].equals("--productName")) {
                productName = args[++i];
            } else if (args[i].equals("--productRepo")) {
                productRepo = args[++i];
            }
        }

        if (transferType == null
                || (transferType != null && ((transferType.equals("dir") && dirReference == null)
                        || (transferType.equals("files") && fileReferences == null)
                        || (transferType != null && !(transferType
                                .equals("dir") || transferType.equals("files")))
                        || productName == null || productRepo == null))) {
            System.err.println(usage);
            System.exit(1);
        }

        // construct a new Product
        Product p = new Product();
        p.setProductName(productName);

        if (transferType.equals("dir")) {
            p.setProductStructure(Product.STRUCTURE_HIERARCHICAL);
            dirReference.setDataStoreReference(new File(new URI(productRepo))
                    .toURL().toExternalForm()
                    + URLEncoder.encode(p.getProductName(), "UTF-8") + "/");
            p.getProductReferences().add(dirReference);
            /* we'll do a simple versioning scheme ourselves: no versioning! */
            p.getProductReferences().addAll(
                    VersioningUtils.getReferencesFromDir(new File(new URI(
                            dirReference.getOrigReference()))));
            VersioningUtils.createBasicDataStoreRefsHierarchical(p
                    .getProductReferences());
        } else if (transferType.equals("files")) {
            p.setProductStructure("Flat");
            p.getProductReferences().addAll(fileReferences);
            VersioningUtils.createBasicDataStoreRefsFlat(productName,
                    productRepo, p.getProductReferences());
        }

        DataTransfer transfer = new LocalDataTransferer();
        transfer.transferProduct(p);

    }

    private void moveDirToProductRepo(Product product) throws IOException,
            URISyntaxException {
        Reference dirRef = (Reference) product.getProductReferences().get(0);
        LOG.log(Level.INFO, "LocalDataTransferer: Moving Directory: "
                + dirRef.getOrigReference() + " to "
                + dirRef.getDataStoreReference());

        // notify the file manager that we started
        quietNotifyTransferProduct(product);

        for (Iterator<Reference> i = product.getProductReferences().iterator(); i
                .hasNext();) {
            Reference r = i.next();
            File fileRef = new File(new URI(r.getOrigReference()));

            if (fileRef.isFile()) {
                moveFile(r, false);
            } else if (fileRef.isDirectory()
                    && (fileRef.list() != null && fileRef.list().length == 0)) {
                // if it's a directory and it doesn't exist yet, we should
                // create it
                // just in case there's no files in it
                if (!new File(new URI(r.getDataStoreReference())).exists()) {
                    LOG.log(Level.FINER, "Directory: ["
                            + r.getDataStoreReference()
                            + "] doesn't exist: creating it");
                    if (!new File(new URI(r.getDataStoreReference())).mkdirs()) {
                        LOG.log(Level.WARNING, "Unable to create directory: ["
                                + r.getDataStoreReference()
                                + "] in local data transferer");
                    }
                }
            }
        }

        // notify the file manager that we're done
        quietNotifyProductTransferComplete(product);

    }

    private void moveFilesToProductRepo(Product product) throws IOException,
            URISyntaxException {
        List<Reference> refs = product.getProductReferences();

        // notify the file manager that we started
        quietNotifyTransferProduct(product);

        for (Iterator<Reference> i = refs.iterator(); i.hasNext();) {
            Reference r = (Reference) i.next();
            moveFile(r, true);
        }

        // notify the file manager that we're done
        quietNotifyProductTransferComplete(product);
    }

    private void moveFile(Reference r, boolean log) throws IOException,
            URISyntaxException {
        if (log) {
            LOG
                    .log(Level.INFO, "LocalDataTransfer: Moving File: "
                            + r.getOrigReference() + " to "
                            + r.getDataStoreReference());
        }
        File srcFileRef = new File(new URI(r.getOrigReference()));
        File destFileRef = new File(new URI(r.getDataStoreReference()));

        FileUtils.copyFile(srcFileRef, destFileRef);
    }

    private void quietNotifyTransferProduct(Product p) {
        if (client == null) {
            LOG
                    .log(Level.WARNING,
                            "File Manager service not defined: this transfer will not be tracked");
            return;
        }

        try {
            client.transferringProduct(p);
        } catch (DataTransferException e) {
            e.printStackTrace();
            LOG.log(Level.WARNING,
                    "Error notifying file manager of product transfer initiation for product: ["
                            + p.getProductId() + "]: Message: "
                            + e.getMessage());
            return;
        }
    }

    private void quietNotifyProductTransferComplete(Product p) {
        if (client == null) {
            LOG
                    .log(Level.WARNING,
                            "File Manager service not defined: this transfer will not be tracked");
            return;
        }

        try {
            client.removeProductTransferStatus(p);
        } catch (DataTransferException e) {
            e.printStackTrace();
            LOG.log(Level.WARNING,
                    "Error notifying file manager of product transfer completion for product: ["
                            + p.getProductId() + "]: Message: "
                            + e.getMessage());
            return;
        }
    }

}