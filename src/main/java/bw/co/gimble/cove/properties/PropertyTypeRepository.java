package bw.co.gimble.cove.properties;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyTypeRepository extends JpaRepository<PropertyType, UUID> {
    Optional<PropertyType> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<PropertyType> findByActiveTrueOrderByLabelAsc();
    List<PropertyType> findAllByOrderByLabelAsc();
}
