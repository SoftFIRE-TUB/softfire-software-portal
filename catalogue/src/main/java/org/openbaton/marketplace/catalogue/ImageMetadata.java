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

import org.openbaton.catalogue.util.IdGenerator;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Version;

/**
 * Created by mpa on 22/05/16.
 */
@Entity
public class ImageMetadata implements Serializable {

  @Id private String id;

  @JsonIgnore
  @Version
  private int hb_version = 0;

  private String upload;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @JsonIgnore private String username;

  @ElementCollection(fetch = FetchType.EAGER) private List<String> ids;

  @ElementCollection(fetch = FetchType.EAGER) private List<String> names;

  public Map<String, String> getExtIds() {
    return extIds;
  }

  public void setExtIds(Map<String, String> extIds) {
    this.extIds = extIds;
  }

  @ElementCollection(fetch = FetchType.EAGER) private Map<String, String> extIds;

  private String link;

  public String getImageRepoId() {
    return imageRepoId;
  }

  @JsonIgnore private String imageRepoId;

  @PrePersist
  public void ensureId() {
    id = IdGenerator.createUUID();
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

  @Override
  public String toString() {
    return "ImageMetadata{" +
           "id='" + id + '\'' +
           ", hb_version=" + hb_version +
           ", upload='" + upload + '\'' +
           ", ids=" + ids +
           ", names=" + names +
           ", link='" + link + '\'' +
           ", imageRepoId='" + imageRepoId + '\'' +
           '}';
  }

  public void setHb_version(int hb_version) {
    this.hb_version = hb_version;
  }

  public String getUpload() {
    return upload;
  }

  public void setUpload(String upload) {
    this.upload = upload;
  }

  public void setIds(List<String> ids) {
    this.ids = ids;
  }

  public void setNames(List<String> names) {
    this.names = names;
  }

  public List<String> getIds() {
    return ids;
  }

  public List<String> getNames() {
    return names;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setImageRepoId(String id) {
    this.imageRepoId = id;
  }
}
