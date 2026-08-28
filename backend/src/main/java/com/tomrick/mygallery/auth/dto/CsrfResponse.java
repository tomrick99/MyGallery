package com.tomrick.mygallery.auth.dto;

public record CsrfResponse(String headerName, String token) {
}
