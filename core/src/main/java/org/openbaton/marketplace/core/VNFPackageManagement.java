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

package org.openbaton.marketplace.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.net.URL;
import org.codehaus.jackson.map.ObjectMapper;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.nfvo.NFVImage;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.catalogue.nfvo.VNFPackage;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.catalogue.security.Project;
import org.openbaton.exceptions.AlreadyExistingException;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimException;
import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.catalogue.VNFPackageMetadata;
import org.openbaton.marketplace.exceptions.FailedToUploadException;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.marketplace.exceptions.PackageIntegrityException;
import org.openbaton.marketplace.imagerepo.core.ImageManager;
import org.openbaton.marketplace.repository.repository.VNFPackageMetadataRepository;
import org.openbaton.marketplace.repository.repository.VNFPackageRepository;
import org.openbaton.nfvo.security.interfaces.UserManagement;
import org.openbaton.sdk.NFVORequestor;
import org.openbaton.sdk.api.exception.SDKException;
import org.openbaton.sdk.api.rest.ProjectAgent;
import org.openbaton.sdk.api.rest.VimInstanceRestAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.YamlJsonParser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

/**
 * Created by mpa on 23.05.16.
 */
@Service
@Scope
public class VNFPackageManagement {

  protected Logger log = LoggerFactory.getLogger(this.getClass());

  private Gson mapper = new GsonBuilder().create();

  @Autowired private VNFPackageRepository vnfPackageRepository;

  @Autowired private VNFPackageMetadataRepository vnfPackageMetadataRepository;
  @Autowired private UserManagement userManagement;
  @Value("${marketplace.nfvo.list.file.location:}") private String nfvoListFile;
  @Value("${marketplace.fiteagle.ip:localhost}") private String fitEagleIp;
  @Value("${marketplace.fiteagle.port:8080}") private String fitEaglePort;
  @Autowired private ImageManager imageManager;
  @Value("${marketplace.openbaton.username:admin}") private String obUsername;
  @Value("${marketplace.openbaton.password:openbaton}") private String obPassword;
  @Value("${marketplace.openbaton.nfvo.ip:localhost}") private String obNfvoIp;
  @Value("${marketplace.openbaton.nfvo.port:8080}") private String obNfvoPort;
  @Value("${marketplace.openbaton.nfvo.ssl:false}") private boolean obSslEnabled;

