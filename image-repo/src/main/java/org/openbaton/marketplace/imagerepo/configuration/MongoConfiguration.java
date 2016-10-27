package org.openbaton.marketplace.imagerepo.configuration;

import com.mongodb.MongoClientURI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * Created by lto on 03/08/16.
 */
@Configuration
public class MongoConfiguration {

  @Value("${spring.data.mongodb.username}") private String mongoUser;
  @Value("${spring.data.mongodb.password}") private String mongoPass;
  @Value("${spring.data.mongodb.host}") private String mongoHost;
  @Value("${spring.data.mongodb.database}") private String databaseName;
  @Value("${spring.data.mongodb.port}") private int mongoPort;

  @Bean
  public MongoDbFactory mongoDbFactory() throws Exception {

    // Set credentials
    //    MongoCredential credential = MongoCredential.createMongoCRCredential(mongoUser, databaseName, mongoPass
    // .toCharArray());
    //    ServerAddress serverAddress = new ServerAddress(mongoHost, mongoPort);

    // Mongo Client
    MongoClientURI
        mongoClientURI =
        new MongoClientURI("mongodb://" +
                           mongoUser +
                           ":" +
                           mongoPass +
                           "@" +
                           mongoHost +
                           ":" +
                           mongoPort +
                           "/" +
                           databaseName);
    //    MongoClient mongoClient = new MongoClient(mongoClientURI);

    // Mongo DB Factory
    SimpleMongoDbFactory simpleMongoDbFactory = new SimpleMongoDbFactory(mongoClientURI);

    return simpleMongoDbFactory;
  }

  /**
   * Template ready to use to operate on the database
   *
   * @return Mongo Template ready to use
   */
  @Bean
  public MongoTemplate mongoTemplate() throws Exception {
    return new MongoTemplate(mongoDbFactory());
  }

  @Bean
  public GridFsTemplate gridFsTemplate() throws Exception {
    return new GridFsTemplate(mongoDbFactory(), mongoTemplate().getConverter());
  }
}
