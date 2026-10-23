package dev.catdoc.api.application;

/** パラメータの組み合わせが成り立たない。400 にあたる。 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