  public VNFPackageMetadata add(String fileName, byte[] pack, boolean imageLink) throws
                                                                                 IOException,
                                                                                 VimException,
                                                                                 NotFoundException,
                                                                                 SQLException,
                                                                                 PluginException,
                                                                                 AlreadyExistingException,
                                                                                 PackageIntegrityException,
                                                                                 FailedToUploadException {
    try {
      updateVims();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SDKException e) {
      e.printStackTrace();
    }

    VNFPackage vnfPackage = new VNFPackage();
    vnfPackage.setScripts(new HashSet<Script>());
    Map<String, Object> metadata = null;
    VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor = null;
    byte[] imageFile = null;
    NFVImage image = new NFVImage();
    ImageMetadata imageMetadata = new ImageMetadata();

    InputStream tarStream;
    ArchiveInputStream myTarFile;
    try {
      tarStream = new ByteArrayInputStream(pack);
      myTarFile = new ArchiveStreamFactory().createArchiveInputStream("tar", tarStream);
    } catch (ArchiveException e) {
      e.printStackTrace();
      throw new IOException();
    }
    TarArchiveEntry entry;
    Map<String, Object> imageDetails = new HashMap<>();
    while ((entry = (TarArchiveEntry) myTarFile.getNextEntry()) != null) {
            /* Get the name of the file */
      if (entry.isFile() && !entry.getName().startsWith("./._")) {
        log.debug("file inside tar: " + entry.getName());
        byte[] content = new byte[(int) entry.getSize()];
        myTarFile.read(content, 0, content.length);
        if (entry.getName().equals("Metadata.yaml")) {
          YamlJsonParser yaml = new YamlJsonParser();
          log.info(new String(content));
          metadata = yaml.parseMap(new String(content));
          //Get configuration for NFVImage
          String[] REQUIRED_PACKAGE_KEYS = new String[]{"name", "description", "provider", "image", "shared"};
          for (String requiredKey : REQUIRED_PACKAGE_KEYS) {
            if (!metadata.containsKey(requiredKey)) {
              throw new PackageIntegrityException("Not found " + requiredKey + " of VNFPackage in Metadata.yaml");
            }
            if (metadata.get(requiredKey) == null) {
              throw new PackageIntegrityException("Not defined " + requiredKey + " of VNFPackage in Metadata.yaml");
            }
          }
          vnfPackage.setName((String) metadata.get("name"));

          if (vnfPackageMetadataRepository.findByNameAndUsername(vnfPackage.getName(), userManagement.getCurrentUser())
                                          .size() != 0) {
            throw new AlreadyExistingException("Package with name " +
                                               vnfPackage.getName() +
                                               " already exists, please " +
                                               "change the name");
          }

          if (metadata.containsKey("scripts-link")) {
            vnfPackage.setScriptsLink((String) metadata.get("scripts-link"));
          }
          if (metadata.containsKey("image")) {
            imageDetails = (Map<String, Object>) metadata.get("image");
            String[] REQUIRED_IMAGE_DETAILS = new String[]{"upload"};
            log.debug("image: " + imageDetails);
            for (String requiredKey : REQUIRED_IMAGE_DETAILS) {
              if (!imageDetails.containsKey(requiredKey)) {
                throw new PackageIntegrityException("Not found key: " + requiredKey + " of image in Metadata.yaml");
              }
              if (imageDetails.get(requiredKey) == null) {
                throw new PackageIntegrityException("Not defined value of key: " +
                                                    requiredKey +
                                                    " of image in Metadata.yaml");
              }
            }
            imageMetadata.setUsername(userManagement.getCurrentUser());
            imageMetadata.setUpload((String) imageDetails.get("option"));
            if (imageDetails.containsKey("ids")) {
              imageMetadata.setIds((List<String>) imageDetails.get("ids"));
            } else {
              imageMetadata.setIds(new ArrayList<String>());
            }
            if (imageDetails.containsKey("names")) {
              imageMetadata.setNames((List<String>) imageDetails.get("names"));
            } else {
              imageMetadata.setNames(new ArrayList<String>());
            }
            if (imageDetails.containsKey("link")) {
              imageMetadata.setLink((String) imageDetails.get("link"));
            } else {
              imageMetadata.setLink(null);
            }

            //If upload==true -> create a new Image
            if (imageDetails.get("upload").equals("true") || imageDetails.get("upload").equals("check")) {
              vnfPackage.setImageLink((String) imageDetails.get("link"));
              if (metadata.containsKey("image-config")) {
                log.debug("image-config: " + metadata.get("image-config"));
                Map<String, Object> imageConfig = (Map<String, Object>) metadata.get("image-config");
                //Check if all required keys are available
                String[]
                    REQUIRED_IMAGE_CONFIG =
                    new String[]{"name", "diskFormat", "containerFormat", "minCPU", "minDisk", "minRam", "isPublic"};
                for (String requiredKey : REQUIRED_IMAGE_CONFIG) {
                  if (!imageConfig.containsKey(requiredKey)) {
                    throw new PackageIntegrityException("Not found key: " +
                                                        requiredKey +
                                                        " of image-config in Metadata.yaml");
                  }
                  if (imageConfig.get(requiredKey) == null) {
                    throw new PackageIntegrityException("Not defined value of key: " +
                                                        requiredKey +
                                                        " of image-config in Metadata.yaml");
                  }
                }
                image.setName((String) imageConfig.get("name"));
                image.setDiskFormat(((String) imageConfig.get("diskFormat")).toUpperCase());
                image.setContainerFormat(((String) imageConfig.get("containerFormat")).toUpperCase());
                image.setMinCPU(Integer.toString((Integer) imageConfig.get("minCPU")));
                image.setMinDiskSpace((Integer) imageConfig.get("minDisk"));
                image.setMinRam((Integer) imageConfig.get("minRam"));
                image.setIsPublic(Boolean.parseBoolean(Integer.toString((Integer) imageConfig.get("minRam"))));
              } else {
                throw new PackageIntegrityException(
                    "The image-config is not defined. Please define it to upload a new image");
              }
            }
          } else {
            throw new PackageIntegrityException(
                "The image details are not defined. Please define it to use the right image");
          }
        } else if (!entry.getName().startsWith("scripts/") && entry.getName().endsWith(".json")) {
          //this must be the vnfd
          //and has to be onboarded in the catalogue
          String json = new String(content);
          log.trace("Content of json is: " + json);
          try {
            virtualNetworkFunctionDescriptor = mapper.fromJson(json, VirtualNetworkFunctionDescriptor.class);
            //remove the images
            for (VirtualDeploymentUnit vdu : virtualNetworkFunctionDescriptor.getVdu()) {
              vdu.setVm_image(new HashSet<String>());
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
          log.trace("Created VNFD: " + virtualNetworkFunctionDescriptor);
        } else if (entry.getName().endsWith(".img")) {
          //this must be the image
          //and has to be upladed to the RIGHT vim
          imageFile = content;
          log.debug("imageFile is: " + entry.getName());
          throw new VimException(
              "Uploading an image file from the VNFPackage is not supported at this moment. Please use the image link" +
              ".");
        } else if (entry.getName().startsWith("scripts/")) {
          Script script = new Script();
          script.setName(entry.getName().substring(8));
          script.setPayload(content);
          vnfPackage.getScripts().add(script);
        }
      }
    }
    if (metadata == null) {
      throw new PackageIntegrityException("Not found Metadata.yaml");
    }
    if (vnfPackage.getScriptsLink() != null) {
      if (vnfPackage.getScripts().size() > 0) {
        log.debug("VNFPackageManagement: Remove scripts got by scripts/ because the scripts-link is defined");
        vnfPackage.setScripts(new HashSet<Script>());
      }
    }
    List<String> vimInstances = new ArrayList<>();
    if (imageDetails.get("upload").equals("check")) {
      if (!imageLink) {
        if (vnfPackage.getImageLink() == null && imageFile == null) {
          throw new PackageIntegrityException(
              "VNFPackageManagement: For option upload=check you must define an image. Neither the image link is " +
              "defined nor the image file is available. Please define at least one if you want to upload a new image");
        }
      }
    }

    if (imageDetails.get("upload").equals("true")) {
      log.debug("VNFPackageManagement: Uploading a new Image");
      if (vnfPackage.getImageLink() == null && imageFile == null) {
        throw new PackageIntegrityException(
            "VNFPackageManagement: Neither the image link is defined nor the image file is available. Please define " +
            "at least one if you want to upload a new image");
      }
    } else {
      if (!imageDetails.containsKey("ids") && !imageDetails.containsKey("names")) {
        throw new PackageIntegrityException(
            "VNFPackageManagement: Upload option 'false' or 'check' requires at least a list of ids or names to find " +
            "the right image.");
      }
    }
    vnfPackage.setImage(image);
    myTarFile.close();
    vnfPackage = vnfPackageRepository.save(vnfPackage);
    virtualNetworkFunctionDescriptor.setVnfPackageLocation(vnfPackage.getId());

    VNFPackageMetadata vnfPackageMetadata = new VNFPackageMetadata();
    vnfPackageMetadata.setName(vnfPackage.getName());
    vnfPackageMetadata.setUsername(userManagement.getCurrentUser());
    vnfPackageMetadata.setVnfd(virtualNetworkFunctionDescriptor);
    vnfPackageMetadata.setVnfPackage(vnfPackage);
    vnfPackageMetadata.setNfvImage(image);
    vnfPackageMetadata.setImageMetadata(imageMetadata);
    vnfPackageMetadata.setVnfPackageFileName(fileName);
    vnfPackageMetadata.setVnfPackageFile(pack);
    String description = (String) metadata.get("description");
    if (description.length() > 100) {
      description = description.substring(0, 100);
    }
    vnfPackageMetadata.setDescription(description);
    vnfPackageMetadata.setProvider((String) metadata.get("provider"));
    vnfPackageMetadata.setRequirements((Map) metadata.get("requirements"));
    vnfPackageMetadata.setShared((boolean) metadata.get("shared"));
    vnfPackageMetadata.setMd5sum(DigestUtils.md5DigestAsHex(pack));
    try {
      this.dispatch(vnfPackageMetadata);
    } catch (FailedToUploadException e) {
      vnfPackageRepository.delete(vnfPackage.getId());
      throw e;
    }
    vnfPackageMetadataRepository.save(vnfPackageMetadata);

    //        vnfdRepository.save(virtualNetworkFunctionDescriptor);
    log.debug("Persisted " + vnfPackageMetadata);
    //        log.trace("Onboarded VNFPackage (" + virtualNetworkFunctionDescriptor.getVnfPackageLocation() + ")
    // successfully");


    return vnfPackageMetadata;
  }

  private void updateVims() throws IOException, InterruptedException, ClassNotFoundException, SDKException {

    NFVORequestor requestor = new NFVORequestor(obUsername, obPassword, "", obSslEnabled, obNfvoIp, obNfvoPort, "1");

    String
        cmd =
        "curl -u openbatonOSClient:secret -X POST http://" +
        obNfvoIp +
        ":" +
        obNfvoPort +
        "/oauth/token -H \"Accept:application/json\" -d username=" +
        obUsername +
        "&password=" +
        obPassword +
        "&grant_type=password";

    log.debug("Executing command: " + cmd);

    Process
        process =
        Runtime.getRuntime()
               .exec(cmd);
    int res = process.waitFor();
    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));

    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

    String s;
    String output = "";
    String imageId = null;
    if (res != 0) {
      log.warn("Probably the upload of the image went wrong!");
      while ((s = stdError.readLine()) != null) {
        log.warn(s);
      }
    } else {
      while ((s = stdInput.readLine()) != null) {

        output += s;
      }
    }

    log.debug("Result is: " + output);

    String token = new GsonBuilder().create().fromJson(output, JsonObject.class).get("value").getAsString();

    ProjectAgent projectAgent = requestor.getProjectAgent();
    for (Project project : projectAgent.findAll()) {
      requestor.setProjectId(project.getId());
      VimInstanceRestAgent vimInstanceRestAgent = requestor.getVimInstanceAgent();
      vimInstanceRestAgent.setProjectId(project.getId());
      for (VimInstance vimInstance : vimInstanceRestAgent.findAll()) {
        String vimId = vimInstance.getId();
        log.debug("Found Vim Id: " + vimId);
        Runtime.getRuntime()
               .exec("curl POST -H \"Content-type: application/json\" -H \"Authorization: Bearer " +
                     token +
                     "\" http://" +
                     obNfvoIp +
                     ":" +
                     obNfvoPort +
                     "/api/v1/datacenters/" +
                     vimId +
                     "/refresh");
      }
    }
  }

