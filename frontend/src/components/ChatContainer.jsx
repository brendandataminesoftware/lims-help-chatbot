import React, { useEffect, useRef } from 'react';
import { Message } from './Message';
import { TypingIndicator } from './TypingIndicator';
import { WelcomeMessage } from './WelcomeMessage';

export function ChatContainer({ messages, isLoading, error, onClearError, onFollowUpClick, logo }) {
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

    // Only show follow-ups on the last assistant message
    const lastAssistantIndex = messages.reduce((last, msg, idx) =>
        msg.role === 'assistant' ? idx : last, -1);

    return (
        <div className="chat-container" ref={containerRef}>
            {messages.length === 0 ? (
                <WelcomeMessage logo={logo} />
            ) : (
                messages.map((message, index) => (
                    <Message
                        key={index}
                        role={message.role}
                        content={message.content}
                        sources={message.sources}
                        followUps={index === lastAssistantIndex && !isLoading ? message.followUps : []}
                        onFollowUpClick={onFollowUpClick}
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
