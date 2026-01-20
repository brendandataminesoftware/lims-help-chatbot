import React, { useState, useRef, useCallback } from 'react';

export function InputArea({ onSendMessage, isLoading }) {
    const [message, setMessage] = useState('');
    const textareaRef = useRef(null);

    const autoResize = useCallback(() => {
        const textarea = textareaRef.current;
        if (textarea) {
            textarea.style.height = 'auto';
            textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px';
        }
    }, []);

    const handleSubmit = () => {
        if (message.trim() && !isLoading) {
            onSendMessage(message.trim());
            setMessage('');
            if (textareaRef.current) {
                textareaRef.current.style.height = 'auto';
            }
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    return (
        <div className="input-area">
            <div className="input-container">
                <textarea
                    ref={textareaRef}
                    className="message-input"
                    placeholder="Ask a question about Datamine..."
                    rows="1"
                    value={message}
                    onChange={(e) => {
                        setMessage(e.target.value);
                        autoResize();
                    }}
                    onKeyDown={handleKeyDown}
                />
                <button
                    className="send-btn"
                    onClick={handleSubmit}
                    disabled={isLoading}
                >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="22" y1="2" x2="11" y2="13"></line>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                </button>
            </div>
            <p className="disclaimer">Responses are generated from Datamine documentation. Please verify critical information.</p>
        </div>
    );
}
