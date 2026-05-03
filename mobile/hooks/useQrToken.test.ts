import { renderHook, act } from '@testing-library/react-native';
import { useQrToken } from './useQrToken';

describe('useQrToken', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test('should initialize with a token and 60s timer when anonymousId is present', () => {
    const { result } = renderHook(() => useQrToken('test-id'));
    
    expect(result.current.token).not.toBeNull();
    expect(result.current.timeLeft).toBe(60);
  });

  test('should not initialize if anonymousId is null', () => {
    const { result } = renderHook(() => useQrToken(null));
    
    expect(result.current.token).toBeNull();
  });

  test('should decrement timer every second', () => {
    const { result } = renderHook(() => useQrToken('test-id'));
    
    act(() => {
      jest.advanceTimersByTime(1000);
    });
    
    expect(result.current.timeLeft).toBe(59);
  });

  test('should rotate token and reset timer when it reaches 0', () => {
    const { result } = renderHook(() => useQrToken('test-id'));
    const initialToken = result.current.token;
    
    act(() => {
      jest.advanceTimersByTime(60000);
    });
    
    expect(result.current.token).not.toBe(initialToken);
    expect(result.current.timeLeft).toBe(60);
  });
});
