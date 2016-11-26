package org.openbaton.marketplace.exceptions;

/**
 * Created by lto on 24/11/16.
 */
public class PackageIntegrityException extends Exception {
  public PackageIntegrityException(String message) {
    super(message);
  }

  public PackageIntegrityException(String message, Throwable cause) {
    super(message, cause);
  }

  public PackageIntegrityException(Throwable cause) {
    super(cause);
  }
}
