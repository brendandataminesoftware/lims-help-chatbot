import { useState, useCallback, useEffect, useRef } from 'react';

const API_BASE = '/api/conversations';
const SAVE_DEBOUNCE_MS = 1000;

export function useConversations() {
    const [conversations, setConversations] = useState([]);
    const [activeId, setActiveId] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const saveTimeoutRef = useRef(null);
    const pendingSaveRef = useRef(null);

    // Load conversations from API on mount
    useEffect(() => {
        loadConversations();

        // Cleanup: flush pending save on unmount
        return () => {
            if (saveTimeoutRef.current) {
                clearTimeout(saveTimeoutRef.current);
            }
            // Flush any pending save synchronously
            const save = pendingSaveRef.current;
            if (save) {
                navigator.sendBeacon(
                    `${API_BASE}/${save.id}`,
                    new Blob([JSON.stringify({ messagesJson: JSON.stringify(save.messages) })], { type: 'application/json' })
                );
            }
        };
    }, []);

    const loadConversations = async () => {
        try {
            setIsLoading(true);
            const response = await fetch(API_BASE);
            if (response.ok) {
                const data = await response.json();
                const mapped = data.map(conv => ({
                    id: conv.id,
                    title: conv.title,
                    messages: JSON.parse(conv.messagesJson || '[]'),
                    createdAt: conv.createdAt
                }));
                setConversations(mapped);

                // Set active to first conversation, or create one if empty
                if (mapped.length > 0) {
                    if (!activeId || !mapped.find(c => c.id === activeId)) {
                        setActiveId(mapped[0].id);
                    }
                } else {
                    // Create initial conversation
                    await createConversationInternal();
                }
            }
        } catch (error) {
            console.error('Failed to load conversations:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const createConversationInternal = async () => {
        try {
            const response = await fetch(API_BASE, { method: 'POST' });
            if (response.ok) {
                const conv = await response.json();
                const mapped = {
                    id: conv.id,
                    title: conv.title,
                    messages: JSON.parse(conv.messagesJson || '[]'),
                    createdAt: conv.createdAt
                };
                setConversations(prev => [mapped, ...prev]);
                setActiveId(conv.id);
                return conv.id;
            }
        } catch (error) {
            console.error('Failed to create conversation:', error);
        }
        return null;
    };

    const activeConversation = conversations.find(c => c.id === activeId) || conversations[0];

    const createConversation = useCallback(async () => {
        return await createConversationInternal();
    }, []);

    const selectConversation = useCallback((id) => {
        setActiveId(id);
    }, []);

    const deleteConversation = useCallback(async (id) => {
        try {
            const response = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
            if (response.ok) {
                setConversations(prev => {
                    const filtered = prev.filter(c => c.id !== id);

                    // If deleting active conversation, switch to another or create new
                    if (activeId === id) {
                        if (filtered.length > 0) {
                            setActiveId(filtered[0].id);
                        } else {
                            // Create a new conversation
                            createConversationInternal();
                        }
                    }

                    return filtered;
                });
            }
        } catch (error) {
            console.error('Failed to delete conversation:', error);
        }
    }, [activeId]);

    const updateMessages = useCallback((messages) => {
        if (!activeId) return;

        // Update local state immediately for responsiveness
        setConversations(prev => prev.map(conv => {
            if (conv.id === activeId) {
                const title = getConversationTitle(messages);
                return { ...conv, messages, title };
            }
            return conv;
        }));

        // Debounce the API call to prevent flooding
        pendingSaveRef.current = { id: activeId, messages };

        if (saveTimeoutRef.current) {
            clearTimeout(saveTimeoutRef.current);
        }

        saveTimeoutRef.current = setTimeout(async () => {
            const save = pendingSaveRef.current;
            if (!save) return;

            try {
                await fetch(`${API_BASE}/${save.id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ messagesJson: JSON.stringify(save.messages) })
                });
            } catch (error) {
                console.error('Failed to update conversation:', error);
            }
            pendingSaveRef.current = null;
        }, SAVE_DEBOUNCE_MS);
    }, [activeId]);

    const clearCurrentChat = useCallback(() => {
        updateMessages([]);
    }, [updateMessages]);

    return {
        conversations,
        activeConversation,
        activeId,
        isLoading,
        createConversation,
        selectConversation,
        deleteConversation,
        updateMessages,
        clearCurrentChat
    };
}

function getConversationTitle(messages) {
    if (messages.length === 0) return 'New Chat';
    const firstUserMessage = messages.find(m => m.role === 'user');
    if (!firstUserMessage) return 'New Chat';
    const title = firstUserMessage.content.slice(0, 40);
    return title.length < firstUserMessage.content.length ? title + '...' : title;
}
