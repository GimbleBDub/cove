package bw.co.gimble.cove.organisations;

import bw.co.gimble.cove.common.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "organisations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organisation extends Auditable {

    @Column(nullable = false)
    private String name;

    /** Short, unique, URL-friendly handle used for search/deep-links, e.g. "acme-realty". */
    @Column(nullable = false, unique = true)
    private String slug;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organisation_types", joinColumns = @JoinColumn(name = "organisation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false)
    @Builder.Default
    private List<OrgType> types = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrgVerificationStatus verificationStatus = OrgVerificationStatus.UNDER_REVIEW;

    private String description;

    private String email;

    private String phone;

    private String website;

    /** Public bucket URL - set via POST /api/organisations/{id}/logo, not on create/update. */
    private String logoUrl;

    /** Anyone can find and request to join by default; an org can hide itself from search. */
    @Column(nullable = false)
    @Builder.Default
    private boolean discoverable = true;
}