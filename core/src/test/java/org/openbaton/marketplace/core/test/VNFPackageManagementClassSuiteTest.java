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

package org.openbaton.marketplace.core.test;

import org.apache.commons.compress.archivers.ArchiveException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbaton.catalogue.mano.common.HighAvailability;
import org.openbaton.catalogue.mano.common.ResiliencyLevel;
import org.openbaton.catalogue.mano.common.VNFDeploymentFlavour;
import org.openbaton.catalogue.mano.descriptor.VirtualDeploymentUnit;
import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.nfvo.NFVImage;
import org.openbaton.catalogue.nfvo.Script;
import org.openbaton.catalogue.nfvo.VNFPackage;
import org.openbaton.catalogue.nfvo.VimInstance;
import org.openbaton.exceptions.AlreadyExistingException;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimException;
import org.openbaton.marketplace.catalogue.ImageMetadata;
import org.openbaton.marketplace.catalogue.VNFPackageMetadata;
import org.openbaton.marketplace.core.VNFPackageManagement;
import org.openbaton.marketplace.repository.repository.VNFPackageMetadataRepository;
import org.openbaton.marketplace.repository.repository.VNFPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Created by lto on 20/04/15.
 */
public class VNFPackageManagementClassSuiteTest {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @InjectMocks private VNFPackageManagement vnfPackageManagement;

  @Mock private VNFPackageMetadataRepository vnfPackageMetadataRepository;

  @Mock private VNFPackageRepository vnfPackageRepository;

