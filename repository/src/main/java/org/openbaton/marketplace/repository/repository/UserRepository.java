package org.openbaton.marketplace.repository.repository;

import org.openbaton.catalogue.security.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by lto on 18/08/16.
 */
public interface UserRepository extends CrudRepository<User,String>{
  void deleteByUsername(String username);
}
