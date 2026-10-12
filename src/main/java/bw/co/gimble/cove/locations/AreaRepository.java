package bw.co.gimble.cove.locations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    List<Area> findByCityIdOrderByNameAsc(UUID cityId);
    Optional<Area> findByCityIdAndNameIgnoreCase(UUID cityId, String name);
}
