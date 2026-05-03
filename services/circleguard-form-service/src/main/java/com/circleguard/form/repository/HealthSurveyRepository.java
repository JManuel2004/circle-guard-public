package com.circleguard.form.repository;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface HealthSurveyRepository extends JpaRepository<HealthSurvey, UUID> {
    List<HealthSurvey> findByAttachmentPathIsNotNullAndValidationStatus(ValidationStatus status);
}
