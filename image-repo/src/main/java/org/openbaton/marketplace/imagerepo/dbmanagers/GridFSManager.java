package org.openbaton.marketplace.imagerepo.dbmanagers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;

import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.catalogue.VNFPackageMetadata;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.NotFoundException;
import org.openbaton.marketplace.imagerepo.interfaces.DatabaseManager;
import org.openbaton.marketplace.repository.repository.ImageMetadataRepository;
import org.openbaton.marketplace.repository.repository.VNFPackageMetadataRepository;
import org.openbaton.nfvo.security.interfaces.UserManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lto on 03/08/16.
 */
@Service
public class GridFSManager implements DatabaseManager {

  @Autowired private GridFsTemplate gridFsTemplate;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  @Autowired private ImageMetadataRepository imageMetadataRepository;
  @Autowired private UserManagement userManagement;
  @Autowired private VNFPackageMetadataRepository vnfPackageMetadataRepository;

  @Override
  public String persistObject(InputStream file, String contentType, String filename) throws
                                                                                     NoSuchAlgorithmException,
                                                                                     IOException {
//    DigestInputStream md5Stream = new DigestInputStream(file, MessageDigest.getInstance("MD5"));
//    file = new BufferedInputStream(file);
//    List<GridFSDBFile> files = gridFsTemplate.find(new Query());
//    for (GridFSDBFile f : files) {
//      log.debug("MongoMD5 = " + f.getMD5());
//      file.mark(Integer.MAX_VALUE);
//      String md5 = DigestUtils.md5Hex(file);
//      log.debug(" FileMD5 = " + md5);
//      file.reset();
//      if (f.getMD5().equals(md5)) {
//        return f.getId().toString();
//      }
//    }
    DBObject metadata = new BasicDBObject();
    metadata.put("username",userManagement.getCurrentUser());
    return gridFsTemplate.store(file, filename, contentType, metadata).getId().toString();
  }

  @Override
  public ImageMetadata getImageMetadata(String id) throws NotFoundException {
    GridFSDBFile gridFSDBFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
    for (ImageMetadata imageMetadata : imageMetadataRepository.findAll()) {
      if (imageMetadata.getImageRepoId().equals(gridFSDBFile.getId())) {
        return imageMetadata;
      }
    }
    throw new NotFoundException();
  }

  @Override
  public InputStream getObject(String id) throws NotFoundException {
    return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id))).getInputStream();
  }

  @Override
  public List<ImageMetadata> findAllImagesWithFile() {
    List<ImageMetadata> imageMetadatas = new ArrayList<>();
    List<GridFSDBFile> gridFSDBFiles = gridFsTemplate.find(new Query());
    Collection<ImageMetadata> metadatas = imageMetadataRepository.findByUsername(userManagement.getCurrentUser());
    if (!metadatas.isEmpty()) {
      for (ImageMetadata imageMetadata : metadatas) {
        log.debug("Checking for image: " + imageMetadata.getNames());
        for (GridFSDBFile file : gridFSDBFiles) {
          log.debug("\tFile name: " + file.getFilename() + " (" + imageMetadata.getImageRepoId() + " == " + file.getId() + ")");
          if (imageMetadata.getImageRepoId() != null) {
            if (imageMetadata.getImageRepoId().equals(file.getId().toString()) || file.getMetaData().get("username").equals(userManagement.getCurrentUser())) {
              imageMetadatas.add(imageMetadata);
            }
          }
        }
      }
    }
    return imageMetadatas;
  }

  @Override
  public void remove(String id) throws NotAuthorizedException {

    String currentUser = userManagement.getCurrentUser();
    log.debug("Current username: " + currentUser);
    List<ImageMetadata> allImagesWithFile = findAllImagesWithFile();
    log.debug("All images per user are: " + allImagesWithFile.size());
    for (ImageMetadata imageMetadata : allImagesWithFile) {
      log.debug("checking image: " + imageMetadata);
      for (VNFPackageMetadata metadata : vnfPackageMetadataRepository.findAll()) {
        if (metadata.getImageMetadata().getId().equals(id)) {
          throw new NotAuthorizedException("Not able to remove image because another VNFPackage is referring it");
        }
      }
      if (imageMetadata.getId().equals(id)) {
        log.debug("Removing image with mongodb id: " + imageMetadata.getImageRepoId());
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(imageMetadata.getImageRepoId())));
        return;
      }
    }
  }

  @Override
  public void forceRemove(String id) {
    log.debug("Forcing remove of image");
    String currentUser = userManagement.getCurrentUser();
    log.debug("Current username: " + currentUser);
    for (ImageMetadata imageMetadata : imageMetadataRepository.findByUsername(currentUser)) {
      if (imageMetadata.getId().equals(id)) {
        log.debug("Removing image with mongodb id: " + imageMetadata.getImageRepoId());
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(imageMetadata.getImageRepoId())));
        return;
      }
    }
  }
}
