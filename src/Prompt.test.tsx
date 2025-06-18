import { render, screen } from '@testing-library/react';
import Prompt from './Prompt';

describe('Prompt component', () => {
  it('renders without crashing and displays an input field', () => {
    render(<Prompt />);
    const inputElement = screen.getByRole('textbox');
    expect(inputElement).toBeInTheDocument();
  });
});
