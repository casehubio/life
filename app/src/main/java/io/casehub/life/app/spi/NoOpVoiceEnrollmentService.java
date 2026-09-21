package io.casehub.life.app.spi;

import io.casehub.life.api.spi.VoiceEnrollmentService;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@DefaultBean
@ApplicationScoped
public class NoOpVoiceEnrollmentService implements VoiceEnrollmentService {
    @Override public void enrollVoice(UUID memberId, byte[] voiceSample) {}
    @Override public boolean isEnrolled(UUID memberId) { return false; }
    @Override public Optional<UUID> identifyByVoice(byte[] voiceSample) { return Optional.empty(); }
}
