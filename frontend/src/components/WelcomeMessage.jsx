import React from 'react';

export function WelcomeMessage({ logo }) {
    return (
        <div className="welcome-message">
            {logo && (
                <img src={logo} alt="Logo" className="welcome-logo" />
            )}
            <h1>Datamine Help</h1>
            <p>How can I help you today?</p>
        </div>
    );
}
