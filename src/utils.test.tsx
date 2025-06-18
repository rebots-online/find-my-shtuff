import { capitalize } from './utils';

describe('capitalize function', () => {
  it('should capitalize the first letter of a string', () => {
    expect(capitalize('hello')).toBe('Hello');
  });

  it('should return an empty string if the input is empty', () => {
    expect(capitalize('')).toBe('');
  });

  it('should handle already capitalized strings', () => {
    expect(capitalize('World')).toBe('World');
  });
});
