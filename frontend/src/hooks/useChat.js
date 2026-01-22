import { useState, useCallback, useEffect, useRef } from 'react';
import { sendChatMessage } from '../api/chat';

export function useChat(resolvedCollection, systemPrompt, initialMessages = [], onMessagesChange) {
    const [messages, setMessages] = useState(initialMessages);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const onMessagesChangeRef = useRef(onMessagesChange);

    // Keep ref updated
    useEffect(() => {
        onMessagesChangeRef.current = onMessagesChange;
    }, [onMessagesChange]);

    // Sync messages from conversation when it changes (compare by reference)
    const prevInitialRef = useRef(initialMessages);
    useEffect(() => {
        if (prevInitialRef.current !== initialMessages) {
            setMessages(initialMessages);
            prevInitialRef.current = initialMessages;
        }
    }, [initialMessages]);

    const sendMessage = useCallback(async (content) => {
        if (!content.trim() || isLoading) return;

        const userMessage = { role: 'user', content };
        const updatedMessages = [...messages, userMessage];
        setMessages(updatedMessages);
        setIsLoading(true);
        setError(null);

        try {
            const history = updatedMessages.map(m => ({
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

            const finalMessages = [...updatedMessages, assistantMessage];
            setMessages(finalMessages);

            // Save only after complete exchange
            if (onMessagesChangeRef.current) {
                onMessagesChangeRef.current(finalMessages);
            }
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
        if (onMessagesChangeRef.current) {
            onMessagesChangeRef.current([]);
        }
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
