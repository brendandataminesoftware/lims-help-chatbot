import React from 'react';

export function WelcomeMessage({ collection }) {
    return (
        <div className="welcome-message">
            <h1>Datamine Help</h1>
            <p>{collection ? `Collection: ${collection}` : 'How can I help you today?'}</p>
        </div>
    );
}
