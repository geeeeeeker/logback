/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 * <p>
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 * <p>
 * or (per the licensee's choosing)
 * <p>
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusListenerConfigHelper;

// contributors
// Ted Graham, Matt Fowles, see also http://jira.qos.ch/browse/LBCORE-32

/**
 * This class contains logback's logic for automatic configuration
 *
 * @author Ceki Gulcu
 */
//实现Logback自动配置功能
public class ContextInitializer {

  //目前支持groovy、xml两种格式的配置文件
  final public static String GROOVY_AUTOCONFIG_FILE = "logback.groovy";
  final public static String AUTOCONFIG_FILE = "logback.xml";
  final public static String TEST_AUTOCONFIG_FILE = "logback-test.xml";

  /**
   * @deprecated Please use ClassicConstants.CONFIG_FILE_PROPERTY instead
   */
  final public static String CONFIG_FILE_PROPERTY = ClassicConstants.CONFIG_FILE_PROPERTY;

  final LoggerContext loggerContext;

  public ContextInitializer(LoggerContext loggerContext) {
    this.loggerContext = loggerContext;
  }

  /**
   * 依据资源URL处理Logback配置文件
   *
   * @param url
   * @throws JoranException
   */
  public void configureByResource(URL url) throws JoranException {
    if (url == null) {
      throw new IllegalArgumentException("URL argument cannot be null");
    }

    final String urlString = url.toString();

    if (urlString.endsWith("groovy")) {
      if (EnvUtil.isGroovyAvailable()) {
        // avoid directly referring to GafferConfigurator so as to avoid
        // loading groovy.lang.GroovyObject . See also http://jira.qos.ch/browse/LBCLASSIC-214
        //GafferUtil.runGafferConfiguratorOn(loggerContext, this, url);

        StatusManager sm = loggerContext.getStatusManager();
        sm.add(new ErrorStatus("Groovy configuration disabled due to Java 9 compilation issues.", loggerContext));

      } else {
        StatusManager sm = loggerContext.getStatusManager();
        sm.add(new ErrorStatus("Groovy classes are not available on the class path. ABORTING INITIALIZATION.", loggerContext));
      }
    } else if (urlString.endsWith("xml")) {

//      JoranConfigurator configurator = new JoranConfigurator();
//      configurator.setContext(loggerContext);
//      configurator.doConfigure(url);
      this.joranConfigureByResource(url);
    } else {
      throw new LogbackException("Unexpected filename extension of file [" + url.toString() + "]. Should be either .groovy or .xml");
    }
  }

  /**
   * XML配置文件处理委托给Joran框架
   *
   * @param url
   * @throws JoranException
   */
  void joranConfigureByResource(URL url) throws JoranException {
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(loggerContext);
    configurator.doConfigure(url);
  }

  /**
   * 启动时-Dlogback.configurationFile指定logback.xml加载路径
   *
   * @param classLoader
   * @param updateStatus
   * @return
   */
  private URL findConfigFileURLFromSystemProperties(ClassLoader classLoader, boolean updateStatus /*是否更新内部状态*/) {
    String logbackConfigFile = OptionHelper.getSystemProperty(CONFIG_FILE_PROPERTY);
    if (logbackConfigFile != null) {
      URL result = null;
      try {
        result = new URL(logbackConfigFile);
        return result;
      } catch (MalformedURLException e) {
        // so, resource is not a URL:
        // attempt to get the resource from the class path
        result = Loader.getResource(logbackConfigFile, classLoader);
        if (result != null) {
          return result;
        }

        //若指定路径下不存在logback配置文件则用户新建该文件
        File f = new File(logbackConfigFile);
        if (f.exists() && f.isFile()) {
          try {
            result = f.toURI().toURL();
            return result;
          } catch (MalformedURLException e1) {
          }
        }
      } finally {
        if (updateStatus) {
          statusOnResourceSearch(logbackConfigFile, classLoader, result);
        }
      }
    }
    return null;
  }

  public URL findURLOfDefaultConfigurationFile(boolean updateStatus) {
    ClassLoader myClassLoader = Loader.getClassLoaderOfObject(this);

    //优先使用启动命令行参数-Dlogback.configurationFile进行文件资源加载
    URL url = findConfigFileURLFromSystemProperties(myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    //其次选择加载logback-test.xml文件资源
    url = getResource(TEST_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    //然后选择加载logback.groovy文件资源
    url = getResource(GROOVY_AUTOCONFIG_FILE, myClassLoader, updateStatus);
    if (url != null) {
      return url;
    }

    //最后才选择加载logback.xml文件资源
    return getResource(AUTOCONFIG_FILE, myClassLoader, updateStatus);
  }


  /**
   *
   *
   * @param filename
   * @param myClassLoader
   * @param updateStatus
   * @return
   */
  private URL getResource(String filename, ClassLoader myClassLoader, boolean updateStatus) {
    URL url = Loader.getResource(filename, myClassLoader);
    if (updateStatus) {
      statusOnResourceSearch(filename, myClassLoader, url);
    }
    return url;
  }

  /**
   *
   * @throws JoranException
   */
  public void autoConfig() throws JoranException {
    StatusListenerConfigHelper.installIfAsked(loggerContext);
    URL url = findURLOfDefaultConfigurationFile(true);
    if (url != null) {
      configureByResource(url);
    } else {
      //
      Configurator c = EnvUtil.loadFromServiceLoader(Configurator.class);
      if (c != null) {
        try {
          c.setContext(loggerContext);
          c.configure(loggerContext);
        } catch (Exception e) {
          throw new LogbackException(String.format("Failed to initialize Configurator: %s using ServiceLoader", c != null ? c.getClass()
            .getCanonicalName() : "null"), e);
        }
      } else {
        BasicConfigurator basicConfigurator = new BasicConfigurator();
        basicConfigurator.setContext(loggerContext);
        basicConfigurator.configure(loggerContext);
      }
    }
  }

  private void statusOnResourceSearch(String resourceName, ClassLoader classLoader, URL url) {
    StatusManager sm = loggerContext.getStatusManager();
    if (url == null) {
      sm.add(new InfoStatus("Could NOT find resource [" + resourceName + "]", loggerContext));
    } else {
      sm.add(new InfoStatus("Found resource [" + resourceName + "] at [" + url.toString() + "]", loggerContext));
      multiplicityWarning(resourceName, classLoader);
    }
  }

  private void multiplicityWarning(String resourceName, ClassLoader classLoader) {
    Set<URL> urlSet = null;
    StatusManager sm = loggerContext.getStatusManager();
    try {
      urlSet = Loader.getResources(resourceName, classLoader);
    } catch (IOException e) {
      sm.add(new ErrorStatus("Failed to get url list for resource [" + resourceName + "]", loggerContext, e));
    }
    if (urlSet != null && urlSet.size() > 1) {
      sm.add(new WarnStatus("Resource [" + resourceName + "] occurs multiple times on the classpath.", loggerContext));
      for (URL url : urlSet) {
        sm.add(new WarnStatus("Resource [" + resourceName + "] occurs at [" + url.toString() + "]", loggerContext));
      }
    }
  }
}
