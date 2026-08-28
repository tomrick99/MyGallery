package com.tomrick.mygallery.common.web;

public record ApiErrorResponse(String code, String message, String requestId) {
}
