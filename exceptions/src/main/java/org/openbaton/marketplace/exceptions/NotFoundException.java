package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 16/08/16.
 */
public class NotFoundException extends Exception{

  public NotFoundException() {
  }

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public NotFoundException(Throwable cause) {
    super(cause);
  }
}