  @AfterClass
  public static void shutdown() {
    // TODO Teardown to avoid exceptions during test shutdown
  }

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    log.info("Starting test");
  }

  @Test
  public void vnfPackageManagementNotNull() {
    Assert.assertNotNull(vnfPackageManagement);
  }

  //MethodName_StateUnderTest_ExpectedBehavior
  @Test
  public void add_onboardValidVNFP_Success() throws
                                             IOException,
                                             ArchiveException,
                                             SQLException,
                                             VimException,
                                             NotFoundException,
                                             PluginException,
                                             AlreadyExistingException {
    VNFPackageMetadata expectedVnfPackageMetadata = getVnfPackageMetadata();
    when(vnfPackageRepository.save(any(VNFPackage.class))).thenReturn(expectedVnfPackageMetadata.getVnfPackage());
    VNFPackageMetadata
        actualVnfPackageMetadata =
        vnfPackageManagement.add("test_package.tar", getPackageTar(expectedVnfPackageMetadata).toByteArray(), false);
    assertEqualsVnfPackageMetadata(expectedVnfPackageMetadata, actualVnfPackageMetadata);
  }

  public void assertEqualsVnfPackageMetadata(VNFPackageMetadata expected, VNFPackageMetadata actual) {
    Assert.assertEquals(expected.getName(), actual.getName());
    //        Assert.assertEquals(expected.getMd5sum(), actual.getMd5sum());
    //        Assert.assertEquals(expected.getMd5sum(), actual.getName());
  }

  public ByteArrayOutputStream getPackageTar(VNFPackageMetadata vnfPackageMetadata) throws
                                                                                    IOException,
                                                                                    ArchiveException {
    when(vnfPackageMetadataRepository.save(vnfPackageMetadata)).thenReturn(vnfPackageMetadata);
    VNFPackageMetadata vnfPackageMetadataStored = vnfPackageMetadataRepository.save(vnfPackageMetadata);
    when(vnfPackageMetadataRepository.findFirstById("mocked_id")).thenReturn(vnfPackageMetadataStored);
    return vnfPackageManagement.compose(vnfPackageMetadataStored.getId());
  }

  public VNFPackageMetadata getVnfPackageMetadata() {
    VNFPackageMetadata vnfPackageMetadata = new VNFPackageMetadata();
    vnfPackageMetadata.setId("mocked_id");
    vnfPackageMetadata.setName("test_package");
    vnfPackageMetadata.setShared(false);
    vnfPackageMetadata.setDescription("test_description");
    vnfPackageMetadata.setProvider("test_provider");
    vnfPackageMetadata.setVnfd(getVnfd());
    vnfPackageMetadata.setImageMetadata(getImageMetadata());
    vnfPackageMetadata.setVnfPackage(getVnfPackage());
    vnfPackageMetadata.setNfvImage(getNfvImage());
    vnfPackageMetadata.setVnfPackageFileName("test_package.tar");
    vnfPackageMetadata.setRequirements(getRequirements());
    return vnfPackageMetadata;
  }

  private VirtualNetworkFunctionDescriptor getVnfd() {
    VirtualNetworkFunctionDescriptor virtualNetworkFunctionDescriptor = new VirtualNetworkFunctionDescriptor();
    virtualNetworkFunctionDescriptor.setName("" + ((int) (Math.random() * 1000)));
    virtualNetworkFunctionDescriptor.setEndpoint("test");
    virtualNetworkFunctionDescriptor.setMonitoring_parameter(new HashSet<String>() {
      {
        add("monitor1");
        add("monitor2");
        add("monitor3");
      }
    });
    virtualNetworkFunctionDescriptor.setDeployment_flavour(new HashSet<VNFDeploymentFlavour>() {{
      VNFDeploymentFlavour vdf = new VNFDeploymentFlavour();
      vdf.setExtId("ext_id");
      vdf.setFlavour_key("flavor_name");
      add(vdf);
    }});
    virtualNetworkFunctionDescriptor.setVdu(new HashSet<VirtualDeploymentUnit>() {
      {
        VirtualDeploymentUnit vdu = new VirtualDeploymentUnit();
        HighAvailability highAvailability = new HighAvailability();
        highAvailability.setGeoRedundancy(false);
        highAvailability.setRedundancyScheme("1:N");
        highAvailability.setResiliencyLevel(ResiliencyLevel.ACTIVE_STANDBY_STATELESS);
        vdu.setHigh_availability(highAvailability);
        vdu.setComputation_requirement("high_requirements");
        VimInstance vimInstance = new VimInstance();
        vimInstance.setName("vim_instance");
        vimInstance.setType("test");
        add(vdu);
      }
    });
    virtualNetworkFunctionDescriptor.setVnfPackageLocation("http://an.ip.here.com");
    return virtualNetworkFunctionDescriptor;
  }

  private Map<String, String> getRequirements() {
    Map requirements = new HashMap();
    requirements.put("test_key1", "test_value1");
    requirements.put("test_key2", "test_value2");
    return requirements;
  }

  private ImageMetadata getImageMetadata() {
    ImageMetadata imageMetadata = new ImageMetadata();
    List<String> ids = new ArrayList<>();
    ids.add("test_id1");
    ids.add("test_id2");
    imageMetadata.setIds(ids);
    List<String> names = new ArrayList<>();
    names.add("test_id1");
    names.add("test_id2");
    imageMetadata.setNames(names);
    imageMetadata.setLink("images_test_link");
    imageMetadata.setUpload("check");
    return imageMetadata;
  }

  private NFVImage getNfvImage() {
    NFVImage nfvImage = new NFVImage();
    nfvImage.setName("test_image_name");
    nfvImage.setExtId("test_ext_id");
    nfvImage.setMinCPU("1");
    nfvImage.setMinRam(1234);
    nfvImage.setMinDiskSpace(12345);
    nfvImage.setCreated(new Date(1));
    nfvImage.setUpdated(new Date(2));
    nfvImage.setIsPublic(true);
    nfvImage.setContainerFormat("test_containerFormat");
    nfvImage.setDiskFormat("test_DiskFormat");
    return nfvImage;
  }

  private VNFPackage getVnfPackage() {
    VNFPackage vnfPackage = new VNFPackage();
    vnfPackage.setName("test_vnfpackageName");
    vnfPackage.setScripts(new HashSet<Script>());
    vnfPackage.setImage(getNfvImage());
    vnfPackage.setScriptsLink("scripts_test_link");
    vnfPackage.setImageLink("images_test_link");
    return vnfPackage;
  }
}
