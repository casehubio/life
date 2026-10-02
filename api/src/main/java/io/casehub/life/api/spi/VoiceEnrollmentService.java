package io.casehub.life.api.spi;

import java.util.Optional;
import java.util.UUID;

public interface VoiceEnrollmentService {
    void enrollVoice(UUID memberId, byte[] voiceSample);
    boolean isEnrolled(UUID memberId);
    Optional<UUID> identifyByVoice(byte[] voiceSample);
}
