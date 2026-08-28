package com.tomrick.mygallery.photo.admin.application;

public final class InvalidUploadDeclarationException extends RuntimeException {

    public InvalidUploadDeclarationException() {
        super("Invalid upload declaration");
    }
}
