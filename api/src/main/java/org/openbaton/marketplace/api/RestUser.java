package org.openbaton.marketplace.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.openbaton.marketplace.exceptions.NotAuthorizedException;
import org.openbaton.nfvo.security.authorization.UserManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by lto on 18/08/16.
 */
@RestController
@RequestMapping("/api/v1/users")
public class RestUser {

  private Gson gson = new GsonBuilder().create();
  @Autowired private UserManagement userManagement;
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public void createUser(@RequestBody String body) throws NotAuthorizedException {
    JsonObject obj = gson.fromJson(body,JsonObject.class);
    userManagement.add(obj.get("username").getAsString(), obj.get("password").getAsString());
  }

  @RequestMapping(value = "{username}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public void deleteUser(@PathVariable("username") String username) throws NotAuthorizedException {
    userManagement.delete(username);
  }
}

