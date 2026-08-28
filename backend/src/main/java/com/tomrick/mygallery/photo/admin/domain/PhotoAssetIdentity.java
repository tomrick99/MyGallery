package com.tomrick.mygallery.photo.admin.domain;

import java.util.UUID;

public record PhotoAssetIdentity(
        UUID photoId,
        String cloudinaryPublicId
) {
}
