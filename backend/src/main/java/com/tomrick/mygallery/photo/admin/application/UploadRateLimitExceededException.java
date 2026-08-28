package com.tomrick.mygallery.photo.admin.application;

public final class UploadRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public UploadRateLimitExceededException(long retryAfterSeconds) {
        super("Too many upload requests");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
