package ch.qos.logback.core.model;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Abstract representation of configuration elements
 *
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.3.0
 */
//配置元素抽象表示
public class Model {

  // this state should not be here but should be treated via listeners
  // between processors and ModelHandlers
  boolean handled = false;

  public boolean isUnhandled() {
    return !handled;
  }

  public void markAsHandled() {
    this.handled = true;
  }

  String tag; //标签名
  String bodyText; //标签内容
  int lineNumber; //标签行号

  List<Model> subModels = new ArrayList<>();

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public List<Model> getSubModels() {
    return subModels;
  }

  public void addSubModel(Model m) {
    subModels.add(m);
  }

  public String getBodyText() {
    return bodyText;
  }

  public void addText(String bodytext) {
    if (bodyText == null)
      this.bodyText = bodytext;
    else
      this.bodyText += bodytext;
  }

  public String idString() {
    return "<" + tag + "> at line " + lineNumber;
  }


  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [tag=" + tag + ", bodyText=" + bodyText + "]";
  }


}