package com.ucorealty.config;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * Profile h2 only: starts H2 in memory inside this application, before the database connection is opened. The two apps
 * share it: whichever starts first runs the server (port 9092), the other finds the port taken and simply connects.
 * Local connections only. Also opens the H2 web console on port 8082.
 */
@Component
@Profile("h2")
public class H2ServerStarter implements BeanFactoryPostProcessor {
  private static final Logger log = LoggerFactory.getLogger(H2ServerStarter.class);

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    try {
      Server.createTcpServer("-tcpPort", "9092", "-ifNotExists").start();
      log.info("[UCO H2] In-memory database server started on port 9092");
    } catch (SQLException e) {
      log.info("[UCO H2] Port 9092 is taken: using the H2 server already running (the other app)");
    }
    try {
      Server.createWebServer("-webPort", "8082").start();
      log.info("[UCO H2] Web console: http://localhost:8082");
    } catch (SQLException e) {
      log.info("[UCO H2] Port 8082 is taken: web console already running");
    }
  }
}
