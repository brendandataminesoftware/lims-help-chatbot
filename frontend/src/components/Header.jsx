import React from 'react';
import { HamburgerButton } from './HamburgerButton';

export function Header({ logo, title, isSidebarOpen, onToggleSidebar }) {
    return (
        <header className="site-header">
            <HamburgerButton isOpen={isSidebarOpen} onClick={onToggleSidebar} />
            <a href="https://www.dataminesoftware.com" target="_blank" rel="noopener noreferrer" className="header-logo-link">
                <img src={logo} alt="Logo" className="header-logo" />
            </a>
            <span className="header-title">{title}</span>
        </header>
    );
}
