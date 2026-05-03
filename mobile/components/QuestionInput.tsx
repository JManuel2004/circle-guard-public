import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput } from 'react-native';
import { Check, Circle } from 'lucide-react-native';

export type QuestionType = 'YES_NO' | 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'TEXT';

interface QuestionInputProps {
  question: {
    id: string;
    text: string;
    type: QuestionType;
    options?: string; // JSON string
  };
  value: any;
  onChange: (value: any) => void;
}

export const QuestionInput: React.FC<QuestionInputProps> = ({ question, value, onChange }) => {
  const options = question.options ? JSON.parse(question.options) : [];

  const renderYesNo = () => (
    <View style={styles.optionRow}>
      {['YES', 'NO'].map((opt) => (
        <TouchableOpacity
          key={opt}
          style={[styles.choiceButton, value === opt && styles.choiceButtonActive]}
          onPress={() => onChange(opt)}
        >
          <Text style={[styles.choiceText, value === opt && styles.choiceTextActive]}>{opt}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );

  const renderSingleChoice = () => (
    <View style={styles.choiceList}>
      {options.map((opt: string) => (
        <TouchableOpacity
          key={opt}
          style={[styles.listButton, value === opt && styles.listButtonActive]}
          onPress={() => onChange(opt)}
        >
          <View style={[styles.radio, value === opt && styles.radioActive]}>
            {value === opt && <View style={styles.radioInner} />}
          </View>
          <Text style={styles.choiceText}>{opt}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );

  const renderMultiChoice = () => {
    const selected = (value as string[]) || [];
    const toggle = (opt: string) => {
      if (selected.includes(opt)) {
        onChange(selected.filter(i => i !== opt));
      } else {
        onChange([...selected, opt]);
      }
    };

    return (
      <View style={styles.choiceList}>
        {options.map((opt: string) => (
          <TouchableOpacity
            key={opt}
            style={[styles.listButton, selected.includes(opt) && styles.listButtonActive]}
            onPress={() => toggle(opt)}
          >
            <View style={[styles.checkbox, selected.includes(opt) && styles.checkboxActive]}>
              {selected.includes(opt) && <Check size={12} color="#fff" />}
            </View>
            <Text style={styles.choiceText}>{opt}</Text>
          </TouchableOpacity>
        ))}
      </View>
    );
  };

  const renderText = () => (
    <TextInput
      style={styles.textInput}
      value={value}
      onChangeText={onChange}
      placeholder="Type your response..."
      placeholderTextColor="#52525b"
      multiline
    />
  );

  return (
    <View style={styles.container}>
      <Text style={styles.label}>{question.text}</Text>
      {question.type === 'YES_NO' && renderYesNo()}
      {question.type === 'SINGLE_CHOICE' && renderSingleChoice()}
      {question.type === 'MULTI_CHOICE' && renderMultiChoice()}
      {question.type === 'TEXT' && renderText()}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginBottom: 32,
  },
  label: {
    color: '#f4f4f5',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 16,
    lineHeight: 22,
  },
  optionRow: {
    flexDirection: 'row',
    gap: 12,
  },
  choiceButton: {
    flex: 1,
    paddingVertical: 14,
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    alignItems: 'center',
  },
  choiceButtonActive: {
    backgroundColor: '#0891B2',
    borderColor: '#0891B2',
  },
  choiceText: {
    color: '#a1a1aa',
    fontWeight: '700',
    fontSize: 14,
  },
  choiceTextActive: {
    color: '#fff',
  },
  choiceList: {
    gap: 12,
  },
  listButton: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    gap: 12,
  },
  listButtonActive: {
    borderColor: '#0891B2',
    backgroundColor: 'rgba(8, 145, 178, 0.05)',
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: '#3f3f46',
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioActive: {
    borderColor: '#0891B2',
  },
  radioInner: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#0891B2',
  },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: '#3f3f46',
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxActive: {
    backgroundColor: '#0891B2',
    borderColor: '#0891B2',
  },
  textInput: {
    backgroundColor: 'rgba(24, 24, 27, 0.8)',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    padding: 16,
    color: '#f4f4f5',
    fontSize: 14,
    minHeight: 100,
    textAlignVertical: 'top',
  },
});
