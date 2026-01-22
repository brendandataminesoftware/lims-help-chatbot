import React from 'react';

export function Sidebar({
    onNewChat,
    onOpenSettings,
    isOpen,
    onClose,
    isCollapsed,
    onToggleCollapse,
    conversations,
    activeConversationId,
    onSelectConversation,
    onDeleteConversation
}) {
    const handleNewChat = () => {
        onNewChat();
        onClose();
    };

    const handleOpenSettings = () => {
        onOpenSettings();
        onClose();
    };

    const handleSelectConversation = (id) => {
        onSelectConversation(id);
        onClose();
    };

    const handleDeleteConversation = (e, id) => {
        e.stopPropagation();
        onDeleteConversation(id);
    };

    return (
        <>
            <div
                className={`sidebar-overlay ${isOpen ? 'open' : ''}`}
                onClick={onClose}
                aria-hidden="true"
            />
            <aside className={`sidebar ${isOpen ? 'open' : ''} ${isCollapsed ? 'collapsed' : ''}`}>
                <div className="sidebar-header">
                    <button
                        className="new-chat-btn"
                        onClick={handleNewChat}
                        title={isCollapsed ? 'New Chat' : undefined}
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                        <span className="btn-text">New Chat</span>
                    </button>
                </div>
                <div className="sidebar-content">
                    {!isCollapsed && conversations && conversations.length > 0 && (
                        <div className="conversation-list">
                            {conversations.map(conv => (
                                <div
                                    key={conv.id}
                                    className={`conversation-item ${conv.id === activeConversationId ? 'active' : ''}`}
                                    onClick={() => handleSelectConversation(conv.id)}
                                    title={conv.title}
                                >
                                    <svg className="conversation-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                                    </svg>
                                    <span className="conversation-title">{conv.title}</span>
                                    <button
                                        className="conversation-delete"
                                        onClick={(e) => handleDeleteConversation(e, conv.id)}
                                        title="Delete conversation"
                                        aria-label="Delete conversation"
                                    >
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <polyline points="3 6 5 6 21 6"></polyline>
                                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                        </svg>
                                    </button>
                                </div>
                            ))}
                        </div>
                    )}
                    {isCollapsed && (
                        <div className="collapsed-conversations">
                            <button
                                className="collapsed-conv-btn"
                                title="Conversations"
                            >
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                                </svg>
                            </button>
                        </div>
                    )}
                    <div className="prompt-section">
                        <button
                            className="settings-btn"
                            onClick={handleOpenSettings}
                            title={isCollapsed ? 'System Prompt' : undefined}
                        >
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="3"></circle>
                                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                            </svg>
                            <span className="btn-text">System Prompt</span>
                        </button>
                    </div>
                </div>
                <div className="sidebar-footer">
                    <button
                        className="collapse-btn"
                        onClick={onToggleCollapse}
                        title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
                        aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
                    >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polyline points={isCollapsed ? "9 18 15 12 9 6" : "15 18 9 12 15 6"}></polyline>
                        </svg>
                    </button>
                </div>
            </aside>
        </>
    );
}
