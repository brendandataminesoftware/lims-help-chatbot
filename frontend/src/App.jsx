import React, { useEffect, useRef } from 'react';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { ChatContainer } from './components/ChatContainer';
import { InputArea } from './components/InputArea';
import { SystemPromptDialog } from './components/SystemPromptDialog';
import { useCollection } from './hooks/useCollection';
import { useChat } from './hooks/useChat';
import { useSystemPrompt } from './hooks/useSystemPrompt';

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
    const { messages, isLoading, error, sendMessage, clearChat, clearError } = useChat(
        resolvedCollection,
        systemPrompt
    );

    const inputRef = useRef(null);

    // Clear chat when collection changes
    useEffect(() => {
        clearChat();
    }, [urlCollection, clearChat]);

    // Focus input on mount
    useEffect(() => {
        if (inputRef.current) {
            inputRef.current.focus();
        }
    }, []);

    const handleNewChat = () => {
        clearChat();
    };

    return (
        <>
            <Header logo={logo} title={title} />
            <div className="app-container">
                <Sidebar
                    collection={urlCollection}
                    onNewChat={handleNewChat}
                    onOpenSettings={openDialog}
                />
                <main className="main-content">
                    <ChatContainer
                        messages={messages}
                        isLoading={isLoading}
                        error={error}
                        collection={urlCollection}
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
