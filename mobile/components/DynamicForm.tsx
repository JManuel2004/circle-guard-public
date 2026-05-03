import React, { useState } from 'react';
import { View, StyleSheet, ScrollView } from 'react-native';
import { QuestionInput, QuestionType } from './QuestionInput';

interface Question {
  id: string;
  text: string;
  type: QuestionType;
  options?: string;
}

interface DynamicFormProps {
  questions: Question[];
  onSubmit: (responses: Record<string, any>) => void;
  submitLabel?: string;
}

export const DynamicForm: React.FC<DynamicFormProps> = ({ questions, onSubmit }) => {
  const [responses, setResponses] = useState<Record<string, any>>({});

  const handleUpdate = (id: string, value: any) => {
    setResponses(prev => ({ ...prev, [id]: value }));
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      {questions.map((q) => (
        <QuestionInput
          key={q.id}
          question={q}
          value={responses[q.id]}
          onChange={(val) => handleUpdate(q.id, val)}
        />
      ))}
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
