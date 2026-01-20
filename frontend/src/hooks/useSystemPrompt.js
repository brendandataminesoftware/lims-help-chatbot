import { useState, useEffect, useCallback } from 'react';
import { fetchSystemPrompt, saveSystemPrompt as apiSavePrompt, resetSystemPrompt as apiResetPrompt } from '../api/chat';

export function useSystemPrompt() {
    const [systemPrompt, setSystemPrompt] = useState('');
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editedPrompt, setEditedPrompt] = useState('');

    useEffect(() => {
        async function loadPrompt() {
            try {
                const prompt = await fetchSystemPrompt();
                setSystemPrompt(prompt);
                setEditedPrompt(prompt);
            } catch (error) {
                console.error('Error loading system prompt:', error);
            }
        }
        loadPrompt();
    }, []);

    const openDialog = useCallback(() => {
        setEditedPrompt(systemPrompt);
        setIsDialogOpen(true);
    }, [systemPrompt]);

    const closeDialog = useCallback(() => {
        setIsDialogOpen(false);
    }, []);

    const savePrompt = useCallback(async () => {
        try {
            await apiSavePrompt(editedPrompt.trim());
            setSystemPrompt(editedPrompt.trim());
            setIsDialogOpen(false);
        } catch (error) {
            console.error('Error saving system prompt:', error);
            alert('Failed to save: ' + error.message);
        }
    }, [editedPrompt]);

    const resetPrompt = useCallback(async () => {
        try {
            const defaultPrompt = await apiResetPrompt();
            setSystemPrompt(defaultPrompt);
            setEditedPrompt(defaultPrompt);
        } catch (error) {
            console.error('Error resetting system prompt:', error);
            alert('Failed to reset system prompt. Please try again.');
        }
    }, []);

    return {
        systemPrompt,
        isDialogOpen,
        editedPrompt,
        setEditedPrompt,
        openDialog,
        closeDialog,
        savePrompt,
        resetPrompt
    };
}
