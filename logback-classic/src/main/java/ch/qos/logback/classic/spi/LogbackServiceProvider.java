package ch.qos.logback.classic.spi;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;

public class LogbackServiceProvider implements SLF4JServiceProvider {

  final static String NULL_CS_URL = CoreConstants.CODES_URL + "#null_CS";

  /**
   * Declare the version of the SLF4J API this implementation is compiled against.
   * The value of this field is modified with each major release.
   */
  // to avoid constant folding by the compiler, this field must *not* be final
  public static String REQUESTED_API_VERSION = "1.8.99"; // !final

  private LoggerContext defaultLoggerContext;
  private IMarkerFactory markerFactory;
  private MDCAdapter mdcAdapter;

//  private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();
//  private static Object KEY = new Object();
//  private volatile boolean initialized = false;

  /**
   * 初始化slf4j实际绑定的Logback日志框架相关组件(LoggerContext、MarkerFactory、MDCAdaptor)
   */
  @Override
  public void initialize() {

    //创建Logback日志上下文实例,设置上下文名称
    defaultLoggerContext = new LoggerContext();
    defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);

    //解析Logback配置文件,根据解析结果组装并初始化日志上下文实例
    initializeLoggerContext();

    //使用slf4j的BasicMarkerFactory
    markerFactory = new BasicMarkerFactory();

    //使用Logback的MDCAdapter扩展实现
    mdcAdapter = new LogbackMDCAdapter();

    //initialized = true;
  }

  /**
   * 初始化Logback日志上下文
   */
  private void initializeLoggerContext() {
    try {
      try {
        //自动按默认优先级顺序解析Logback配置文件,实现日志上下文初始化
        new ContextInitializer(defaultLoggerContext).autoConfig();
      } catch (JoranException je) {
        Util.report("Failed to auto configure default logger context", je);
      }
      // logback-292
      if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
        StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
      }

      // contextSelectorBinder.init(defaultLoggerContext, KEY);

    } catch (Exception t) { // see LOGBACK-1159
      Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
    }
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return defaultLoggerContext;

    //FIXME: 2018年在commit-id=6660751088197e633ea55f8f46968e3bf99bd43c中删除了以下逻辑
//    if (!initialized) {
//      return defaultLoggerContext;
//
//
//      if (contextSelectorBinder.getContextSelector() == null) {
//        throw new IllegalStateException("contextSelector cannot be null. See also " + NULL_CS_URL);
//      }
//      return contextSelectorBinder.getContextSelector().getLoggerContext();
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  /**
   * 返回MDC适配器
   *
   * @return
   */
  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  /**
   * 返回请求API版本
   *
   * @return
   */
  @Override
  public String getRequesteApiVersion() {
    return REQUESTED_API_VERSION;
  }

}
