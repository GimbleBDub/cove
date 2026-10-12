package bw.co.gimble.cove.properties;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.properties.dto.PropertyTypeResponse;
import bw.co.gimble.cove.properties.dto.UpsertPropertyTypeRequest;
import bw.co.gimble.cove.security.AuthSupport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyTypeService {

    private static final Map<String, String> SEED = new LinkedHashMap<>();
    static {
        SEED.put("HOUSE", "House");
        SEED.put("BUNGALOW", "Bungalow");
        SEED.put("TOWNHOUSE", "Townhouse");
        SEED.put("APARTMENT", "Apartment");
        SEED.put("FLAT", "Flat");
        SEED.put("ROOM", "Room");
        SEED.put("PLOT", "Plot");
        SEED.put("COMMERCIAL", "Commercial");
        SEED.put("FARM", "Farm");
    }

    private final PropertyTypeRepository propertyTypeRepository;
    private final PermissionService permissionService;

    @PostConstruct
    public void seed() {
        SEED.forEach((code, label) -> {
            if (!propertyTypeRepository.existsByCodeIgnoreCase(code)) {
                propertyTypeRepository.save(PropertyType.builder()
                        .code(code)
                        .label(label)
                        .active(true)
                        .build());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<PropertyTypeResponse> listActive() {
        return propertyTypeRepository.findByActiveTrueOrderByLabelAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<PropertyTypeResponse> adminListAll() {
        requirePlatformAdmin();
        return propertyTypeRepository.findAllByOrderByLabelAsc().stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public PropertyType requireActive(UUID id) {
        PropertyType type = propertyTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.PROPERTY_TYPE_NOT_FOUND, "Property type not found"));
        if (!type.isActive()) {
            throw ApiException.badRequest(ErrorCode.PROPERTY_TYPE_NOT_FOUND, "This property type is no longer available");
        }
        return type;
    }

    public PropertyTypeResponse adminCreate(UpsertPropertyTypeRequest request) {
        requirePlatformAdmin();
        String code = request.code().trim().toUpperCase().replace(' ', '_');
        if (propertyTypeRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict(ErrorCode.PROPERTY_TYPE_CODE_TAKEN, "A property type with this code already exists");
        }
        PropertyType saved = propertyTypeRepository.save(PropertyType.builder()
                .code(code)
                .label(request.label().trim())
                .active(request.active() == null || request.active())
                .build());
        return map(saved);
    }

    public PropertyTypeResponse adminUpdate(UUID id, UpsertPropertyTypeRequest request) {
        requirePlatformAdmin();
        PropertyType type = propertyTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.PROPERTY_TYPE_NOT_FOUND, "Property type not found"));
        String code = request.code().trim().toUpperCase().replace(' ', '_');
        propertyTypeRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.conflict(ErrorCode.PROPERTY_TYPE_CODE_TAKEN, "A property type with this code already exists");
                });
        type.setCode(code);
        type.setLabel(request.label().trim());
        if (request.active() != null) {
            type.setActive(request.active());
        }
        return map(type);
    }

    private void requirePlatformAdmin() {
        permissionService.requireStaff(false, bw.co.gimble.cove.staff.StaffPermission.CATALOGUE);
    }

    private PropertyTypeResponse map(PropertyType t) {
        return new PropertyTypeResponse(t.getId(), t.getCode(), t.getLabel(), t.isActive());
    }
}
