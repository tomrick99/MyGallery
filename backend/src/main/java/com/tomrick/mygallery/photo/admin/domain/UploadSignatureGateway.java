package com.tomrick.mygallery.photo.admin.domain;

import java.util.Map;

public interface UploadSignatureGateway {

    String sign(Map<String, Object> parameters);
}
