package com.circleguard.form.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "health_surveys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthSurvey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anonymous_id", nullable = false)
    private UUID anonymousId;

    @Column(name = "has_fever")
    private Boolean hasFever;

    @Column(name = "has_cough")
    private Boolean hasCough;

    @Column(name = "other_symptoms")
    private String otherSymptoms;

    @Column(name = "exposure_date")
    private LocalDate exposureDate;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> responses;

    @Column(name = "attachment_path")
    private String attachmentPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status")
    private ValidationStatus validationStatus;

    @Column(name = "validated_by")
    private UUID validatedBy;
}
