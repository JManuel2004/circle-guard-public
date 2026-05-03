package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Question;
import com.circleguard.form.model.QuestionType;
import com.circleguard.form.model.Questionnaire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SymptomMapperTest {

    private final SymptomMapper mapper = new SymptomMapper();

    @Test
    void shouldDetectSymptomsFromFever() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Do you have a fever?")
                .type(QuestionType.YES_NO)
                .build();
        
        Questionnaire questionnaire = Questionnaire.builder()
                .questions(List.of(q))
                .build();
        
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "YES"))
                .build();
        
        assertTrue(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldNotDetectSymptomsWhenNo() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Do you have a fever?")
                .type(QuestionType.YES_NO)
                .build();
        
        Questionnaire questionnaire = Questionnaire.builder()
                .questions(List.of(q))
                .build();
        
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "NO"))
                .build();
        
        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }
}
