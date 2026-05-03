package com.circleguard.form.service;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionnaireService {
    private final QuestionnaireRepository repository;

    public List<Questionnaire> getAllQuestionnaires() {
        return repository.findAll();
    }

    public Optional<Questionnaire> getActiveQuestionnaire() {
        return repository.findFirstByIsActiveTrueOrderByVersionDesc();
    }

    @Transactional
    public Questionnaire saveQuestionnaire(Questionnaire questionnaire) {
        if (questionnaire.getQuestions() != null) {
            questionnaire.getQuestions().forEach(q -> q.setQuestionnaire(questionnaire));
        }
        return repository.save(questionnaire);
    }

    @Transactional
    public void activateQuestionnaire(UUID id) {
        // Deactivate all others
        repository.findAll().forEach(q -> {
            if (q.getIsActive()) {
                q.setIsActive(false);
                repository.save(q);
            }
        });
        
        // Activate target
        repository.findById(id).ifPresent(q -> {
            q.setIsActive(true);
            repository.save(q);
        });
    }
}
