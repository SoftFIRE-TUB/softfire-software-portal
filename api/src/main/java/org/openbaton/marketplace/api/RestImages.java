package org.openbaton.marketplace.api;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimException;
import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.exceptions.ImageRepositoryNotEnabled;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.NumberOfImageExceededException;
import org.openbaton.marketplace.imagerepo.core.ImageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Created by lto on 04/10/16.
 */
@RestController
@RequestMapping("/api/v1/images")
public class RestImages {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private ImageManager imageManager;

  @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ImageMetadata onboard(@RequestParam("image") MultipartFile image,
                               @RequestHeader(value = "min-ram", defaultValue = "0") int minRam,
                               @RequestHeader(value = "min-disk", defaultValue = "0") int minDisk) throws
                                                                                                   IOException,
                                                                                                   NoSuchAlgorithmException,
                                                                                                   ImageRepositoryNotEnabled,
                                                                                                   NumberOfImageExceededException,
                                                                                                   InterruptedException {
    return imageManager.createImage(image.getOriginalFilename(),
                                    image.getSize(),
                                    image.getInputStream(),
                                    image.getContentType(),
                                    minDisk,
                                    minRam);
  }

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Iterable<ImageMetadata> listImages() throws
                                              IOException,
                                              VimException,
                                              NotFoundException,
                                              SQLException,
                                              PluginException,
                                              ImageRepositoryNotEnabled {
    return imageManager.listImages();
  }

  @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public void deleteImage(@PathVariable("id") String id) throws
                                                         NotFoundException,
                                                         org.openbaton.marketplace.exceptions.NotFoundException,
                                                         NotAuthorizedException, IOException, InterruptedException {
    log.debug("Incoming request for deleting image with id: " + id);
    imageManager.deleteImage(id);
  }

  @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<InputStreamResource> downloadImage(@PathVariable("id") String id) throws
                                                                                          NotFoundException,
                                                                                          org.openbaton.marketplace
                                                                                              .exceptions
                                                                                              .NotFoundException {
    log.trace("Incoming request for getting Image: " + id);
    return ResponseEntity.ok()
                         .contentType(MediaType.parseMediaType("application/octet-stream"))
                         .body(new InputStreamResource(imageManager.getImage(id)));
  }
}
