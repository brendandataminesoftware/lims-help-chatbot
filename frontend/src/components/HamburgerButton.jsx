import React from 'react';

export function HamburgerButton({ isOpen, onClick }) {
    return (
        <button
            className={`hamburger-btn ${isOpen ? 'open' : ''}`}
            onClick={onClick}
            aria-label={isOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={isOpen}
        >
            <span className="hamburger-line"></span>
            <span className="hamburger-line"></span>
            <span className="hamburger-line"></span>
        </button>
    );
}
