import React from 'react';

export function SystemPromptDialog({
    isOpen,
    editedPrompt,
    onEditedPromptChange,
    onClose,
    onSave,
    onReset
}) {
    const handleOverlayClick = (e) => {
        if (e.target.classList.contains('dialog-overlay')) {
            onClose();
        }
    };

    return (
        <div
            className={`dialog-overlay ${isOpen ? 'open' : ''}`}
            onClick={handleOverlayClick}
        >
            <div className="dialog">
                <div className="dialog-header">
                    <h2>System Prompt</h2>
                    <button className="dialog-close" onClick={onClose}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>
                <div className="dialog-body">
                    <p className="dialog-description">
                        Customize the system prompt to change how the AI responds.
                        This will be saved and persisted across sessions.
                    </p>
                    <textarea
                        className="system-prompt-textarea"
                        placeholder="Enter custom system prompt..."
                        rows="12"
                        value={editedPrompt}
                        onChange={(e) => onEditedPromptChange(e.target.value)}
                    />
                </div>
                <div className="dialog-footer">
                    <button className="btn-secondary" onClick={onReset}>
                        Reset to Default
                    </button>
                    <button className="btn-primary" onClick={onSave}>
                        Save
                    </button>
                </div>
            </div>
        </div>
    );
}