  public ByteArrayOutputStream download(String id) throws IOException {
    VNFPackageMetadata vnfPackageMetadata = vnfPackageMetadataRepository.findFirstById(id);
    byte[] vnfPackageFile = vnfPackageMetadata.getVnfPackageFile();
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    outStream.write(vnfPackageFile);
    return outStream;
  }

  public ByteArrayOutputStream compose(String id) throws IOException, ArchiveException {

    VNFPackageMetadata vnfPackageMetadata = vnfPackageMetadataRepository.findFirstById(id);
    String vnfPackageName = vnfPackageMetadata.getName();
    VirtualNetworkFunctionDescriptor vnfd = vnfPackageMetadata.getVnfd();
    VNFPackage vnfPackage = vnfPackageMetadata.getVnfPackage();
    ImageMetadata imageMetadata = vnfPackageMetadata.getImageMetadata();
    NFVImage nfvImage = vnfPackageMetadata.getNfvImage();
    String vnfdJson = mapper.toJson(vnfd);

    HashMap<String, Object> imageConfigJson = new ObjectMapper().readValue(mapper.toJson(nfvImage), HashMap.class);
    imageConfigJson.put("minDisk", imageConfigJson.get("minDiskSpace"));
    Object minCPU = imageConfigJson.get("minCPU");
    if (minCPU != null) {
      imageConfigJson.put("minCPU", Integer.parseInt((String) minCPU));
    } else {
      imageConfigJson.put("minCPU", 0);
    }
    imageConfigJson.remove("minDiskSpace");
    imageConfigJson.remove("id");
    imageConfigJson.remove("hb_version");

    HashMap<String, String>
        imageMetadataJson =
        new ObjectMapper().readValue(mapper.toJson(imageMetadata), HashMap.class);
    imageMetadataJson.put("link", imageMetadata.getLink());
    imageMetadataJson.remove("id");
    imageMetadataJson.remove("hb_version");

    ByteArrayOutputStream tar_output = new ByteArrayOutputStream();
    ArchiveOutputStream
        my_tar_ball =
        new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, tar_output);

