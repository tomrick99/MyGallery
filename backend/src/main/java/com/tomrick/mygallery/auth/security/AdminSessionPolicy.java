package com.tomrick.mygallery.auth.security;

import java.time.Duration;

public record AdminSessionPolicy(Duration absoluteTimeout) {
}
