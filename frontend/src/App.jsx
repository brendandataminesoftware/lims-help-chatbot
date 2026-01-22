import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { ChatContainer } from './components/ChatContainer';
import { InputArea } from './components/InputArea';
import { SystemPromptDialog } from './components/SystemPromptDialog';
import { useCollection } from './hooks/useCollection';
import { useChat } from './hooks/useChat';
import { useSystemPrompt } from './hooks/useSystemPrompt';
import { useConversations } from './hooks/useConversations';

function App() {
    const { urlCollection, resolvedCollection, title, logo } = useCollection();
    const {
        systemPrompt,
        isDialogOpen,
        editedPrompt,
        setEditedPrompt,
        openDialog,
        closeDialog,
        savePrompt,
        resetPrompt
    } = useSystemPrompt();

    const {
        conversations,
        activeConversation,
        activeId,
        createConversation,
        selectConversation,
        deleteConversation,
        updateMessages
    } = useConversations();

    const handleMessagesChange = useCallback((messages) => {
        updateMessages(messages);
    }, [updateMessages]);

    const { messages, isLoading, error, sendMessage, clearChat, clearError } = useChat(
        resolvedCollection,
        systemPrompt,
        activeConversation?.messages || [],
        activeId,
        handleMessagesChange
    );

    const inputRef = useRef(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

    const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);
    const closeSidebar = () => setIsSidebarOpen(false);
    const toggleSidebarCollapse = () => setIsSidebarCollapsed(!isSidebarCollapsed);

    // Focus input on mount
    useEffect(() => {
        if (inputRef.current) {
            inputRef.current.focus();
        }
    }, []);

    const handleNewChat = () => {
        createConversation();
    };

    return (
        <>
            <Header logo={logo} title={title} isSidebarOpen={isSidebarOpen} onToggleSidebar={toggleSidebar} />
            <div className={`app-container ${isSidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
                <Sidebar
                    onNewChat={handleNewChat}
                    onOpenSettings={openDialog}
                    isOpen={isSidebarOpen}
                    onClose={closeSidebar}
                    isCollapsed={isSidebarCollapsed}
                    onToggleCollapse={toggleSidebarCollapse}
                    conversations={conversations}
                    activeConversationId={activeId}
                    onSelectConversation={selectConversation}
                    onDeleteConversation={deleteConversation}
                />
                <main className="main-content">
                    <ChatContainer
                        messages={messages}
                        isLoading={isLoading}
                        error={error}
                        onClearError={clearError}
                    />
                    <InputArea
                        onSendMessage={sendMessage}
                        isLoading={isLoading}
                    />
                </main>
            </div>
            <SystemPromptDialog
                isOpen={isDialogOpen}
                editedPrompt={editedPrompt}
                onEditedPromptChange={setEditedPrompt}
                onClose={closeDialog}
                onSave={savePrompt}
                onReset={resetPrompt}
            />
        </>
    );
}

export default App;
