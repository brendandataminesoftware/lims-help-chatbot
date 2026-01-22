import { useState, useCallback, useEffect, useRef } from 'react';
import { sendChatMessage } from '../api/chat';

export function useChat(resolvedCollection, systemPrompt, initialMessages = [], onMessagesChange) {
    const [messages, setMessages] = useState(initialMessages);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const isInitialMount = useRef(true);

    // Sync messages from conversation when it changes
    useEffect(() => {
        setMessages(initialMessages);
    }, [initialMessages]);

    // Notify parent when messages change (but not on initial mount)
    useEffect(() => {
        if (isInitialMount.current) {
            isInitialMount.current = false;
            return;
        }
        if (onMessagesChange) {
            onMessagesChange(messages);
        }
    }, [messages, onMessagesChange]);

    const sendMessage = useCallback(async (content) => {
        if (!content.trim() || isLoading) return;

        const userMessage = { role: 'user', content };
        setMessages(prev => [...prev, userMessage]);
        setIsLoading(true);
        setError(null);

        try {
            const history = [...messages, userMessage].map(m => ({
                role: m.role,
                content: m.content
            }));

            console.log('Sending chat message with collection:', resolvedCollection);
            const data = await sendChatMessage(
                content,
                history,
                systemPrompt,
                resolvedCollection
            );

            const assistantMessage = {
                role: 'assistant',
                content: data.message,
                sources: data.sources || []
            };

            setMessages(prev => [...prev, assistantMessage]);
        } catch (err) {
            console.error('Error:', err);
            setError('Failed to get response. Please check that the server is running and try again.');
        } finally {
            setIsLoading(false);
        }
    }, [messages, isLoading, resolvedCollection, systemPrompt]);

    const clearChat = useCallback(() => {
        setMessages([]);
        setError(null);
    }, []);

    const clearError = useCallback(() => {
        setError(null);
    }, []);

    return {
        messages,
        isLoading,
        error,
        sendMessage,
        clearChat,
        clearError
    };
}
