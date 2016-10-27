package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 18/08/16.
 */
public class NumberOfImageExceededException extends Exception {
  public NumberOfImageExceededException(String message) {
    super(message);
  }

  public NumberOfImageExceededException(String message, Throwable cause) {
    super(message, cause);
  }

  public NumberOfImageExceededException(Throwable cause) {
    super(cause);
  }
}
