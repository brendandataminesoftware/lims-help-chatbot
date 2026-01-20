import React from 'react';

export function TypingIndicator() {
    return (
        <div className="message assistant">
            <div className="message-content">
                <div className="avatar assistant">Alfred</div>
                <div className="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                </div>
            </div>
        </div>
    );
}
