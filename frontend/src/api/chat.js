const DEFAULT_LOGO = 'https://docs.dataminesoftware.com/Assets/Images/Datamine-Logo.png';

export async function sendChatMessage(message, history, systemPrompt, collectionName) {
    const response = await fetch('/api/chat', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message,
            history: history.slice(-10),
            systemPrompt: systemPrompt || null,
            collectionName: collectionName || null
        })
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
}

export async function fetchCollectionMetadata(collectionName) {
    if (!collectionName) {
        return {
            title: 'Product Documentation',
            logo: DEFAULT_LOGO,
            resolvedCollection: null
        };
    }

    try {
        const response = await fetch(`/api/collections/${encodeURIComponent(collectionName)}/metadata`);
        if (response.ok) {
            const data = await response.json();
            return {
                title: data.title || 'Product Documentation',
                logo: data.logo || DEFAULT_LOGO,
                resolvedCollection: data.resolvedCollection || collectionName
            };
        }
    } catch (error) {
        console.error('Error loading collection metadata:', error);
    }

    return {
        title: 'Product Documentation',
        logo: DEFAULT_LOGO,
        resolvedCollection: collectionName
    };
}

export async function fetchSystemPrompt() {
    const response = await fetch('/api/chat/system-prompt');
    const data = await response.json();
    return data.prompt;
}

export async function saveSystemPrompt(prompt) {
    const response = await fetch('/api/chat/system-prompt', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ prompt })
    });

    if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Unknown error');
    }

    return true;
}

export async function resetSystemPrompt() {
    const response = await fetch('/api/chat/system-prompt', {
        method: 'DELETE'
    });

    if (!response.ok) {
        throw new Error('Failed to reset system prompt');
    }

    const data = await response.json();
    return data.prompt;
}
