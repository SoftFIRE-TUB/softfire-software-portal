package org.openbaton.marketplace.imagerepo.core;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Module;

import org.jclouds.ContextBuilder;
import org.jclouds.io.Payload;
import org.jclouds.io.payloads.BaseMutableContentMetadata;
import org.jclouds.io.payloads.InputStreamPayload;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.glance.v1_0.GlanceApi;
import org.jclouds.openstack.glance.v1_0.domain.ContainerFormat;
import org.jclouds.openstack.glance.v1_0.domain.DiskFormat;
import org.jclouds.openstack.glance.v1_0.domain.ImageDetails;
import org.jclouds.openstack.glance.v1_0.features.ImageApi;
import org.jclouds.openstack.glance.v1_0.options.CreateImageOptions;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.exceptions.ImageRepositoryNotEnabled;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.NotFoundException;
import org.openbaton.marketplace.exceptions.NumberOfImageExceededException;
import org.openbaton.marketplace.imagerepo.interfaces.DatabaseManager;
import org.openbaton.marketplace.repository.repository.ImageMetadataRepository;
import org.openbaton.nfvo.security.interfaces.UserManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

/**
 * Created by lto on 03/08/16.
 */
@Service
public class ImageManager {

  @Value("${marketplace.server.ip:localhost}") private String marketplaceIp;
  @Value("${server.port:8082}") private int marketplacePort;
  @Autowired(required = false) private DatabaseManager dbManager;
  @Value("${marketplace.image.number.max:3}") private int maxImagePerUser;
  @Autowired private UserManagement userManagement;
  @Autowired private ImageMetadataRepository imageMetadataRepository;
  @Value("${marketplace.vim.path:./vims}") private String vimInstancePath;

  public String persistImage(InputStream file, String imageName, String contentType) throws
                                                                                     ImageRepositoryNotEnabled,
                                                                                     NoSuchAlgorithmException,
                                                                                     NumberOfImageExceededException,
                                                                                     IOException {
    if (dbManager.findAllImagesWithFile().size() >= maxImagePerUser) {
      throw new NumberOfImageExceededException("Sorry you are allowed to upload only " +
                                               maxImagePerUser +
                                               " images :(");
    }
    if (dbManager == null) {
      throw new ImageRepositoryNotEnabled("ImageRepository is not enabled");
    }
    return dbManager.persistObject(file, contentType, imageName);
  }

  public InputStream getImage(String id) throws NotFoundException {
    return dbManager.getObject(id);
  }

  public ImageMetadata createImage(String name,
                                   long size,
                                   InputStream stream,
                                   String contentType,
                                   long minDisk,
                                   long minRam) throws
                                                ImageRepositoryNotEnabled,
                                                NoSuchAlgorithmException,
                                                NumberOfImageExceededException,
                                                IOException {
    ImageMetadata imageMetadata = new ImageMetadata();
    //    String imageRepoId = persistImage(stream, name, contentType);
    String currentUsername = userManagement.getCurrentUser();
    ArrayList<String> names = new ArrayList<>();
    names.add(currentUsername + "_" + name);
    imageMetadata.setNames(names);
    imageMetadata.setUpload("check");
    imageMetadata.setExtIds(new HashMap<String, String>());
    imageMetadata.setUsername(currentUsername);
    imageMetadataRepository.save(imageMetadata);
    for (VimInstance vimInstance : loadVimInstances()) {
      uploadImage(stream,
                  vimInstance.getAuthUrl(),
                  vimInstance.getTenant(),
                  vimInstance.getUsername(),
                  vimInstance.getPassword(),
                  size,
                  minDisk,
                  minRam,
                  false,
                  "qcow2",
                  "bare",
                  currentUsername + "_" + name,
                  imageMetadata,
                  vimInstance.getName());
    }

    return imageMetadata;
  }

  private List<VimInstance> loadVimInstances() throws FileNotFoundException {
    List<VimInstance> result = new ArrayList<>();
    File dir = new File(vimInstancePath);
    if (dir.isDirectory()) {
      for (File f : dir.listFiles()) {
        if (f.isFile() && f.getName().endsWith(".json")) {
          result.add(gson.fromJson(new FileReader(f), VimInstance.class));
        }
      }
    }
    return result;
  }

  public Iterable<ImageMetadata> listImages() {
    //    return dbManager.findAllImagesWithFile();
    return imageMetadataRepository.findByUsername(userManagement.getCurrentUser());
  }

  public void deleteImage(String id) throws NotAuthorizedException, FileNotFoundException {

    deleteImageOpenstack(imageMetadataRepository.findFirstById(id));
//    dbManager.remove(id);
    imageMetadataRepository.delete(id);
  }

