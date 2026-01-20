import React from 'react';

export function Header({ logo, title }) {
    return (
        <header className="site-header">
            <a href="https://www.dataminesoftware.com" target="_blank" rel="noopener noreferrer" className="header-logo-link">
                <img src={logo} alt="Logo" className="header-logo" />
            </a>
            <span className="header-title">{title}</span>
        </header>
    );
}
