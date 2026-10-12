package bw.co.gimble.cove.company;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.AuditLogService;
import bw.co.gimble.cove.common.ErrorCode;
import bw.co.gimble.cove.company.CompanyDtos.*;
import bw.co.gimble.cove.integration.OutboundUrlPolicy;
import bw.co.gimble.cove.leads.Lead;
import bw.co.gimble.cove.leads.LeadRepository;
import bw.co.gimble.cove.leads.LeadStage;
import bw.co.gimble.cove.listings.ListingService;
import bw.co.gimble.cove.listings.ListingStatus;
import bw.co.gimble.cove.listings.dto.ListingResponse;
import bw.co.gimble.cove.organisations.Organisation;
import bw.co.gimble.cove.organisations.OrganisationRepository;
import bw.co.gimble.cove.permission.PermissionService;
import bw.co.gimble.cove.profile.models.UserProfile;
import bw.co.gimble.cove.profile.repositories.UserProfileRepository;
import bw.co.gimble.cove.properties.PropertyService;
import bw.co.gimble.cove.role.Role;
import bw.co.gimble.cove.role.RoleStatus;
import bw.co.gimble.cove.role.UserRoleRepository;
import bw.co.gimble.cove.role.UserRoleService;
import bw.co.gimble.cove.security.AuthSupport;
import bw.co.gimble.cove.security.TokenHasher;
import bw.co.gimble.cove.staff.TemporaryPasswords;
import bw.co.gimble.cove.user.User;
import bw.co.gimble.cove.user.UserRepository;
import bw.co.gimble.cove.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** The company portal: what members see, and what company admins manage. */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private static final Set<Role> INVITABLE = EnumSet.of(
            Role.AGENCY_ADMIN, Role.AGENT, Role.PROPERTY_MANAGER, Role.ORG_STAFF, Role.VALUER, Role.AUCTIONEER);

    private final OrganisationRepository organisationRepository;
    private final PermissionService permissionService;
    private final ListingService listingService;
    private final PropertyService propertyService;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;
    private final WebhookEndpointRepository webhookRepository;
    private final TokenHasher tokenHasher;
    private final OutboundUrlPolicy urlPolicy;
    private final AuditLogService auditLogService;

    // ─── Access ──────────────────────────────────────────────────

    public UUID requireMember(UUID organisationId) {
        UUID actor = AuthSupport.requireUserId();
        if (!permissionService.isOrgMember(actor, organisationId)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "You are not a member of this company");
        }
        return actor;
    }

    public UUID requireAdmin(UUID organisationId) {
        UUID actor = AuthSupport.requireUserId();
        if (!permissionService.hasOrgRole(actor, organisationId, Role.AGENCY_ADMIN)) {
            throw ApiException.forbidden(ErrorCode.ORG_ROLE_FORBIDDEN, "Only a company admin can do this");
        }
        return actor;
    }

    private Organisation requireOrg(UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    // ─── Overview, listings, enquiries ───────────────────────────

    @Transactional(readOnly = true)
    public OverviewResponse overview(UUID organisationId) {
        UUID actor = requireMember(organisationId);
        Organisation org = requireOrg(organisationId);
        List<ListingResponse> listings = listingService.forOrganisation(organisationId);
        List<Lead> leads = leadsFor(listings);
        Instant month = Instant.now().minus(30, ChronoUnit.DAYS);
        return new OverviewResponse(
                org.getId(), org.getName(), org.getVerificationStatus().name(), org.getLogoUrl(),
                org.getTypes().stream().map(Enum::name).toList(),
                permissionService.hasOrgRole(actor, organisationId, Role.AGENCY_ADMIN),
                userRoleRepository.countDistinctUsersByOrganisationIdAndStatus(organisationId, RoleStatus.ACTIVE),
                propertyService.byManagingOrganisation(organisationId).size(),
                listings.stream().filter(l -> l.status() == ListingStatus.PUBLISHED).count(),
                listings.stream().filter(l -> l.status() == ListingStatus.PENDING_REVIEW).count(),
                listings.stream().filter(l -> l.status() == ListingStatus.REJECTED || l.status() == ListingStatus.TAKEN_DOWN).count(),
                leads.stream().filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(month)).count(),
                leads.stream().filter(l -> l.getStage() == LeadStage.NEW).count());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> listings(UUID organisationId) {
        requireMember(organisationId);
        return listingService.forOrganisation(organisationId);
    }

    @Transactional(readOnly = true)
    public List<CompanyLeadResponse> leads(UUID organisationId, Instant since) {
        requireMember(organisationId);
        return leadResponses(organisationId, since);
    }

    /** Shared with the integration API (no member check there; the key is already scoped). */
    @Transactional(readOnly = true)
    public List<CompanyLeadResponse> leadResponses(UUID organisationId, Instant since) {
        List<ListingResponse> listings = listingService.forOrganisation(organisationId);
        Map<UUID, ListingResponse> byId = new HashMap<>();
        listings.forEach(l -> byId.put(l.id(), l));
        return leadsFor(listings).stream()
                .filter(l -> since == null || (l.getCreatedAt() != null && l.getCreatedAt().isAfter(since)))
                .sorted(Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(l -> {
                    ListingResponse listing = byId.get(l.getListingId());
                    User inquirer = userRepository.findById(l.getInquirerUserId()).orElse(null);
                    String name = profileRepository.findByUserId(l.getInquirerUserId()).map(UserProfile::getFullName).orElse(null);
                    String title = listing == null ? "Listing"
                            : (listing.typeLabel() == null ? "Property" : listing.typeLabel())
                            + (listing.suburb() == null ? "" : " in " + listing.suburb());
                    return new CompanyLeadResponse(l.getId(), l.getListingId(), title, name,
                            inquirer == null ? null : inquirer.getEmail(),
                            inquirer == null ? null : inquirer.getPhone(),
                            l.getStage().name(), l.getAssignedAgentUserId(), l.getCreatedAt());
                })
                .toList();
    }

    private List<Lead> leadsFor(List<ListingResponse> listings) {
        List<Lead> out = new ArrayList<>();
        for (ListingResponse l : listings) out.addAll(leadRepository.findByListingIdOrderByCreatedAtDesc(l.id()));
        return out;
    }

    // ─── Team ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MemberResponse> members(UUID organisationId) {
        requireMember(organisationId);
        Map<UUID, List<bw.co.gimble.cove.role.UserRole>> byUser = new LinkedHashMap<>();
        userRoleRepository.findByOrganisationIdAndStatus(organisationId, RoleStatus.ACTIVE)
                .forEach(r -> byUser.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r));
        return byUser.entrySet().stream().map(e -> {
            User u = userRepository.findById(e.getKey()).orElse(null);
            String name = profileRepository.findByUserId(e.getKey()).map(UserProfile::getFullName).orElse(null);
            Instant joined = e.getValue().stream().map(bw.co.gimble.cove.role.UserRole::getGrantedAt)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            return new MemberResponse(e.getKey(), name, u == null ? null : u.getEmail(), u == null ? null : u.getPhone(),
                    e.getValue().stream().map(r -> new MemberRole(r.getId(), r.getRole().name())).toList(), joined);
        }).sorted(Comparator.comparing(MemberResponse::joinedAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    /** Creates an account for a new colleague with a temporary password (shown once). */
    public InviteMemberResponse invite(UUID organisationId, InviteMemberRequest request) {
        UUID actor = requireAdmin(organisationId);
        if (!INVITABLE.contains(request.role())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "That role can't be given to a company member");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "That person already has a Cove account. Ask them to request to join your company from their Settings.");
        }
        String temporary = TemporaryPasswords.generate();
        User user = userRepository.save(User.builder()
                .email(email)
                .phone(request.phone().trim())
                .passwordHash(passwordEncoder.encode(temporary))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .passwordChangeRequired(true)
                .build());
        profileRepository.save(UserProfile.builder()
                .user(user)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .build());
        userRoleService.assignOrgRole(organisationId, user.getId(), request.role());
        auditLogService.record("ORGANISATION", organisationId, "MEMBER_INVITED", null, request.role().name(), actor, email);
        return new InviteMemberResponse(user.getId(), email, request.role().name(), temporary);
    }

    // ─── API keys ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> apiKeys(UUID organisationId) {
        requireAdmin(organisationId);
        return apiKeyRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .map(k -> mapKey(k, null)).toList();
    }

    public ApiKeyResponse createApiKey(UUID organisationId, CreateApiKeyRequest request) {
        UUID actor = requireAdmin(organisationId);
        requireOrg(organisationId);
        String prefix = randomToken(10);
        String secret = randomToken(32);
        ApiKey key = apiKeyRepository.save(ApiKey.builder()
                .organisationId(organisationId)
                .name(request.name().trim())
                .prefix(prefix)
                .secretHash(tokenHasher.hash(secret))
                .createdByUserId(actor)
                .build());
        auditLogService.record("ORGANISATION", organisationId, "API_KEY_CREATED", null, prefix, actor, key.getName());
        return mapKey(key, "cove_live_" + prefix + "_" + secret);
    }

    public void revokeApiKey(UUID organisationId, UUID keyId) {
        UUID actor = requireAdmin(organisationId);
        ApiKey key = apiKeyRepository.findById(keyId)
                .filter(k -> k.getOrganisationId().equals(organisationId))
                .orElseThrow(() -> ApiException.notFound("API key not found"));
        if (key.getRevokedAt() == null) {
            key.setRevokedAt(Instant.now());
            auditLogService.record("ORGANISATION", organisationId, "API_KEY_REVOKED", null, key.getPrefix(), actor, key.getName());
        }
    }

    private ApiKeyResponse mapKey(ApiKey k, String fullKey) {
        String createdBy = userRepository.findById(k.getCreatedByUserId()).map(User::getEmail).orElse(null);
        return new ApiKeyResponse(k.getId(), k.getName(), k.getPrefix(), createdBy, k.getCreatedAt(),
                k.getLastUsedAt(), k.getRevokedAt() != null, fullKey);
    }

    // ─── Webhooks ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WebhookResponse> webhooks(UUID organisationId) {
        requireAdmin(organisationId);
        return webhookRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).stream()
                .map(w -> mapWebhook(w, null)).toList();
    }

    public WebhookResponse createWebhook(UUID organisationId, WebhookRequest request) {
        UUID actor = requireAdmin(organisationId);
        urlPolicy.require(request.url(), true);
        String secret = "whsec_" + randomToken(32);
        WebhookEndpoint hook = webhookRepository.save(WebhookEndpoint.builder()
                .organisationId(organisationId)
                .url(request.url().trim())
                .secret(secret)
                .events(eventsString(request.events()))
                .active(request.active() == null || request.active())
                .build());
        auditLogService.record("ORGANISATION", organisationId, "WEBHOOK_CREATED", null, hook.getUrl(), actor, null);
        return mapWebhook(hook, secret);
    }

    public WebhookResponse updateWebhook(UUID organisationId, UUID webhookId, WebhookRequest request) {
        requireAdmin(organisationId);
        WebhookEndpoint hook = requireWebhook(organisationId, webhookId);
        urlPolicy.require(request.url(), true);
        hook.setUrl(request.url().trim());
        hook.setEvents(eventsString(request.events()));
        if (request.active() != null) {
            hook.setActive(request.active());
            if (request.active()) hook.setConsecutiveFailures(0);
        }
        return mapWebhook(hook, null);
    }

    public void deleteWebhook(UUID organisationId, UUID webhookId) {
        UUID actor = requireAdmin(organisationId);
        WebhookEndpoint hook = requireWebhook(organisationId, webhookId);
        webhookRepository.delete(hook);
        auditLogService.record("ORGANISATION", organisationId, "WEBHOOK_DELETED", hook.getUrl(), null, actor, null);
    }

    public WebhookEndpoint requireWebhook(UUID organisationId, UUID webhookId) {
        return webhookRepository.findById(webhookId)
                .filter(w -> w.getOrganisationId().equals(organisationId))
                .orElseThrow(() -> ApiException.notFound("Webhook not found"));
    }

    private String eventsString(Set<String> events) {
        if (events == null || events.isEmpty() || events.contains("*")) return "*";
        for (String e : events) {
            if (!CompanyDtos.WEBHOOK_EVENTS.contains(e)) {
                throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                        "Unknown event '" + e + "'. Use any of " + CompanyDtos.WEBHOOK_EVENTS);
            }
        }
        return String.join(",", new TreeSet<>(events));
    }

    private WebhookResponse mapWebhook(WebhookEndpoint w, String secret) {
        List<String> events = "*".equals(w.getEvents()) ? List.of("*") : Arrays.asList(w.getEvents().split(","));
        return new WebhookResponse(w.getId(), w.getUrl(), events, w.isActive(), w.getLastDeliveryAt(),
                w.getLastStatus(), w.getConsecutiveFailures(), w.getCreatedAt(), secret);
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static String randomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        return sb.toString();
    }
}
