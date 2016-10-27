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

package org.openbaton.marketplace.catalogue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.nfvo.NFVImage;
import org.openbaton.catalogue.nfvo.VNFPackage;
import org.openbaton.catalogue.util.IdGenerator;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Version;

/**
 * Created by mpa on 22/05/16.
 */
@Entity
@JsonIgnoreProperties(value = { "vnfPackageFile", "hb_version", "vnfPackage" })
public class VNFPackageMetadata implements Serializable {

    @Id
    private String id;

    @JsonIgnore
    @Version
    private int hb_version = 0;

    //Name of the Package
    private String name;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String username;

    private String description;

    private String provider;

    private boolean shared;

    @ElementCollection
    private Map<String, String> requirements;

    //URL to the image's location
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JsonIgnore
    private VNFPackage vnfPackage;

    //URL to the scripts' location
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private VirtualNetworkFunctionDescriptor vnfd;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private NFVImage nfvImage;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private ImageMetadata imageMetadata;

    private String vnfPackageFileName;

    @JsonIgnore
    @Lob
    private byte[] vnfPackageFile;

    private String md5sum;

    @PrePersist
    public void ensureId(){
        id= IdGenerator.createUUID();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getHb_version() {
        return hb_version;
    }

    public void setHb_version(int hb_version) {
        this.hb_version = hb_version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, String> requirements) {
        this.requirements = requirements;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public String getVnfPackageFileName() {
        return vnfPackageFileName;
    }

    public void setVnfPackageFileName(String vnfPackageFileName) {
        this.vnfPackageFileName = vnfPackageFileName;
    }

    public VNFPackage getVnfPackage() {
        return vnfPackage;
    }

    public void setVnfPackage(VNFPackage vnfPackage) {
        this.vnfPackage = vnfPackage;
    }

    public VirtualNetworkFunctionDescriptor getVnfd() {
        return vnfd;
    }

    public void setVnfd(VirtualNetworkFunctionDescriptor vnfd) {
        this.vnfd = vnfd;
    }

    public NFVImage getNfvImage() {
        return nfvImage;
    }

    public void setNfvImage(NFVImage nfvImage) {
        this.nfvImage = nfvImage;
    }

    public ImageMetadata getImageMetadata() {
        return imageMetadata;
    }

    public void setImageMetadata(ImageMetadata imageMetadata) {
        this.imageMetadata = imageMetadata;
    }

    public byte[] getVnfPackageFile() {
        return vnfPackageFile;
    }

    public void setVnfPackageFile(byte[] vnfPackageFile) {
        this.vnfPackageFile = vnfPackageFile;
    }

    @Override
    public String toString() {
        return "VNFPackageMetadata{" +
                "id='" + id + '\'' +
                ", hb_version=" + hb_version +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", provider='" + provider + '\'' +
                ", shared=" + shared +
                ", requirements=" + requirements +
                ", md5sum=" + md5sum + '\'' +
                ", vnfPackage=" + vnfPackage +
                ", vnfd=" + vnfd +
                ", nfvImage=" + nfvImage +
                ", imageMetadata=" + imageMetadata +
                ", vnfPackageFileName=" + vnfPackageFileName + '\'' +
//                ", vnfPackageFile=" + Arrays.toString(vnfPackageFile) +
                '}';
    }
}
