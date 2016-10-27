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

package org.openbaton.nfvo.security.authentication;

import org.openbaton.catalogue.security.User;
import org.openbaton.marketplace.repository.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class CustomUserDetailsService implements UserDetailsService, CommandLineRunner, UserDetailsManager {

    @Autowired
    @Qualifier("inMemManager")
    private UserDetailsManager inMemManager;
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${marketplace.security.admin.password:openbaton}")
    private String adminPwd;
    @Value("${nfvo.security.project.name:default}")
    private String projectDefaultName;
    @Autowired private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return inMemManager.loadUserByUsername(username);
    }

    @Override
    public void run(String... args) throws Exception {

        log.debug("Creating initial Users...");

        if (!inMemManager.userExists("admin")) {
            UserDetails admin = new org.springframework.security.core.userdetails.User("admin", BCrypt.hashpw(adminPwd, BCrypt.gensalt(12)), true, true, true, true, AuthorityUtils.createAuthorityList("ADMIN"));
            inMemManager.createUser(admin);
        } else {
            log.debug("Admin" + inMemManager.loadUserByUsername("admin"));
        }
        for (User user : userRepository.findAll()) {
            if (!user.getUsername().equals("admin") && !user.getUsername().equals("guest")) {
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), true, true, true, true, AuthorityUtils.createAuthorityList("USER"));
                inMemManager.createUser(userDetails);
            }
        }

        log.debug("Users in UserDetailManager: ");
        log.info("ADMIN: " + inMemManager.loadUserByUsername("admin"));
    }

    @Override
    public void createUser(UserDetails user) {
        this.inMemManager.createUser(user);
    }

    @Override
    public void updateUser(UserDetails user) {
        inMemManager.updateUser(user);
    }

    @Override
    public void deleteUser(String username) {
        inMemManager.deleteUser(username);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        inMemManager.changePassword(oldPassword, newPassword);
    }

    @Override
    public boolean userExists(String username) {
        return inMemManager.userExists(username);
    }
}

