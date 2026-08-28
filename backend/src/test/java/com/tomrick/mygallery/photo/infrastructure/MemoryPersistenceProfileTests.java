package com.tomrick.mygallery.photo.infrastructure;

import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.PhotoAssetGateway;
import com.tomrick.mygallery.photo.domain.PhotoRepository;
import com.tomrick.mygallery.photo.infrastructure.media.InMemoryPhotoAssetGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MemoryPersistenceProfileTests {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AdminPhotoRepository adminPhotoRepository;

    @Autowired
    private PhotoAssetGateway photoAssetGateway;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void defaultProfileUsesMemoryRepositoryWithoutADataSource() {
        assertInstanceOf(InMemoryPhotoRepository.class, photoRepository);
        assertInstanceOf(InMemoryPhotoRepository.class, adminPhotoRepository);
        assertInstanceOf(InMemoryPhotoAssetGateway.class, photoAssetGateway);
        assertTrue(applicationContext.getBeansOfType(DataSource.class).isEmpty());
    }
}
