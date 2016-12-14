package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 14/12/16.
 */
public class FailedToUploadException extends Exception {
  public FailedToUploadException(String message) {
    super(message);
  }

  public FailedToUploadException(String message, Throwable cause) {
    super(message, cause);
  }

  public FailedToUploadException(Throwable cause) {
    super(cause);
  }
}
