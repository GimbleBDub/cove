package bw.co.gimble.cove.locations;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.locations.dto.LocationDtos.*;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.staff.StaffPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationService {

    private final CityRepository cityRepository;
    private final AreaRepository areaRepository;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    /** A resolved, validated city + area pair. */
    public record Place(City city, Area area) {}

    @Transactional(readOnly = true)
    public List<CityResponse> publicCatalogue() {
        return catalogue(false);
    }

    @Transactional(readOnly = true)
    public List<CityResponse> adminCatalogue() {
        permissionService.requireStaff(true, StaffPermission.CATALOGUE);
        return catalogue(true);
    }

    private List<CityResponse> catalogue(boolean includeInactive) {
        return cityRepository.findAll().stream()
                .filter(c -> includeInactive || c.isActive())
                .sorted(Comparator.comparing(c -> c.getName().toLowerCase()))
                .map(c -> map(c, includeInactive))
                .toList();
    }

    public CityResponse createCity(CityRequest request) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.CATALOGUE);
        String name = clean(request.name());
        if (cityRepository.findByNameIgnoreCase(name).isPresent()) {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, name + " already exists");
        }
        City city = cityRepository.save(City.builder()
                .name(name)
                .district(blankToNull(request.district()))
                .active(request.active() == null || request.active())
                .build());
        auditLogService.record("CITY", city.getId(), "CREATED", null, name, actor, null);
        return map(city, true);
    }

    public CityResponse updateCity(UUID cityId, CityRequest request) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.CATALOGUE);
        City city = requireCity(cityId);
        String name = clean(request.name());
        cityRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(cityId))
                .ifPresent(other -> { throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, name + " already exists"); });
        String before = city.getName() + (city.isActive() ? "" : " (inactive)");
        city.setName(name);
        city.setDistrict(blankToNull(request.district()));
        if (request.active() != null) city.setActive(request.active());
        auditLogService.record("CITY", cityId, "UPDATED", before, name + (city.isActive() ? "" : " (inactive)"), actor, null);
        return map(city, true);
    }

    public CityResponse addAreas(UUID cityId, AddAreasRequest request) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.CATALOGUE);
        City city = requireCity(cityId);
        for (String raw : request.names()) {
            String name = clean(raw);
            if (name.isEmpty() || areaRepository.findByCityIdAndNameIgnoreCase(cityId, name).isPresent()) continue;
            Area area = areaRepository.save(Area.builder().cityId(cityId).name(name).build());
            auditLogService.record("AREA", area.getId(), "CREATED", null, city.getName() + " / " + name, actor, null);
        }
        return map(city, true);
    }

    public CityResponse updateArea(UUID areaId, AreaRequest request) {
        UUID actor = permissionService.requireStaff(false, StaffPermission.CATALOGUE);
        Area area = areaRepository.findById(areaId).orElseThrow(() -> ApiException.notFound("Area not found"));
        String name = clean(request.name());
        areaRepository.findByCityIdAndNameIgnoreCase(area.getCityId(), name)
                .filter(other -> !other.getId().equals(areaId))
                .ifPresent(other -> { throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, name + " already exists here"); });
        String before = area.getName() + (area.isActive() ? "" : " (inactive)");
        area.setName(name);
        if (request.active() != null) area.setActive(request.active());
        auditLogService.record("AREA", areaId, "UPDATED", before, name + (area.isActive() ? "" : " (inactive)"), actor, null);
        return map(requireCity(area.getCityId()), true);
    }

    /** For new properties/edits: both must exist, be active, and belong together. */
    @Transactional(readOnly = true)
    public Place resolve(UUID cityId, UUID areaId) {
        if (cityId == null || areaId == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Choose a city/town and an area from the list");
        }
        City city = requireCity(cityId);
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "That area is not available"));
        if (!area.getCityId().equals(city.getId())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, area.getName() + " is not in " + city.getName());
        }
        if (!city.isActive() || !area.isActive()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "We don't list in " + area.getName() + ", " + city.getName() + " yet");
        }
        return new Place(city, area);
    }

    /** Integration API convenience: resolve by names (case-insensitive). */
    @Transactional(readOnly = true)
    public Place resolveByName(String cityName, String areaName) {
        City city = cityRepository.findByNameIgnoreCase(clean(cityName == null ? "" : cityName))
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Unknown city '" + cityName + "'. See GET /reference/locations"));
        Area area = areaRepository.findByCityIdAndNameIgnoreCase(city.getId(), clean(areaName == null ? "" : areaName))
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Unknown area '" + areaName + "' in " + city.getName() + ". See GET /reference/locations"));
        return resolve(city.getId(), area.getId());
    }

    private City requireCity(UUID cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "That city/town is not available"));
    }

    private CityResponse map(City c, boolean includeInactive) {
        List<AreaResponse> areas = new ArrayList<>();
        for (Area a : areaRepository.findByCityIdOrderByNameAsc(c.getId())) {
            if (includeInactive || a.isActive()) areas.add(new AreaResponse(a.getId(), a.getName(), a.isActive()));
        }
        return new CityResponse(c.getId(), c.getName(), c.getDistrict(), c.isActive(), areas);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
