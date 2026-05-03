package com.circleguard.form.repository;

import com.circleguard.form.model.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {
    Optional<Questionnaire> findFirstByIsActiveTrueOrderByVersionDesc();
}
