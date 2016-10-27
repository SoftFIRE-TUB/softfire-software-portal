package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 03/08/16.
 */
public class ImageRepositoryNotEnabled extends Exception {
  public ImageRepositoryNotEnabled(String message) {
    super(message);
  }

  public ImageRepositoryNotEnabled(String message, Throwable cause) {
    super(message, cause);
  }

  public ImageRepositoryNotEnabled(Throwable cause) {
    super(cause);
  }
}
