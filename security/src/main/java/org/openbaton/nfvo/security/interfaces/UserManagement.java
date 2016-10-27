/*
 * Copyright (c) 2015 Fraunhofer FOKUS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.nfvo.security.interfaces;

//import org.openbaton.catalogue.security.User;

import org.openbaton.catalogue.security.User;
import org.openbaton.marketplace.exceptions.NotAuthorizedException;

/**
 * Created by mpa on 30/04/15.
 */

public interface UserManagement {
  String getCurrentUser();

  void add(String username, String password) throws NotAuthorizedException;

  void delete(String user) throws NotAuthorizedException;
/*
    User getCurrentUser();

    *//**
     *
     * @param user
     *//*
    User add(User user);

    *//**
     *
     * @param user
     *//*
    void delete(User user);

    *//**
     *
     * @param new_user
     *//*
    User update(User new_user);

    *//**
     *//*
    Iterable<User> query();

    *//**
     *
     * @param username
     *//*
    User query(String username);


    User queryDB(String currentUserName);*/
}
