import { useState, useCallback, useEffect } from 'react';

const STORAGE_KEY = 'chat_conversations';

function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
}

function getConversationTitle(messages) {
    if (messages.length === 0) return 'New Chat';
    const firstUserMessage = messages.find(m => m.role === 'user');
    if (!firstUserMessage) return 'New Chat';
    const title = firstUserMessage.content.slice(0, 40);
    return title.length < firstUserMessage.content.length ? title + '...' : title;
}

function loadFromStorage() {
    try {
        const data = localStorage.getItem(STORAGE_KEY);
        if (data) {
            return JSON.parse(data);
        }
    } catch (e) {
        console.error('Failed to load conversations from storage:', e);
    }
    return null;
}

function saveToStorage(conversations, activeId) {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ conversations, activeId }));
    } catch (e) {
        console.error('Failed to save conversations to storage:', e);
    }
}

export function useConversations() {
    const [conversations, setConversations] = useState(() => {
        const stored = loadFromStorage();
        if (stored?.conversations?.length > 0) {
            return stored.conversations;
        }
        return [{ id: generateId(), title: 'New Chat', messages: [], createdAt: Date.now() }];
    });

    const [activeId, setActiveId] = useState(() => {
        const stored = loadFromStorage();
        if (stored?.activeId && stored.conversations?.find(c => c.id === stored.activeId)) {
            return stored.activeId;
        }
        return conversations[0]?.id;
    });

    // Save to localStorage whenever conversations or activeId changes
    useEffect(() => {
        saveToStorage(conversations, activeId);
    }, [conversations, activeId]);

    const activeConversation = conversations.find(c => c.id === activeId) || conversations[0];

    const createConversation = useCallback(() => {
        const newConversation = {
            id: generateId(),
            title: 'New Chat',
            messages: [],
            createdAt: Date.now()
        };
        setConversations(prev => [newConversation, ...prev]);
        setActiveId(newConversation.id);
        return newConversation.id;
    }, []);

    const selectConversation = useCallback((id) => {
        setActiveId(id);
    }, []);

    const deleteConversation = useCallback((id) => {
        setConversations(prev => {
            const filtered = prev.filter(c => c.id !== id);
            if (filtered.length === 0) {
                const newConversation = {
                    id: generateId(),
                    title: 'New Chat',
                    messages: [],
                    createdAt: Date.now()
                };
                return [newConversation];
            }
            return filtered;
        });

        // If deleting active conversation, switch to another
        if (activeId === id) {
            setConversations(prev => {
                const remaining = prev.filter(c => c.id !== id);
                if (remaining.length > 0) {
                    setActiveId(remaining[0].id);
                }
                return prev;
            });
        }
    }, [activeId]);

    const updateMessages = useCallback((messages) => {
        setConversations(prev => prev.map(conv => {
            if (conv.id === activeId) {
                return {
                    ...conv,
                    messages,
                    title: getConversationTitle(messages)
                };
            }
            return conv;
        }));
    }, [activeId]);

    const clearCurrentChat = useCallback(() => {
        updateMessages([]);
    }, [updateMessages]);

    return {
        conversations,
        activeConversation,
        activeId,
        createConversation,
        selectConversation,
        deleteConversation,
        updateMessages,
        clearCurrentChat
    };
}
