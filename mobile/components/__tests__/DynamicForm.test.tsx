import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { DynamicForm } from '../DynamicForm';

const mockQuestions: any[] = [
  { id: '1', text: 'Question 1', type: 'YES_NO' },
  { id: '2', text: 'Question 2', type: 'TEXT' }
];

describe('DynamicForm', () => {
  it('renders all questions', () => {
    const { getByText } = render(
      <DynamicForm questions={mockQuestions} onSubmit={() => {}} />
    );

    expect(getByText('Question 1')).toBeTruthy();
    expect(getByText('Question 2')).toBeTruthy();
  });

  it('updates text responses', () => {
    const { getByPlaceholderText } = render(
      <DynamicForm questions={mockQuestions} onSubmit={() => {}} />
    );

    const input = getByPlaceholderText('Type your response...');
    fireEvent.changeText(input, 'Testing');
    
    // Check if value is updated (internal state is harder to test directly without props)
    expect(input.props.value).toBe('Testing');
  });
});
