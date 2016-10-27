package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 18/08/16.
 */
public class NotAuthorizedException extends Exception {
  public NotAuthorizedException(String message) {
    super(message);
  }

  public NotAuthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotAuthorizedException(Throwable cause) {
    super(cause);
  }
}
