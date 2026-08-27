package com.tomrick.mygallery.photo.infrastructure;

import com.tomrick.mygallery.photo.domain.PhotoRepository;
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
    private ApplicationContext applicationContext;

    @Test
    void defaultProfileUsesMemoryRepositoryWithoutADataSource() {
        assertInstanceOf(InMemoryPhotoRepository.class, photoRepository);
        assertTrue(applicationContext.getBeansOfType(DataSource.class).isEmpty());
    }
}