  private void deleteImageOpenstack(ImageMetadata imageMetadata) throws FileNotFoundException {
    for (VimInstance vimInstance : loadVimInstances()) {
      deleteImageSingleOpenstack(vimInstance, imageMetadata.getExtIds().get(vimInstance.getName()));
    }
  }

  @Async
  public Future<Void> deleteImageSingleOpenstack(VimInstance vimInstance, String extId) {
    try {
      GlanceApi
          glanceApi =
          ContextBuilder.newBuilder("openstack-glance")
                        .endpoint(vimInstance.getAuthUrl())
                        .credentials(vimInstance.getTenant() + ":" + vimInstance.getUsername(),
                                     vimInstance.getPassword())
                        .modules(modules)
                        .overrides(overrides)
                        .buildApi(GlanceApi.class);
      ImageApi
          imageApi =
          glanceApi.getImageApi(getZone(vimInstance.getAuthUrl(),
                                        vimInstance.getTenant(),
                                        vimInstance.getUsername(),
                                        vimInstance.getPassword()));
      imageApi.delete(extId);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return new AsyncResult<>(null);
  }

  public void forceDeleteImage(String id) {
    dbManager.forceRemove(id);
  }

  @Async
  public Future<Void> uploadImage(InputStream stream,
                                  String authUrl,
                                  String tenant,
                                  String username,
                                  String password,
                                  long size,
                                  long minDisk,
                                  long minRam,
                                  boolean isPublic,
                                  String diskFormat,
                                  String containerFormat,
                                  String name,
                                  ImageMetadata imageMetadata,
                                  String vimInstanceName) {
    try {
      GlanceApi
          glanceApi =
          ContextBuilder.newBuilder("openstack-glance")
                        .endpoint(authUrl)
                        .credentials(tenant + ":" + username, password)
                        .modules(modules)
                        .overrides(overrides)
                        .buildApi(GlanceApi.class);
      ImageApi imageApi = glanceApi.getImageApi(getZone(authUrl, tenant, username, password));
      CreateImageOptions createImageOptions = new CreateImageOptions();
      createImageOptions.minDisk(minDisk);
      createImageOptions.minRam(minRam);
      createImageOptions.isPublic(isPublic);
      createImageOptions.diskFormat(DiskFormat.valueOf(diskFormat.toUpperCase()));
      createImageOptions.containerFormat(ContainerFormat.valueOf(containerFormat.toUpperCase()));
      log.debug("Initialized jclouds Image: " + createImageOptions);
      Payload jcloudsPayload = new InputStreamPayload(stream);
      BaseMutableContentMetadata contentMetadata = new BaseMutableContentMetadata();
      contentMetadata.setContentLength(size);
      jcloudsPayload.setContentMetadata(contentMetadata);
      //      try {
      //        ByteArrayOutputStream bufferedPayload = new ByteArrayOutputStream();
      //        int read = 0;
      //        byte[] bytes = new byte[1024];
      //        while ((read = payload.read(bytes)) != -1) {
      //          bufferedPayload.write(bytes, 0, read);
      //        }
      //        bufferedPayload.flush();
      //        jcloudsPayload = new ByteArrayPayload(bufferedPayload.toByteArray());
      //      } catch (IOException e) {
      //        log.error(e.getMessage(), e);
      //      }
      ImageDetails imageDetails = imageApi.create(name, jcloudsPayload, new CreateImageOptions[]{createImageOptions});
      imageMetadata.getExtIds().put(vimInstanceName,imageDetails.getId());
      imageMetadataRepository.save(imageMetadata);
      log.debug("Added jclouds Image: " + imageDetails + " to VimInstance: " + authUrl);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return new AsyncResult<>(null);
  }

  private static final Pattern
      PATTERN =
      Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
  Iterable<Module> modules;
  Properties overrides;
  private static Logger log = LoggerFactory.getLogger(ImageManager.class);
  private static Lock lock;
  private Gson gson = new GsonBuilder().create();

  @PostConstruct
  private void init() {
    modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
    overrides = new Properties();
    overrides.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);
  }

  private String getZone(String authUrl, String tenant, String username, String password) {
    NovaApi
        novaApi =
        ContextBuilder.newBuilder("openstack-nova")
                      .endpoint(authUrl)
                      .credentials(tenant + ":" + username, password)
                      .modules(modules)
                      .overrides(overrides)
                      .buildApi(NovaApi.class);
    Set<String> zones = novaApi.getConfiguredRegions();
    log.debug("Available openstack environment zones: " + zones);
    String zone = null;

    if (zone == null) {
      for (String zn : zones) {
        if (zn.contains("nova")) {
          return zn;
        }
      }
      log.debug("Selecting a random Location of openstack environment from: " + zones);
      zone = zones.iterator().next();
      log.debug("Selected Location of openstack environment: '" + zone + "'");
    }
    return zone;
  }
}
