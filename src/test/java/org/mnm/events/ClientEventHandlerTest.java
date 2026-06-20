package org.mnm.events;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientEventHandlerTest {

    private ClientEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = ClientEventHandler.getInstance();
        eventHandler.clear();
    }

    @Test
    void shouldRegisterAndCallListener() {
        final var ref1 = new AtomicInteger();
        final var ref2 = new AtomicInteger();
        eventHandler.register(new FilesValidationListener() {
            @Override
            public void validationStart(int filesCount) {
                ref1.set(filesCount);
            }

            @Override
            public void fileValidated() {
                ref2.set(99);
            }
        });

        eventHandler.validationStart(24);
        eventHandler.fileValidated();

        assertThat(ref1.get()).isEqualTo(24);
        assertThat(ref2.get()).isEqualTo(99);
    }

}
