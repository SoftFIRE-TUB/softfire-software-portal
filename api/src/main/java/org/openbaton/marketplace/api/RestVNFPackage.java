/*
 * #
 * # Copyright (c) 2015 Fraunhofer FOKUS
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #     http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 * #
 *
 */

/*
 */

package org.openbaton.marketplace.api;

import org.apache.commons.compress.archivers.ArchiveException;
import org.openbaton.exceptions.AlreadyExistingException;
import org.openbaton.exceptions.BadRequestException;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimException;
import org.openbaton.marketplace.catalogue.VNFPackageMetadata;
import org.openbaton.marketplace.core.VNFPackageManagement;
import org.openbaton.marketplace.exceptions.ImageRepositoryNotEnabled;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.NumberOfImageExceededException;
import org.openbaton.marketplace.exceptions.PackageIntegrityException;
import org.openbaton.marketplace.imagerepo.core.ImageManager;
import org.openbaton.marketplace.repository.repository.VNFPackageMetadataRepository;
import org.openbaton.sdk.api.exception.SDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Set;

/**
 * Created by mpa on 17.05.16.
 */
@RestController
@RequestMapping("/api/v1/vnf-packages")
public class RestVNFPackage {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private VNFPackageManagement vnfPackageManagement;
  @Autowired private ImageManager imageManager;
  @Autowired private VNFPackageMetadataRepository vnfPackageMetadataRepository;

  /**
   * Adds a new VNFPackage to the marketplace
   */
  @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity uploadPackage(@RequestParam("file") MultipartFile vnfPackage) throws
                                                                                      IOException,
                                                                                      SDKException,
                                                                                      NoSuchAlgorithmException,
                                                                                      ArchiveException,
                                                                                      NumberOfImageExceededException,
                                                                                      SQLException,
                                                                                      PluginException,
                                                                                      VimException,
                                                                                      NotFoundException,
                                                                                      ImageRepositoryNotEnabled,
                                                                                      BadRequestException,
                                                                                      AlreadyExistingException,
                                                                                      PackageIntegrityException {
    log.debug("uploading Package....");

    if ((vnfPackage != null)) {
      return new ResponseEntity<>(onboard(vnfPackage), HttpStatus.OK);
    }

    throw new BadRequestException("VNFPackage is missing");
  }

  private VNFPackageMetadata onboard(MultipartFile vnfPackage) throws

                                                               IOException,
                                                               VimException,
                                                               NotFoundException,
                                                               SQLException,
                                                               PluginException,
                                                               ImageRepositoryNotEnabled,
                                                               NoSuchAlgorithmException,
                                                               ArchiveException,
                                                               SDKException,
                                                               NumberOfImageExceededException,
                                                               AlreadyExistingException, PackageIntegrityException {

    log.debug("onboard....");
    if (!vnfPackage.isEmpty()) {
      byte[] bytes = vnfPackage.getBytes();
      String fileName = vnfPackage.getOriginalFilename();

      VNFPackageMetadata vnfPackageMetadata = vnfPackageManagement.add(fileName, bytes, false);
      vnfPackageManagement.dispatch(vnfPackageMetadata);
      return vnfPackageMetadata;
    } else {
      throw new IOException("File is an empty file!");
    }
  }

  /**
   * Deletes an VNFPackage from the marketplace
   *
   * @param id
   */
  @RequestMapping(value = "{id}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable("id") String id) throws NotFoundException, NotAuthorizedException {
    log.debug("Incoming request for deleting VNFPackage: " + id);
    vnfPackageManagement.delete(id);
    log.trace("Incoming request served by deleting VNFPackage: " + id);
  }

  /**
   * Returns an VNFPackage with the given ID from the marketplace
   *
   * @param id
   * @return VNFPackage
   */
  @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public VNFPackageMetadata get(@PathVariable("id") String id) throws NotFoundException {
    log.trace("Incoming request for getting VNFPackage: " + id);
    VNFPackageMetadata vnfPackageMetadata = vnfPackageManagement.get(id);
    vnfPackageMetadata.setVnfPackageFile(null);
    log.trace("Incoming request served by returning VNFPackage: " + id);
    return vnfPackageMetadata;
  }

  /**
   * Returns an VNFPackage with the given ID from the marketplace
   *
   * @param id
   * @return VNFPackage
   */
  @RequestMapping(value = "{id}/download", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity download(@PathVariable("id") String id) throws
                                                                NotFoundException,
                                                                IOException,
                                                                ArchiveException {
    log.trace("Incoming request for getting VNFPackage: " + id);
    ByteArrayOutputStream tar = vnfPackageManagement.download(id);
    log.trace("Incoming request served by returning VNFPackage: " + id);

    VNFPackageMetadata vnfPackageMetadata = vnfPackageManagement.get(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("Content-Disposition", "attachment; filename=" + vnfPackageMetadata.getVnfPackageFileName());
    ResponseEntity responseEntity = new ResponseEntity(tar.toByteArray(), headers, HttpStatus.OK);

    return responseEntity;
  }

  @RequestMapping(value = "{id}/download-with-link", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity downloadWithLink(@PathVariable("id") String id) throws
                                                                        NotFoundException,
                                                                        IOException,
                                                                        ArchiveException {
    log.trace("Incoming request for getting VNFPackage: " + id);
    ByteArrayOutputStream tar = vnfPackageManagement.compose(id);
    log.trace("Incoming request served by returning VNFPackage: " + id);

    VNFPackageMetadata vnfPackageMetadata = vnfPackageManagement.get(id);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("Content-Disposition", "attachment; filename=" + vnfPackageMetadata.getVnfPackageFileName());
    ResponseEntity responseEntity = new ResponseEntity(tar.toByteArray(), headers, HttpStatus.OK);

    return responseEntity;
  }

  /**
   * Lists all Applications of the marketplace
   *
   * @return List<VNFPackage>
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public Set<VNFPackageMetadata> get() {
    log.trace("Incoming request for listing VNFPackagesMetadata...");
    Set<VNFPackageMetadata> vnfPackagesMetadata = vnfPackageManagement.get();
    for (VNFPackageMetadata vnfPackageMetadata : vnfPackagesMetadata) {
      vnfPackageMetadata.setVnfPackageFile(null);
    }
    log.trace("Incoming request served by listing all VNFPackagesMetadata: " + vnfPackagesMetadata);
    return vnfPackagesMetadata;
  }

  //    /**
  //     * Updates the VNFPackage with the given ID of the marketplace
  //     *
  //     * @param id
  //     * @param vnfPackage
  //     * @return VNFPackage
  //     */
  //    @RequestMapping(value = "{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
  // produces = MediaType.APPLICATION_JSON_VALUE)
  //    @ResponseStatus(HttpStatus.OK)
  //    public VNFPackage update(@PathVariable("id") String id, @RequestBody @Valid VNFPackage vnfPackage) throws
  // NotFoundException {
  //        log.trace("Incoming request for updating VNFPackage: " + id + " with: " + vnfPackage);
  //        vnfPackage = vnfPackageManagement.update(id, vnfPackage);
  //        log.trace("Incoming request served by updating VNFPackage: " + vnfPackage);
  //        return vnfPackage;
  //
  //    }
}
