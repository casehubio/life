package io.casehub.life.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LifeCalendarConflictStore {

    LifeCalendarConflict save(LifeCalendarConflict conflict);

    Optional<LifeCalendarConflict> findById(UUID id);

    List<LifeCalendarConflict> findByMapping(UUID mappingId);

    List<LifeCalendarConflict> findUnresolved();

    long countUnresolved();

    Optional<LifeCalendarConflict> findUnresolvedByMapping(UUID mappingId);

    void deleteByMapping(UUID mappingId);
}
