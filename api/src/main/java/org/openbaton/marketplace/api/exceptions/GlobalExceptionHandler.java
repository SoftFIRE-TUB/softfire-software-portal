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

package org.openbaton.marketplace.api.exceptions;

import org.openbaton.exceptions.NotFoundException;
import org.openbaton.marketplace.exceptions.ImageRepositoryNotEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


/**
 * Created by gca on 27/08/15.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler({NotFoundException.class, ImageRepositoryNotEnabled.class})
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    protected ResponseEntity<Object> handleNotFoundException(HttpServletRequest req, Exception e) {
        log.error("Exception with message " + e.getMessage() + " was thrown");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map body = new HashMap<>();
        body.put("error", "Not Found");
        body.put("exception", e.getClass().toString());
        body.put("message", e.getMessage());
        body.put("path", req.getRequestURI());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("timestamp", new Date().getTime());
        ResponseEntity responseEntity = new ResponseEntity(body, headers, HttpStatus.NOT_FOUND);
        return responseEntity;
    }
}
