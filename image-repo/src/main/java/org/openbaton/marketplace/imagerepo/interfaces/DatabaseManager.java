package org.openbaton.marketplace.imagerepo.interfaces;

import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by lto on 03/08/16.
 */
public interface DatabaseManager {

  String persistObject(InputStream file, String contentType, String vnfPackageId) throws
                                                                                  NoSuchAlgorithmException,
                                                                                  IOException;

  org.openbaton.marketplace.catalogue.ImageMetadata getImageMetadata(String id) throws NotFoundException;

  InputStream getObject(String id) throws NotFoundException;

  List<ImageMetadata> findAllImagesWithFile();

  void remove(String id) throws NotAuthorizedException;

  void forceRemove(String id);
}
