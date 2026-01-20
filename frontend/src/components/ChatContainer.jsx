import React, { useEffect, useRef } from 'react';
import { Message } from './Message';
import { TypingIndicator } from './TypingIndicator';
import { WelcomeMessage } from './WelcomeMessage';

export function ChatContainer({ messages, isLoading, error, onClearError }) {
    const containerRef = useRef(null);

    useEffect(() => {
        if (containerRef.current) {
            containerRef.current.scrollTop = containerRef.current.scrollHeight;
        }
    }, [messages, isLoading]);

    useEffect(() => {
        if (error) {
            const timer = setTimeout(onClearError, 5000);
            return () => clearTimeout(timer);
        }
    }, [error, onClearError]);

    return (
        <div className="chat-container" ref={containerRef}>
            {messages.length === 0 ? (
                <WelcomeMessage />
            ) : (
                messages.map((message, index) => (
                    <Message
                        key={index}
                        role={message.role}
                        content={message.content}
                        sources={message.sources}
                    />
                ))
            )}
            {isLoading && <TypingIndicator />}
            {error && (
                <div className="error-message">{error}</div>
            )}
        </div>
    );
}