    //prepare Metadata.yaml
    File tar_input_file = File.createTempFile("Metadata", null);
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("name", vnfPackageName);
    data.put("description", vnfPackageMetadata.getDescription());
    data.put("provider", vnfPackageMetadata.getProvider());
    data.put("requirements", vnfPackageMetadata.getRequirements());
    data.put("shared", vnfPackageMetadata.isShared());
    data.put("image", imageMetadataJson);
    data.put("image-config", imageConfigJson);
    data.put("scripts-link", vnfPackage.getScriptsLink());
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);
    FileWriter writer = new FileWriter(tar_input_file);
    yaml.dump(data, writer);
    TarArchiveEntry tar_file = new TarArchiveEntry(tar_input_file, "Metadata.yaml");
    tar_file.setSize(tar_input_file.length());
    my_tar_ball.putArchiveEntry(tar_file);
    IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball);
        /* Close Archieve entry, write trailer information */
    my_tar_ball.closeArchiveEntry();

    //prepare VNFD
    tar_input_file = File.createTempFile("vnfd", null);
    tar_file = new TarArchiveEntry(tar_input_file, "vnfd.json");
    writer = new FileWriter(tar_input_file);
    writer.write(vnfdJson);
    writer.close();
    tar_file.setSize(tar_input_file.length());
    my_tar_ball.putArchiveEntry(tar_file);
    IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball);
        /* Close Archieve entry, write trailer information */
    my_tar_ball.closeArchiveEntry();

    //scripts
    for (Script script : vnfPackage.getScripts()) {
      tar_input_file = File.createTempFile("script", null);
      tar_file = new TarArchiveEntry(tar_input_file, "scripts/" + script.getName());
      FileOutputStream outputStream = new FileOutputStream(tar_input_file);
      outputStream.write(script.getPayload());
      outputStream.close();
      tar_file.setSize(tar_input_file.length());
      my_tar_ball.putArchiveEntry(tar_file);
      IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball);
      my_tar_ball.closeArchiveEntry();
    }

    //close tar
    my_tar_ball.finish();
        /* Close output stream, our files are zipped */
    tar_output.close();
    return tar_output;
  }

  public void delete(String id) throws NotFoundException, NotAuthorizedException {
    log.debug("Deleting VNFPackage: " + id);
    boolean delete = true;
    VNFPackageMetadata vnfPackageMetadata = vnfPackageMetadataRepository.findFirstById(id);
    if (vnfPackageMetadata == null) {
      throw new NotFoundException("Not found VNFPackage with ID: " + id);
    }
    if (vnfPackageMetadata.getImageMetadata().getImageRepoId() != null) {
      for (VNFPackageMetadata metadata : vnfPackageMetadataRepository.findAll()) {
        log.debug(metadata.getImageMetadata().getImageRepoId() +
                  " == " +
                  vnfPackageMetadata.getImageMetadata().getImageRepoId());
        if (!metadata.getId().equals(id) &&
            metadata.getImageMetadata()
                    .getImageRepoId()
                    .equals(vnfPackageMetadata.getImageMetadata().getImageRepoId())) {
          delete = false;
        }
      }
    } else {
      delete = false;
    }

    if (delete) {
      log.debug("Removing image: " + vnfPackageMetadata.getImageMetadata().getId());
      imageManager.forceDeleteImage(vnfPackageMetadata.getImageMetadata().getId());
    }
    try {
      this.deleteOnFitEagle(vnfPackageMetadata);
    } catch (Exception e) {
      log.error("Exception deleting on fiteagle");
    }
    vnfPackageMetadataRepository.delete(id);
    log.info("Deleted VNFPackage: " + id);
  }

  public VNFPackageMetadata get(String id) throws NotFoundException {
    log.debug("Getting VNFPackageMetadata: " + id);
    VNFPackageMetadata vnfPackageMetadata = vnfPackageMetadataRepository.findFirstById(id);
    if (vnfPackageMetadata == null) {
      throw new NotFoundException("Not found VNFPackageMetadata with ID: " + id);
    }
    log.info("Got VNFPackageMetadata: " + vnfPackageMetadata);
    return vnfPackageMetadata;
  }

  public Set<VNFPackageMetadata> get() {
    log.debug("Listing VNFPackagesMetadata ...");
    Iterable<VNFPackageMetadata> vnfPackagesMetadata = vnfPackageMetadataRepository.findAll();
    Set<VNFPackageMetadata> result = new HashSet<>();
    String currentUser = userManagement.getCurrentUser();
    for (VNFPackageMetadata metadata : vnfPackagesMetadata) {
      log.trace("Checking package: " + metadata.getName());
      log.trace(metadata.getImageMetadata().getUsername() + " == " + currentUser);
      if (metadata.getImageMetadata().getUsername().equals(currentUser)) {
        result.add(metadata);
      }
    }
    log.debug("Listed VNFPackagesMetadata: ");
    log.trace("" + result);
    return result;
  }

  //    public VNFPackage update(String id, VNFPackage vnfPackage) throws NotFoundException {

  //    }
  //        return applicationToUpdate;
  //        log.info("Updated VNFPackage: " + applicationToUpdate);
  //        applicationToUpdate = vnfPackageRepository.save(applicationToUpdate);
  //        applicationToUpdate = vnfPackage;
  ////        vnfPackage.setHb_version(applicationToUpdate.getHb_version());
  //        vnfPackage.setId(applicationToUpdate.getId());
  //        }
  //            throw new NotFoundException("Not found VNFPackage with ID: " + id);
  //        if (applicationToUpdate == null) {
  //        VNFPackage applicationToUpdate = vnfPackageRepository.findOne(id);
  //        log.debug("Updating VNFPackage: " + id);

  /*
   *
   *
   * This is how the file should look like...
   *
   * curl -X POST -F 'file=@/Path/To/The/Package.tar' -F projectId=123456 localhost:8080/OpenBaton/upload
   *
   * [
   *          {
   *                  "url":"http://localhost:8080",
   *                  "username":"admin",
   *                 "password":"openbaton",
   *                  "projectId":"lfjfld-93nrpd9-3nfi-49fnwlD",
   *                  "ssl":false
   *          }
   *  ]
   *
  */
  public void dispatch(VNFPackageMetadata vnfPackageMetadata) throws FailedToUploadException {

    RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(10000).setConnectTimeout(60000).build();
    CloseableHttpResponse response = null;
    HttpPost httpPost = null;
    String url = "https://" + fitEagleIp + ":" + fitEaglePort + "/OpenBaton/upload/v2";
    try {
      log.debug("Executing post on " + url);
      httpPost = new HttpPost(url);
      //      httpPost.setHeader(new BasicHeader("Accept", "multipart/form-data"));
      //      httpPost.setHeader(new BasicHeader("Content-type", "multipart/form-data"));
      httpPost.setHeader(new BasicHeader("username", userManagement.getCurrentUser()));
      httpPost.setHeader(new BasicHeader("filename", vnfPackageMetadata.getVnfPackageFileName()));

      MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
      multipartEntityBuilder.addBinaryBody("file", vnfPackageMetadata.getVnfPackageFile());
      httpPost.setEntity(multipartEntityBuilder.build());

      CloseableHttpClient client = getHttpClientForSsl(config);

      response = client.execute(httpPost);
    } catch (ClientProtocolException e) {
      httpPost.releaseConnection();
      e.printStackTrace();
      log.error("NotAble to upload VNFPackage");
      throw new FailedToUploadException("Not Able to upload VNFPackage to Fiteagle because: " + e.getMessage());
    } catch (IOException e) {
      httpPost.releaseConnection();
      e.printStackTrace();
      httpPost.releaseConnection();
      log.error("NotAble to upload VNFPackage");
      throw new FailedToUploadException("Not Able to upload VNFPackage to Fiteagle because: " + e.getMessage());
    }

    // check response status
    String result = "";
    if (response != null && response.getEntity() != null) {
      try {
        result = EntityUtils.toString(response.getEntity());
      } catch (IOException e) {
        e.printStackTrace();
        httpPost.releaseConnection();
        throw new FailedToUploadException("Not Able to upload VNFPackage to Fiteagle because: " + e.getMessage());
      }
    }

    if (response != null && response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
      log.debug("Uploaded the VNFPackage");
      log.debug("received: " + result);
      if (vnfPackageMetadata.getRequirements() == null) {
        vnfPackageMetadata.setRequirements(new HashMap<String, String>());
      }
      vnfPackageMetadata.getRequirements().put("fiteagle-id", result);
    } else throw new FailedToUploadException("Not Able to upload VNFPackage to Fiteagle because: Fiteagle answered " + response.getStatusLine().getStatusCode());
    httpPost.releaseConnection();
  }

  private void deleteOnFitEagle(VNFPackageMetadata vnfPackageMetadata) {
    RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(10000).setConnectTimeout(60000).build();
    CloseableHttpResponse response = null;
    HttpDelete httpDelete = null;
    String
        url =
        "https://" +
        fitEagleIp +
        ":" +
        fitEaglePort +
        "/OpenBaton/upload/v2/" +
        vnfPackageMetadata.getRequirements().get("fiteagle-id");
    try {
      log.debug("Executing DELETE on " + url);
      httpDelete = new HttpDelete(url);
      //      httpPost.setHeader(new BasicHeader("Accept", "multipart/form-data"));
      //      httpPost.setHeader(new BasicHeader("Content-type", "multipart/form-data"));
      httpDelete.setHeader(new BasicHeader("username", userManagement.getCurrentUser()));
      httpDelete.setHeader(new BasicHeader("filename", vnfPackageMetadata.getVnfPackageFileName()));
      httpDelete.setHeader(new BasicHeader("name", vnfPackageMetadata.getName()));
      httpDelete.setHeader(new BasicHeader("package-id", vnfPackageMetadata.getRequirements().get("fiteagle-id")));

      CloseableHttpClient client = getHttpClientForSsl(config);

      response = client.execute(httpDelete);
    } catch (ClientProtocolException e) {
      httpDelete.releaseConnection();
      e.printStackTrace();
      log.error("Not able to delete VNFPackage");
      return;
    } catch (IOException e) {
      httpDelete.releaseConnection();
      e.printStackTrace();
      log.error("Not able to delete VNFPackage");
      return;
    }

    // check response status
    String result = "";
    if (response != null && response.getEntity() != null) {
      try {
        result = EntityUtils.toString(response.getEntity());
      } catch (IOException e) {
        e.printStackTrace();
        httpDelete.releaseConnection();
        return;
      }
    }

    if (response != null && response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_NO_CONTENT) {
      log.debug("Deleted the VNFPackage");
      log.debug("received: " + result);
    }
    httpDelete.releaseConnection();
  }

  private CloseableHttpClient getHttpClientForSsl(RequestConfig config) {
    SSLContext sslContext = null;
    try {
      sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
    } catch (NoSuchAlgorithmException e) {
      log.error("Could not initialize the HttpClient for SSL connections");
      log.error(e.getMessage(), e);
    } catch (KeyManagementException e) {
      log.error("Could not initialize the HttpClient for SSL connections");
      log.error(e.getMessage(), e);
    } catch (KeyStoreException e) {
      log.error("Could not initialize the HttpClient for SSL connections");
      log.error(e.getMessage(), e);
    }

    // necessary to trust self signed certificates
    SSLConnectionSocketFactory
        sslConnectionSocketFactory =
        new SSLConnectionSocketFactory(sslContext, new String[]{"TLSv1"}, null, new NoopHostnameVerifier());

    Registry<ConnectionSocketFactory>
        socketFactoryRegistry =
        RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslConnectionSocketFactory).build();

    return HttpClientBuilder.create()
                            .setDefaultRequestConfig(config)
                            .setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry))
                            .setSSLSocketFactory(sslConnectionSocketFactory)
                            .build();
  }

  public void dispatch3(VNFPackageMetadata packageMetadata) throws IOException, ArchiveException, SDKException {
    log.debug("Trying to upload package to FITEagle");

    String url = "http://" + fitEagleIp + ":" + fitEaglePort + "/OpenBaton/upload/v2";

    log.debug("FITEagle URL is: " + url);

    //    HttpClient client = HttpClientBuilder.create().build();
    HttpClient client = HttpClients.createDefault();
    HttpPost request = new HttpPost(url);

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();

    byte[] vnfPackageFile = packageMetadata.getVnfPackageFile();
    log.debug("Trying to add binary file of length: " + vnfPackageFile.length);
    builder.addBinaryBody("file", vnfPackageFile, ContentType.APPLICATION_OCTET_STREAM, packageMetadata.getName());

    String currentUser = userManagement.getCurrentUser();
    request.addHeader("username", currentUser);
    log.debug("Added username header: " + currentUser);

    request.setEntity(builder.build());
    log.debug("Added binary file of length: " + vnfPackageFile.length);
    HttpResponse response = client.execute(request);
    log.debug("Executed POST!");
    log.debug("Response Code from FITEAGLE: " + response.getStatusLine().getStatusCode());
  }

  public void dispatch2(VNFPackageMetadata packageMetadata) throws IOException, ArchiveException, SDKException {
    if (nfvoListFile == null) {
      log.info("no nfvo list file defined, using classpath file");
      nfvoListFile = this.getClass().getResource("/nfvo-list.json").getFile();
    }
    JsonArray nfvoList;
    try {
      nfvoList = mapper.fromJson(new FileReader(nfvoListFile), JsonArray.class);
    } catch (FileNotFoundException e) {
      log.warn("File " + nfvoListFile + " not found, not pushing anything");
      return;
    }
    File tar = File.createTempFile(packageMetadata.getName(), ".tar");
    FileOutputStream fileOutputStream = new FileOutputStream(tar);
    fileOutputStream.write(compose(packageMetadata.getId()).toByteArray());

    for (JsonElement nfvo : nfvoList) {
      String username = nfvo.getAsJsonObject().get("username").getAsString();
      String password = nfvo.getAsJsonObject().get("password").getAsString();
      String projectId = nfvo.getAsJsonObject().get("projectId").getAsString();

      boolean sslEnabled = nfvo.getAsJsonObject().get("ssl").getAsBoolean();
      URL url = new URL(nfvo.getAsJsonObject().get("url").getAsString());
      String nfvoIp = url.getHost();
      String nfvoPort = String.valueOf(url.getPort());
      NFVORequestor nfvoRequestor = new NFVORequestor(username, password, projectId, sslEnabled, nfvoIp, nfvoPort, "1");

      nfvoRequestor.getVNFPackageAgent().create(tar.getPath());
    }
  }

  private Set fromIterbaleToSet(Iterable iterable) {
    Set set = new HashSet();
    Iterator iterator = iterable.iterator();
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return set;
  }

  public static void main(String[] args) {
    VNFPackageManagement vnfPackageManagement = new VNFPackageManagement();

    try {
      vnfPackageManagement.updateVims();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SDKException e) {
      e.printStackTrace();
    }
  }
}
