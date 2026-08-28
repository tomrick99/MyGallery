package com.tomrick.mygallery.auth.dto;

public record AdminSessionResponse(boolean authenticated, String username) {
}
