import { render, screen } from '@testing-library/react';
import TopBar from './TopBar';

describe('TopBar component', () => {
  it('renders and displays the title', () => {
    render(<TopBar />);
    const titleElement = screen.getByText(/Object Locator/i);
    expect(titleElement).toBeInTheDocument();
  });
});
